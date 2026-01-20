/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toLocalDateTime
import kotlin.math.*
import kotlin.time.Instant

internal interface TimeZoneRules {

    fun infoAtInstant(instant: Instant): UtcOffset

    fun infoAtDatetime(localDateTime: LocalDateTime): OffsetInfo
}

internal class TimeZoneRulesCommon(
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
) : TimeZoneRules {
    init {
        require(offsets.size == transitionEpochSeconds.size + 1) {
            "offsets.size must be one more than transitionEpochSeconds.size"
        }
    }

    /**
     * Constructs a [TimeZoneRulesCommon] without any historic data.
     */
    constructor(initialOffset: UtcOffset, rules: RecurringZoneRules) : this(
        transitionEpochSeconds = emptyList(),
        offsets = listOf(initialOffset),
        recurringZoneRules = rules,
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

    override fun infoAtInstant(instant: Instant): UtcOffset {
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

    override fun infoAtDatetime(localDateTime: LocalDateTime): OffsetInfo {
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
            return OffsetInfo.Regular(offsets.first())
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
            OffsetInfo.Regular(offsets[lastIndexNotBiggerThanLdt / 2 + 1])
        }
    }

    private fun offsetInfoForTransitionIndex(transitionIndex: Int): OffsetInfo {
        val transitionInstant = Instant.fromEpochSeconds(transitionEpochSeconds[transitionIndex])
        val offsetBefore = offsets[transitionIndex]
        val offsetAfter = offsets[transitionIndex + 1]
        return OffsetInfo(transitionInstant, offsetBefore, offsetAfter)
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
    val rules: List<Rule<MonthDayTime>>
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

    /**
     * IMPORTANT: keep this implementation in sync with [TimeZoneRulesCommon.infoAtDatetime].
     * The algorithms and corner-case handling should stay identical so that Darwin (Foundation-based)
     * and tzdb-based platforms compute the same results.  When you change logic here, reflect the
     * same change in [TimeZoneRulesCommon.infoAtDatetime].
     */
    fun infoAtLocalDateTime(localDateTime: LocalDateTime, offsetAtYearStart: UtcOffset): OffsetInfo {
        val year = localDateTime.year
        var offset = offsetAtYearStart
        for (rule in rulesForYear(year)) {
            val ldtBefore = rule.transitionDateTime.toLocalDateTime(rule.offsetBefore)
            val ldtAfter = rule.transitionDateTime.toLocalDateTime(rule.offsetAfter)
            return if (localDateTime < ldtBefore && localDateTime < ldtAfter) {
                OffsetInfo.Regular(rule.offsetBefore)
            } else if (localDateTime >= ldtBefore && localDateTime >= ldtAfter) {
                offset = rule.offsetAfter
                continue
            } else if (ldtAfter < ldtBefore) {
                OffsetInfo.Overlap(rule.transitionDateTime, rule.offsetBefore, rule.offsetAfter)
            } else {
                OffsetInfo.Gap(rule.transitionDateTime, rule.offsetBefore, rule.offsetAfter)
            }
        }
        return OffsetInfo.Regular(offset)
    }

    override fun toString(): String = rules.joinToString(", ")
}
