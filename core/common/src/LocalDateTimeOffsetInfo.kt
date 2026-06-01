/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.time.Instant

/**
 * The information about the offsets from UTC corresponding to a [LocalDateTime] value in some [time zone][TimeZone].
 *
 * A [LocalDateTime] represents a reading from a wall clock (for example, `2026-05-15T12:06:30`),
 * and not every wall clock reading corresponds to a specific moment in time.
 *
 * Examples:
 *
 * - In Berlin, on `2021-03-28`, clocks were moved forward from `02:00` to `03:00`.
 *   Clock readings between `02:00` and `02:59` never happened.
 *   We say that a ["time gap"][LocalDateTimeOffsetInfo.Gap] occurred.
 * - In Berlin, on `2021-10-31`, clocks were moved back from `03:00` to `02:00`.
 *   Therefore, clock readings between `02:00` and `02:59` were each observed twice.
 *   We say that a ["time overlap"][LocalDateTimeOffsetInfo.Overlap] occurred.
 *
 * A [LocalDateTimeOffsetInfo] instance represents one of the three situations:
 * a [regular][LocalDateTimeOffsetInfo.Regular] case if a clock reading corresponds to exactly one moment in time,
 * a [gap][LocalDateTimeOffsetInfo.Gap] if a clock reading didn't occur at all,
 * or an [overlap][LocalDateTimeOffsetInfo.Overlap] if a clock reading happened twice.
 *
 * ### Usage
 *
 * This is a `sealed` interface, which allows exhaustively matching the three cases with `when`:
 *
 * ```
 * fun LocalDateTimeOffsetInfo.offsetDescription(): String = when (this) {
 *     is LocalDateTimeOffsetInfo.Regular ->
 *         "The unique UTC offset is $offset"
 *     is LocalDateTimeOffsetInfo.Gap ->
 *         "No corresponding UTC offset: the datetime was skipped by moving from $offsetBefore to $offsetAfter"
 *     is LocalDateTimeOffsetInfo.Overlap ->
 *         "Two possible UTC offsets: $offsetBefore and $offsetAfter"
 * }
 * ```
 *
 * There is additionally an intermediate sealed interface for [Gap][LocalDateTimeOffsetInfo.Gap] and
 * [Overlap][LocalDateTimeOffsetInfo.Overlap] called [Transition][LocalDateTimeOffsetInfo.Transition].
 *
 * ### Construction
 *
 * Normally, instances of [LocalDateTimeOffsetInfo] are obtained by querying a [TimeZone]
 * for information about a [LocalDateTime] using [TimeZone.offsetInfoFor].
 *
 * [LocalDateTimeOffsetInfo.Regular], [LocalDateTimeOffsetInfo.Gap], and [LocalDateTimeOffsetInfo.Overlap]
 * constructors can be used to manually construct an instance.
 * For convenience, the [LocalDateTimeOffsetInfo.Transition] function is provided to construct either a
 * `Gap` or an `Overlap`, depending on whether the clocks were moved forward or backward.
 */
public sealed interface LocalDateTimeOffsetInfo {
    /**
     * The unique UTC offset corresponding to a [LocalDateTime] in some [time zone][TimeZone].
     *
     * See [LocalDateTimeOffsetInfo] for general information about UTC offset information for [LocalDateTime].
     *
     * The [offset] field contains the UTC offset in effect at the implied [LocalDateTime] in the implied [TimeZone].
     */
    public class Regular(public val offset: UtcOffset) : LocalDateTimeOffsetInfo {
        override fun toString(): String = "Regular($offset)"

        override fun equals(other: Any?): Boolean = other is Regular && this.offset == other.offset

        override fun hashCode(): Int = offset.hashCode()
    }

    /**
     * A transition between two UTC offsets.
     *
     * The most common example of transitions is the moments clocks are moved forward or backward
     * to account for daylight saving time.
     * [offsetBefore] represents the UTC offset in effect before the clocks are moved,
     * [offsetAfter] represents the UTC offset just after that, and
     * [transitionInstant] is the moment the clocks are moved.
     *
     * This is a common supertype for [Gap] and [Overlap].
     * There are no other implementations of this `sealed` interface, so it is possible to match
     * a [Transition] using `when` exhaustively:
     *
     * ```
     * val transitionDescription = when (transition) {
     *     is LocalDateTimeOffsetInfo.Gap -> {
     *         "Gap occurred when transitioning " +
     *             "from a lower UTC offset (${transition.offsetBefore}) " +
     *             "to a bigger one (${transition.offsetAfter})."
     *     }
     *     is LocalDateTimeOffsetInfo.Overlap -> {
     *         "Overlap occurred when transitioning " +
     *             "from a bigger UTC offset (${transition.offsetBefore}) " +
     *             "to a lower one (${transition.offsetAfter})."
     *     }
     * }
     * ```
     *
     * [Gap] and [Overlap] do not carry any additional information compared to [Transition].
     * A [Gap] is a [Transition] with `offsetBefore.totalSeconds < offsetAfter.totalSeconds`
     * (for example, transitioning from `+01:00` to `+02:00`),
     * whereas an [Overlap] is a [Transition] with `offsetBefore.totalSeconds > offsetAfter.totalSeconds`
     * (for example, transitioning from `+02:00` to `+01:00`).
     * The [Gap] and [Overlap] are provided for matching using `is`-checks,
     * which is more streamlined than comparing [offsetBefore] with [offsetAfter] manually.
     *
     * For convenience, the [LocalDateTimeOffsetInfo.Transition] function is provided to construct either a
     * [Gap] or an [Overlap], depending on whether the clocks were moved forward or backward.
     */
    public sealed interface Transition : LocalDateTimeOffsetInfo {
        /**
         * The moment right after the clocks were moved.
         */
        public val transitionInstant: Instant

        /**
         * The UTC offset in effect before the transition.
         */
        public val offsetBefore: UtcOffset

        /**
         * The UTC offset in effect after the transition.
         */
        public val offsetAfter: UtcOffset
    }

    /**
     * A time gap, the type of [Transition] that happens when clocks are moved forward.
     *
     * A [Gap] is a [Transition] with `offsetBefore.totalSeconds < offsetAfter.totalSeconds`.
     * The other type of [Transition] is called [Overlap].
     *
     * Example of a gap where the clocks are moved from `02:00` to `03:00`:
     *
     * ```
     *  01:30 02:00 02:30 03:00
     * ...|-----)                       offsetBefore = +01:00
     *                      [-----|...  offsetAfter  = +02:00
     *                    03:00 03:30
     * ```
     *
     * A [Gap] can be constructed by calling its constructor, which will fail with an [IllegalArgumentException]
     * if the UTC offset before isn't lower than the UTC offset after the transition.
     * Another way to obtain a [Gap] is to call the [Transition] constructor function
     * and then downcast its result.
     */
    public class Gap(
        /**
         * The first moment after the clocks were moved forward.
         *
         * For example, if the clocks were moved from `02:00` to `03:00`,
         * this is the moment the clocks start showing `03:00`.
         */
        override val transitionInstant: Instant,
        override val offsetBefore: UtcOffset,
        override val offsetAfter: UtcOffset,
    ) : Transition {
        init {
            require(offsetBefore.totalSeconds < offsetAfter.totalSeconds) {
                if (offsetBefore.totalSeconds == offsetAfter.totalSeconds) {
                    "During a time gap, the UTC offset changes, but both 'offsetBefore' and 'offsetAfter' " +
                        "are $offsetBefore. " +
                        "Use Regular to represent UTC offset information that is not a transition."
                } else {
                    "For a time gap, 'offsetBefore' must be less than 'offsetAfter', " +
                        "got 'offsetBefore' = $offsetBefore, 'offsetAfter' = $offsetAfter. " +
                        "Use Overlap to construct overlaps, or Transition to automatically detect the transition type."
                }
            }
        }

        override fun toString(): String =
            "Gap($transitionInstant, offsetBefore=$offsetBefore, offsetAfter=$offsetAfter)"

        override fun equals(other: Any?): Boolean =
            other is Gap && this.transitionInstant == other.transitionInstant
                && this.offsetBefore == other.offsetBefore && this.offsetAfter == other.offsetAfter

        override fun hashCode(): Int =
            transitionInstant.hashCode() xor offsetBefore.hashCode() xor offsetAfter.hashCode() xor 13
    }

    /**
     * A time overlap, the type of [Transition] that happens when clocks are moved backward.
     *
     * An [Overlap] is a [Transition] with `offsetBefore.totalSeconds > offsetAfter.totalSeconds`.
     * The other type of [Transition] is called [Gap].
     *
     * Example of an overlap where the clocks are moved from `03:00` to `02:00`:
     *
     * ```
     *  01:30 02:00 02:30 03:00
     * ...|-----|-----|-----)           offsetBefore = +02:00
     *          [-----|-----|-----|...  offsetAfter  = +01:00
     *        02:00 02:30 03:00 03:30
     * ```
     *
     * An [Overlap] can be constructed by calling its constructor, which will fail with an [IllegalArgumentException]
     * if the UTC offset before isn't greater than the UTC offset after the transition.
     * Another way to obtain an [Overlap] is to call the [Transition] constructor function
     * and then downcast its result.
     */
    public class Overlap(
        /**
         * The first moment after the clocks were moved backward.
         *
         * For example, if the clocks were moved from `03:00` to `02:00`,
         * this is the moment the clocks start showing `02:00` for the second time.
         */
        override val transitionInstant: Instant,
        override val offsetBefore: UtcOffset,
        override val offsetAfter: UtcOffset,
    ) : Transition {
        init {
            require(offsetBefore.totalSeconds > offsetAfter.totalSeconds) {
                if (offsetBefore.totalSeconds == offsetAfter.totalSeconds) {
                    "During a time overlap, the UTC offset changes, but both 'offsetBefore' and 'offsetAfter' " +
                        "are $offsetBefore. " +
                        "Use Regular to represent UTC offset information that is not a transition."
                } else {
                    "In a time overlap, 'offsetBefore' must be larger than 'offsetAfter', " +
                        "got 'offsetBefore' = $offsetBefore, 'offsetAfter' = $offsetAfter. " +
                        "Use Gap to construct gaps, or Transition to automatically detect the transition type."
                }
            }
        }

        override fun toString(): String =
            "Overlap($transitionInstant, offsetBefore=$offsetBefore, offsetAfter=$offsetAfter)"

        override fun equals(other: Any?): Boolean =
            other is Overlap && this.transitionInstant == other.transitionInstant &&
                this.offsetBefore == other.offsetBefore && this.offsetAfter == other.offsetAfter

        override fun hashCode(): Int =
            transitionInstant.hashCode() xor offsetBefore.hashCode() xor offsetAfter.hashCode() xor 17
    }

    /** @suppress */
    public companion object {
        /**
         * Constructor function for the [Transition] interface.
         *
         * This function returns either a [Gap] or an [Overlap],
         * depending on whether `offsetBefore.totalSeconds` is greater or lower than `offsetAfter.totalSeconds`.
         *
         * An [IllegalArgumentException] is thrown if [offsetBefore] and [offsetAfter] are exactly equal,
         * indicating that no transition has actually happened.
         */
        public fun Transition(
            transitionInstant: Instant,
            offsetBefore: UtcOffset,
            offsetAfter: UtcOffset,
        ): Transition {
            val totalSecondsBefore = offsetBefore.totalSeconds
            val totalSecondsAfter = offsetAfter.totalSeconds
            require(totalSecondsBefore != totalSecondsAfter) {
                "Offsets before and after transition must be different, but were $offsetBefore and $offsetAfter"
            }
            return if (totalSecondsBefore < totalSecondsAfter) Gap(transitionInstant, offsetBefore, offsetAfter)
            else Overlap(transitionInstant, offsetBefore, offsetAfter)
        }
    }
}

internal fun LocalDateTimeOffsetInfo(
    transitionInstant: Instant,
    offsetBefore: UtcOffset,
    offsetAfter: UtcOffset,
): LocalDateTimeOffsetInfo {
    val totalSecondsBefore = offsetBefore.totalSeconds
    val totalSecondsAfter = offsetAfter.totalSeconds
    return when {
        totalSecondsBefore == totalSecondsAfter -> LocalDateTimeOffsetInfo.Regular(offsetBefore)
        totalSecondsBefore < totalSecondsAfter -> LocalDateTimeOffsetInfo.Gap(transitionInstant, offsetBefore, offsetAfter)
        else -> LocalDateTimeOffsetInfo.Overlap(transitionInstant, offsetBefore, offsetAfter)
    }
}