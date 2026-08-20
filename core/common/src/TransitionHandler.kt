package kotlinx.datetime

import kotlin.time.Instant

/**
 * A strategy for handling a [Transition][LocalDateTimeOffsetInfo.Transition].
 *
 * Please see [LocalDateTimeOffsetInfo] for information about what transitions are.
 *
 * ## Construction
 *
 * There are predefined transition handlers:
 * - [USE_OFFSET_BEFORE], for leniently adjusting the datetime to a reasonable [Instant].
 * - [FIND_EARLIEST_VALID_TIME], for determining the earliest [Instant] that could correspond to a datetime.
 * - [REQUIRE_RESOLVED], for ensuring the input is well-defined and corresponds to exactly one [Instant].
 * - [REJECT_TRANSITIONS], for ensuring that predefined data is not in a transition.
 *
 * If none of these fulfills your needs, a custom implementation can be defined:
 *
 * ```
 * /**
 *  * The `earlier` strategy from Temporal.JS.
 *  *
 *  * If there are two possible instants, choose the earlier one.
 *  * If there is a gap, go back by the gap duration.
 *  */
 * val RESOLVE_TO_EARLIER = object: TransitionHandler {
 *     override fun resolveDateTime(
 *         dateTime: LocalDateTime,
 *         transition: LocalDateTimeOffsetInfo.Transition,
 *         preferredOffset: UtcOffset?,
 *     ): Instant {
 *         val offset = when (transition) {
 *             is LocalDateTimeOffsetInfo.Overlap ->
 *                 if (preferredOffset == transition.offsetAfter)
 *                     transition.offsetAfter
 *                 else
 *                     transition.offsetBefore
 *             is LocalDateTimeOffsetInfo.Gap ->
 *                 transition.offsetAfter
 *         }
 *         return dateTime.toInstant(offset)
 *     }
 * }
 * ```
 *
 * ## Usage
 *
 * Implementations of this interface typically should not be used directly.
 * Instead, they can be passed as parameters to `kotlinx-datetime` functions such as
 * [LocalDateTime.toInstant] or calendar-based arithmetic operations on [Instant].
 */
public interface TransitionHandler {
    /**
     * Calculates the [Instant] corresponding to the given [dateTime].
     *
     * This function should only be implemented but typically not called from client code.
     *
     * Implementations may rely on the following guarantees:
     *
     * - [preferredOffset] is equal to `null`, `transition.offsetBefore`, or `transition.offsetAfter`.
     *   It represents the best estimation of the implied offset.
     * - [transition] is equal to the result of [TimeZone.offsetInfoFor] for [dateTime]
     *   in the implied time zone.
     */
    public fun resolveDateTime(
        dateTime: LocalDateTime,
        transition: LocalDateTimeOffsetInfo.Transition,
        preferredOffset: UtcOffset?,
    ): Instant

    public companion object {
        /**
         * Behave as if the transition has not happened yet for ambiguous or non-existent datetimes.
         *
         * This strategy is equivalent to the `java.time`'s default handling of transitions.
         * It's well-suited for leniently adjusting ambiguous or non-existent datetimes
         * to the moments in time they could likely refer to.
         * Therefore, this is the default transition handler for datetime arithmetic operations in
         * `kotlinx-datetime`.
         *
         * If the transition is an [overlap][LocalDateTimeOffsetInfo.Overlap] and a preferred UTC offset is provided,
         * it gets used.
         * In all other cases, the [UTC offset before the transition][LocalDateTimeOffsetInfo.Transition.offsetBefore]
         * is used, as if the clocks were not moved yet.
         *
         * For example, if the clocks were moved from `03:00` to `02:00` on a given day,
         * the ambiguous time-of-day `02:31` will be resolved to the first occurrence.
         * If the clocks were moved from `02:00` to `03:00`,
         * the non-existent time-of-day `02:31` will be resolved to the moment corresponding to `03:31`,
         * which would have been `02:31` if the clocks were not moved that day.
         */
        public val USE_OFFSET_BEFORE: TransitionHandler = object: TransitionHandler {
            override fun resolveDateTime(
                dateTime: LocalDateTime,
                transition: LocalDateTimeOffsetInfo.Transition,
                preferredOffset: UtcOffset?
            ): Instant = (if (preferredOffset == transition.offsetAfter && transition is LocalDateTimeOffsetInfo.Overlap)
                    transition.offsetAfter
                else
                    transition.offsetBefore
            ).let { dateTime.toInstant(it) }
        }

        /**
         * Find the earliest moment near to the given [LocalDateTime] and adjust to it if needed.
         *
         * On [overlaps][LocalDateTimeOffsetInfo.Overlap],
         * this strategy always chooses to use [offsetBefore][LocalDateTimeOffsetInfo.Transition.offsetBefore].
         * For example, if the clocks were moved from `03:00` to `02:00` on a given day,
         * the ambiguous time-of-day `02:31` will be resolved to the first occurrence.
         *
         * On [gaps][LocalDateTimeOffsetInfo.Gap],
         * it finds the first moment that does not fall into the gap.
         * For example, if the clocks were moved from `02:00` to `03:00` on a given day,
         * the non-existent time-of-day `02:31` will be resolved to the moment corresponding to `03:00`.
         *
         * Note that the preferred UTC offset is always ignored.
         */
        public val FIND_EARLIEST_VALID_TIME: TransitionHandler = object: TransitionHandler {
            override fun resolveDateTime(
                dateTime: LocalDateTime,
                transition: LocalDateTimeOffsetInfo.Transition,
                preferredOffset: UtcOffset?
            ): Instant = when (transition) {
                is LocalDateTimeOffsetInfo.Gap -> transition.transitionInstant
                is LocalDateTimeOffsetInfo.Overlap -> dateTime.toInstant(transition.offsetBefore)
            }
        }

        /**
         * Throw [IllegalArgumentException] if the datetime is non-existent or ambiguous.
         *
         * This strategy is suitable for when an input is expected to point
         * to a specific moment in time in the given time zone.
         * A [gap][LocalDateTimeOffsetInfo.Gap] always leads to an [IllegalArgumentException],
         * since a datetime in a gap does not correspond to a moment in time.
         * An [overlap][LocalDateTimeOffsetInfo.Overlap] throws an [IllegalArgumentException]
         * unless the preferred UTC offset is specified.
         *
         * [REJECT_TRANSITIONS] is a transition handler that unconditionally asserts that no transitions occur
         * and does not allow disambiguating overlaps.
         *
         * [USE_OFFSET_BEFORE] is a transition handler that leniently adapts datetimes instead of throwing exceptions,
         * similar to the `java.time` behavior for `ZonedDateTime`.
         */
        public val REQUIRE_RESOLVED: TransitionHandler = object: TransitionHandler {
            override fun resolveDateTime(
                dateTime: LocalDateTime,
                transition: LocalDateTimeOffsetInfo.Transition,
                preferredOffset: UtcOffset?
            ): Instant {
                require(transition is LocalDateTimeOffsetInfo.Overlap) {
                    "$dateTime corresponds to a gap: $transition"
                }
                require(preferredOffset != null) {
                    "$dateTime corresponds to an ambiguous time: $transition, " +
                            "and no suitable disambiguating offset was found."
                }
                return dateTime.toInstant(preferredOffset)
            }
        }

        /**
         * Throw [IllegalArgumentException] if a transition happens.
         *
         * This strategy is suitable for constant datetimes and time zones used in testing.
         * It ensures that the chosen datetimes are well-defined in the chosen time zones.
         *
         * ```
         * val dateTime = LocalDate(2026, 5, 18).atTime(15, 38)
         * // Assert that `dateTime` doesn't correspond to a transition in Europe/Berlin
         * val instant = dateTime.toInstant(TimeZone.of("Europe/Berlin"), TransitionHandler.REJECT_TRANSITIONS)
         * ```
         *
         * [REQUIRE_RESOLVED] is a similar strategy that allows handling overlaps more gracefully
         * by supplying the disambiguating [UtcOffset].
         */
        public val REJECT_TRANSITIONS: TransitionHandler = object: TransitionHandler {
            override fun resolveDateTime(
                dateTime: LocalDateTime,
                transition: LocalDateTimeOffsetInfo.Transition,
                preferredOffset: UtcOffset?
            ): Nothing {
                throw IllegalArgumentException(
                    "On $dateTime, there is a transition: $transition"
                )
            }
        }
    }
}
