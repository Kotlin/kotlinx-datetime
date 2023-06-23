/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*
import kotlin.math.*

internal sealed interface OffsetInfo

internal sealed interface AbnormalOffsetInfo : OffsetInfo {
    val start: Instant
    val offsetBefore: UtcOffset
    val offsetAfter: UtcOffset
}

internal fun AbnormalOffsetInfo(start: Instant, offsetBefore: UtcOffset, offsetAfter: UtcOffset): AbnormalOffsetInfo =
    if (offsetBefore.totalSeconds < offsetAfter.totalSeconds) Gap(start, offsetBefore, offsetAfter)
    else Overlap(start, offsetBefore, offsetAfter)

internal data class Gap(
    override val start: Instant,
    override val offsetBefore: UtcOffset,
    override val offsetAfter: UtcOffset
) : AbnormalOffsetInfo {
    init {
        check(offsetBefore.totalSeconds < offsetAfter.totalSeconds)
    }

    val transitionDurationSeconds: Int get() = offsetAfter.totalSeconds - offsetBefore.totalSeconds
}

internal data class Overlap(
    override val start: Instant,
    override val offsetBefore: UtcOffset,
    override val offsetAfter: UtcOffset
) : AbnormalOffsetInfo {
    init {
        check(offsetBefore.totalSeconds > offsetAfter.totalSeconds)
    }
}

internal data class Regular(
    val offset: UtcOffset
) : OffsetInfo

/**
 * A rule expressing how to create a date in a given year.
 *
 * Some examples of expressible dates:
 *  * the 16th March
 *  * the Sunday on or after the 16th March
 *  * the Sunday on or before the 16th March
 *  * the last Sunday in February
 *  * the 300th day of the year
 *  * the last day of February
 */
internal interface DateOfYear {
    /**
     * Converts this date-time to an [Instant] in the given [year],
     * using the knowledge of the offset that's in effect at the resulting date-time.
     */
    fun toLocalDate(year: Int): LocalDate
}

/**
 * The day of year, in the 1..365 range. During leap years, 29th February is counted as the 60th day of the year.
 * The number 366 is not supported, as outside the leap years, there are only 365 days in a year.
 */
internal class JulianDayOfYear(val dayOfYear: Int) : DateOfYear {
    init {
        require(dayOfYear in 1..365)
    }
    override fun toLocalDate(year: Int): LocalDate =
        LocalDate(year, 1, 1).plusDays(dayOfYear - 1)

    override fun toString(): String = "JulianDayOfYear($dayOfYear)"
}

/**
 * The day of year, in the 1..365 range. During leap years, 29th February is skipped.
 */
internal fun JulianDayOfYearSkippingLeapDate(dayOfYear: Int) : DateOfYear {
    require(dayOfYear in 1..365)
    // In this form, the `dayOfYear` corresponds exactly to a specific month and day.
    // For example, `dayOfYear = 60` is always 1st March, even in leap years.
    // We take a non-leap year, as in that case, this is the same as JulianDayOfYear, so regular addition works.
    val date = LocalDate(2011, 1, 1).plusDays(dayOfYear - 1)
    return MonthDayOfYear(date.month, MonthDayOfYear.TransitionDay.ExactlyDayOfMonth(date.dayOfMonth))
}

internal class MonthDayOfYear(val month: Month, val day: TransitionDay) : DateOfYear {
    override fun toLocalDate(year: Int): LocalDate = day.resolve(year, month)

    /**
     * The day of month when the transition occurs.
     */
    sealed interface TransitionDay {
        /**
         * The first given [dayOfWeek] of the month that is not earlier than [atLeastDayOfMonth].
         */
        class First(val dayOfWeek: DayOfWeek, val atLeastDayOfMonth: Int?) : TransitionDay {
            override fun resolve(year: Int, month: Month): LocalDate =
                LocalDate(year, month, atLeastDayOfMonth ?: 1).nextOrSame(dayOfWeek)

            override fun toString(): String = "the first $dayOfWeek" +
                (atLeastDayOfMonth?.let { " on or after $it" } ?: "")
        }

        companion object {
            /**
             * The [n]th given [dayOfWeek] in the month.
             */
            fun Nth(dayOfWeek: DayOfWeek, n: Int): TransitionDay =
                First(dayOfWeek, n * 7 + 1)
        }

        /**
         * The last given [dayOfWeek] of the month that is not later than [atMostDayOfMonth].
         */
        class Last(val dayOfWeek: DayOfWeek, val atMostDayOfMonth: Int?) : TransitionDay {
            override fun resolve(year: Int, month: Month): LocalDate {
                val dayOfMonth = atMostDayOfMonth ?: month.number.monthLength(isLeapYear(year))
                return LocalDate(year, month, dayOfMonth).previousOrSame(dayOfWeek)
            }

            override fun toString(): String = "the last $dayOfWeek" +
                (atMostDayOfMonth?.let { " on or before $it" } ?: "")
        }

        /**
         * Exactly the given [dayOfMonth].
         */
        class ExactlyDayOfMonth(val dayOfMonth: Int) : TransitionDay {
            override fun resolve(year: Int, month: Month): LocalDate = LocalDate(year, month, dayOfMonth)

            override fun toString(): String = "$dayOfMonth"
        }

        fun resolve(year: Int, month: Month): LocalDate
    }

    override fun toString(): String = "$month, $day"
}

internal class MonthDayTime(
    /**
     * The date.
     */
    val date: DateOfYear,
    /**
     * The procedure to calculate the local time.
     */
    val time: TransitionLocaltime,
    /**
     * The definition of how the offset in which the local date-time is expressed.
     */
    val offset: OffsetResolver,
) {

    /**
     * Converts this [MonthDayTime] to an [Instant] in the given [year],
     * using the knowledge of the offset that's in effect at the resulting date-time.
     */
    fun toInstant(year: Int, effectiveOffset: UtcOffset): Instant {
        val localDateTime = time.resolve(date.toLocalDate(year))
        return when (this.offset) {
            is OffsetResolver.WallClockOffset -> localDateTime.toInstant(effectiveOffset)
            is OffsetResolver.FixedOffset -> localDateTime.toInstant(this.offset.offset)
        }
    }

    /**
     * Describes how the offset in which the local date-time is expressed is defined.
     */
    sealed interface OffsetResolver {
        /**
         * The offset is the one currently used by the wall clock.
         */
        object WallClockOffset : OffsetResolver {
            override fun toString(): String = "wall clock offset"
        }

        /**
         * The offset is fixed to a specific value.
         */
        class FixedOffset(val offset: UtcOffset) : OffsetResolver {
            override fun toString(): String = offset.toString()
        }
    }

    /**
     * The local time of day at which the transition occurs.
     */
    class TransitionLocaltime(val seconds: Int) {
        constructor(time: LocalTime) : this(time.toSecondOfDay())

        constructor(hour: Int, minute: Int, second: Int) : this(hour * 3600 + minute * 60 + second)

        fun resolve(date: LocalDate): LocalDateTime = date.atTime(LocalTime(0, 0)).plusSeconds(seconds)

        override fun toString(): String = if (seconds < 86400)
            LocalTime.ofSecondOfDay(seconds, 0).toString() else "$seconds seconds since the day start"
    }

    override fun toString(): String = "$date, $time, $offset"
}

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
) {
    init {
        require(offsets.size == transitionEpochSeconds.size + 1) {
            "offsets.size must be one more than transitionEpochSeconds.size"
        }
    }

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

    fun infoAtDatetime(localDateTime: LocalDateTime): OffsetInfo {
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
            return Regular(offsets.first())
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
            Regular(offsets[lastIndexNotBiggerThanLdt / 2 + 1])
        }
    }

    private fun offsetInfoForTransitionIndex(transitionIndex: Int): OffsetInfo {
        val transitionInstant = Instant.fromEpochSeconds(transitionEpochSeconds[transitionIndex])
        val offsetBefore = offsets[transitionIndex]
        val offsetAfter = offsets[transitionIndex + 1]
        return AbnormalOffsetInfo(transitionInstant, offsetBefore, offsetAfter)
    }
}

internal class RecurringZoneRules(
    /**
     * The list of transitions that occur every year, in the order of occurrence.
     */
    val rules: List<Rule>
) {
    class Rule(
        val transitionDateTime: MonthDayTime,
        val offsetAfter: UtcOffset,
    ) {
        override fun toString(): String = "transitioning to $offsetAfter on $transitionDateTime"
    }

    fun infoAtInstant(instant: Instant, offsetAtYearStart: UtcOffset): UtcOffset {
        val approximateYear = instant.toLocalDateTime(offsetAtYearStart).year
        var offset = offsetAtYearStart
        for (rule in rules) {
            val transitionInstant = rule.transitionDateTime.toInstant(approximateYear, offset)
            if (instant < transitionInstant) {
                return offset
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

    fun infoAtLocalDateTime(localDateTime: LocalDateTime, offsetAtYearStart: UtcOffset): OffsetInfo {
        val year = localDateTime.year
        var offset = offsetAtYearStart
        for (rule in rules) {
            val transitionInstant = rule.transitionDateTime.toInstant(year, offset)
            val ldtBefore = transitionInstant.toLocalDateTime(offset)
            val ldtAfter = transitionInstant.toLocalDateTime(rule.offsetAfter)
            return if (localDateTime < ldtBefore && localDateTime < ldtAfter) {
                Regular(offset)
            } else if (localDateTime > ldtBefore && localDateTime >= ldtAfter) {
                offset = rule.offsetAfter
                continue
            } else if (ldtAfter < ldtBefore) {
                Overlap(transitionInstant, offset, rule.offsetAfter)
            } else {
                Gap(transitionInstant, offset, rule.offsetAfter)
            }
        }
        return Regular(offset)
    }

    override fun toString(): String = rules.joinToString(", ")
}
