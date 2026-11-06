/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*
import kotlin.math.*
import kotlin.time.Instant

internal class TimeZoneRules(
    /**
     * The list of [Instant.epochSeconds] parts of the instants when recorded transitions occur, in ascending order.
     */
    val transitionEpochSeconds: List<Long>,
    /**
     * The list of effective offsets.
     * The length is one more than the length of [transitionEpochSeconds].
     * The first element is the offset before the initial transition, and all the rest are the offsets after the
     * corresponding transitions.
     */
    val offsets: List<UtcOffset>,
    /**
     * The transition rules for recurring transitions.
     *
     * If not null, then the last element of [offsets] must be the offset that is in effect at the start of each year
     * after the last transition recorded in [transitionEpochSeconds], before the first transition recorded in
     * [recurringZoneRules].
     */
    val recurringZoneRules: RecurringZoneRules?,
    /**
     * The set of data for determining the standard offset.
     */
    val standardOffsetInformation: StandardOffsetInformation
) {
    init {
        require(offsets.size == transitionEpochSeconds.size + 1) {
            "offsets.size must be one more than transitionEpochSeconds.size"
        }
    }

    /**
     * Constructs a [TimeZoneRules] without any historic data.
     */
    constructor(initialOffset: UtcOffset, rules: RecurringZoneRules) : this(
        transitionEpochSeconds = emptyList(),
        offsets = listOf(initialOffset),
        recurringZoneRules = rules,
        standardOffsetInformation = StandardOffsetInformation.None,
    )

    /**
     * The list of [LocalDateTime] values related to the transitions.
     * The length is twice the length of [transitionEpochSeconds].
     * For each transition, there are two [LocalDateTime] elements in a row, one before the transition, and one after,
     * but reordered so that they are in ascending order to allow efficient binary search.
     */
    private val transitionLocalDateTimes: List<LocalDateTime> = buildList {
        for (i in transitionEpochSeconds.indices) {
            val instant = Instant.fromEpochSeconds(transitionEpochSeconds[i])
            val ldtBefore = instant.toLocalDateTime(offsets[i])
            val ldtAfter = instant.toLocalDateTime(offsets[i + 1])
            if (ldtBefore < ldtAfter) {
                add(ldtBefore)
                add(ldtAfter)
            } else {
                add(ldtAfter)
                add(ldtBefore)
            }
        }
    }

    fun infoAtInstant(instant: Instant): UtcOffset {
        val epochSeconds = instant.epochSeconds
        // good: no transitions, or instant is after the last transition
        if (recurringZoneRules != null && transitionEpochSeconds.lastOrNull()?.let { epochSeconds >= it } != false) {
            return recurringZoneRules.infoAtInstant(instant, offsets.last())
        }
        // an index in the [offsets] list of the offset that is in effect at the given instant,
        // which is the index of the first element that is greater than the given instant, plus one.
        val index = transitionEpochSeconds.binarySearch(epochSeconds).let {
            // if the exact value is not found, the returned value is (-insertionPoint - 1), but in that case, we want
            // the index of the element that is smaller than the searched value, so we look at (insertionPoint - 1).
            (it + 1).absoluteValue
        }
        return offsets[index]
    }

    fun infoAtDatetime(localDateTime: LocalDateTime): LocalDateTimeOffsetInfo {
        if (recurringZoneRules != null && transitionLocalDateTimes.lastOrNull()?.let { localDateTime > it } != false) {
            return recurringZoneRules.infoAtLocalDateTime(localDateTime, offsets.last())
        }
        val lastIndexNotBiggerThanLdt = transitionLocalDateTimes.binarySearch(localDateTime).let {
            // if the exact value is not found, the returned value is (-insertionPoint - 1), but in that case, we want
            // the index of the element that is smaller than the searched value, so we look at (insertionPoint - 1).
            if (it < 0) -it - 2 else it
        }
        if (lastIndexNotBiggerThanLdt == -1) {
            // before the first transition
            return LocalDateTimeOffsetInfo.Regular(offsets.first())
        }
        return if (lastIndexNotBiggerThanLdt % 2 == 0) {
            // inside a transition: after the smaller LDT but before the bigger one
            offsetInfoForTransitionIndex(lastIndexNotBiggerThanLdt / 2)
        } else if (lastIndexNotBiggerThanLdt != transitionLocalDateTimes.size - 1 &&
            transitionLocalDateTimes[lastIndexNotBiggerThanLdt] == transitionLocalDateTimes[lastIndexNotBiggerThanLdt + 1]
        ) {
            // seemingly outside a transition, but actually a transition happens right after the last one.
            // TODO: 310bp does this, but can this ever actually happen?
            offsetInfoForTransitionIndex(lastIndexNotBiggerThanLdt / 2 + 1)
        } else {
            // outside a transition
            LocalDateTimeOffsetInfo.Regular(offsets[lastIndexNotBiggerThanLdt / 2 + 1])
        }
    }

    fun standardOffsetInfoAtInstant(instant: Instant): UtcOffset {
        // See the explanation in `infoAtInstant`
        val epochSeconds = instant.epochSeconds
        if (recurringZoneRules != null && transitionEpochSeconds.lastOrNull()?.let { epochSeconds >= it } != false) {
            return recurringZoneRules.standardOffsetInfo(instant)
        }
        val index = transitionEpochSeconds.binarySearch(epochSeconds).let { (it + 1).absoluteValue }
        return standardOffsetInformation.standardOffsetInfoAtOffsetIndex(index, offsets)
    }

    private fun offsetInfoForTransitionIndex(transitionIndex: Int): LocalDateTimeOffsetInfo {
        val transitionInstant = Instant.fromEpochSeconds(transitionEpochSeconds[transitionIndex])
        val offsetBefore = offsets[transitionIndex]
        val offsetAfter = offsets[transitionIndex + 1]
        return LocalDateTimeOffsetInfo(transitionInstant, offsetBefore, offsetAfter)
    }

    override fun toString(): String = buildString {
        for (i in transitionEpochSeconds.indices) {
            append(offsets[i])
            append(" until ")
            append(Instant.fromEpochSeconds(transitionEpochSeconds[i]))
            append(", ")
        }
        append("then ")
        append(offsets.last())
        if (recurringZoneRules != null) {
            append(", after that ")
            append(recurringZoneRules)
        }
    }
}

internal class RecurringZoneRules(
    /**
     * The list of transitions that occur every year, in the order of occurrence.
     */
    val rules: List<Rule<MonthDayTime>>,
    val standardOffset: UtcOffset,
) {
    class Rule<T>(
        val transitionDateTime: T,
        val offsetBefore: UtcOffset,
        val offsetAfter: UtcOffset,
    ) {
        override fun toString(): String = "transitioning from $offsetBefore to $offsetAfter on $transitionDateTime"
    }

    // see `tzparse` in https://data.iana.org/time-zones/tzdb/localtime.c: looks like there's no guarantees about
    // a way to pre-sort the transitions, so we have to do it for each query separately.
    fun rulesForYear(year: Int): List<Rule<Instant>> {
        return rules.map { rule ->
            val transitionInstant = rule.transitionDateTime.toInstant(year, rule.offsetBefore)
            Rule(transitionInstant, rule.offsetBefore, rule.offsetAfter)
        }.sortedBy { it.transitionDateTime }
    }

    fun infoAtInstant(instant: Instant, offsetAtYearStart: UtcOffset): UtcOffset {
        val approximateYear = instant.toLocalDateTime(offsetAtYearStart).year
        var offset = offsetAtYearStart
        for (rule in rulesForYear(approximateYear)) {
            if (instant < rule.transitionDateTime) {
                return rule.offsetBefore
            }
            offset = rule.offsetAfter
        }
        return if (instant.toLocalDateTime(offset).year == approximateYear) {
            // [instant] is still in the same year, just after the last transition
            offset
        } else {
            // [instant] is in the next year, so we need to find the offset at the start of that year.
            // This will converge in the next iteration, because then, the year will be correct.
            infoAtInstant(instant, offset)
        }
    }

    fun infoAtLocalDateTime(localDateTime: LocalDateTime, offsetAtYearStart: UtcOffset): LocalDateTimeOffsetInfo {
        val year = localDateTime.year
        var offset = offsetAtYearStart
        for (rule in rulesForYear(year)) {
            val ldtBefore = rule.transitionDateTime.toLocalDateTime(rule.offsetBefore)
            val ldtAfter = rule.transitionDateTime.toLocalDateTime(rule.offsetAfter)
            return if (localDateTime < ldtBefore && localDateTime < ldtAfter) {
                LocalDateTimeOffsetInfo.Regular(rule.offsetBefore)
            } else if (localDateTime >= ldtBefore && localDateTime >= ldtAfter) {
                offset = rule.offsetAfter
                continue
            } else if (ldtAfter < ldtBefore) {
                LocalDateTimeOffsetInfo.Overlap(rule.transitionDateTime, rule.offsetBefore, rule.offsetAfter)
            } else {
                LocalDateTimeOffsetInfo.Gap(rule.transitionDateTime, rule.offsetBefore, rule.offsetAfter)
            }
        }
        return LocalDateTimeOffsetInfo.Regular(offset)
    }

    fun standardOffsetInfo(instant: Instant): UtcOffset = standardOffset

    override fun toString(): String = rules.joinToString(", ")
}

internal interface StandardOffsetInformation {
    /**
     * The information about the standard offset is inferred from which offsets are themselves standard
     * and which are DST.
     */
    class StandardOrDaylightFlag(val isDsts: BooleanArray): StandardOffsetInformation {
        override fun standardOffsetInfoAtOffsetIndex(offsetIndex: Int, offsets: List<UtcOffset>): UtcOffset = when {
            // Is the *current* offset a standard offset?
            !isDsts[offsetIndex] -> offsets[offsetIndex]
            /* Patching up a common case: `DST(u1); |DST(u2)|; STD(u3)`
            the previous transition happened during the DST and changed the standard offset.
            The likely standard offset is `u3`. */
            isDsts.getOrNull(offsetIndex - 1) != false && isDsts.getOrNull(offsetIndex + 1) == false ->
                offsets[offsetIndex + 1]
            /* Patching up a common case: `STD(u1) ; |DST(u1)| ; STD(u2)`
            the previous standard offset becomes a DST offset, with no clocks moving.
            The likely standard offset is `u2`. */
            isDsts.getOrNull(offsetIndex - 1) == false && offsets[offsetIndex - 1] == offsets[offsetIndex] -> {
                for (j in offsetIndex + 1..<offsets.size) {
                    if (!isDsts[j]) {
                        return offsets[j]
                    }
                }
                offsets[offsetIndex - 1]
            }
            else -> {
                /* The default implementation: recreating the `tz` behavior when the standard offset is requested:
                <https://github.com/eggert/tz/blob/e29969d3f327f129ec4aebaa9c7aee96c46f8e3b/localtime.c#L771-L774>
                rolling back until we find any non-DST offset. */
                for (i in offsetIndex - 1 downTo 1) {
                    if (!isDsts[i]) {
                        return offsets[i]
                    }
                }
                offsets[0]
            }
        }
    }

    /**
     * The information about the standard offset is the standard offset corresponding to the given normal offset.
     */
    class ExactStandardOffset(val standardOffsets: List<UtcOffset>): StandardOffsetInformation {
        override fun standardOffsetInfoAtOffsetIndex(offsetIndex: Int, offsets: List<UtcOffset>): UtcOffset =
            return standardOffsets[offsetIndex]
    }

    object None: StandardOffsetInformation {
        override fun standardOffsetInfoAtOffsetIndex(offsetIndex: Int, offsets: List<UtcOffset>): Nothing =
            throw UnsupportedOperationException(
                "Use TimeZoneContext.Bundled from kotlinx-datetime-zoneinfo to obtain standard offset information"
            )
    }

    fun standardOffsetInfoAtOffsetIndex(offsetIndex: Int, offsets: List<UtcOffset>): UtcOffset
}

