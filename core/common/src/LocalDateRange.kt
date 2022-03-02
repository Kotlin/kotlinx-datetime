/*
 * Copyright 2019-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

public abstract class LocalDateIterator : Iterator<LocalDate> {
    final override fun next(): LocalDate = nextLocalDate()
    public abstract fun nextLocalDate(): LocalDate
}

internal class LocalDateProgressionIterator(first: LocalDate, last: LocalDate, val step: DatePeriod) : LocalDateIterator() {
    private val finalElement: LocalDate = last
    private val increasing = step.positive()
    private var hasNext: Boolean = if (increasing) first <= last else first >= last
    private var next: LocalDate = if (hasNext) first else finalElement

    override fun hasNext(): Boolean = hasNext

    override fun nextLocalDate(): LocalDate {
        if(!hasNext) throw NoSuchElementException()
        val value = next
        next += step
        /**
         * Some [DatePeriod]s with opposite-signed constituent parts can get stuck in an infinite loop rather than progressing toward the far future or far past.
         * A period of P1M-28D for example, when added to any date in February, will return that same date, thus leading to a loop.
         */
        if(next == value) throw IllegalStateException("Progression has hit an infinite loop. Check to ensure that the the values for total months and days in the provided step DatePeriod are not equal and opposite for certain month(s).")
        if ((increasing && next > finalElement) || (!increasing && next < finalElement)) {
            hasNext = false
        }
        return value
    }
}

public open class LocalDateProgression
internal constructor
    (
    start: LocalDate,
    endInclusive: LocalDate,
    public val step: DatePeriod
) : Iterable<LocalDate> {
    init {
        if(!step.positive() && !step.negative()) throw IllegalArgumentException("Provided step DatePeriod is of size zero (or equivalent over an arbitrarily long timeline)")
    }
    public val first: LocalDate = start
    public val last: LocalDate = endInclusive

    override fun iterator(): LocalDateIterator = LocalDateProgressionIterator(first, last, step)

    public open fun isEmpty(): Boolean = if (step.positive()) first > last else first < last


    override fun toString(): String = if (step.positive()) "$first..$last step $step" else "$first downTo $last step $step"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as LocalDateProgression

        if (first != other.first) return false
        if (last != other.last) return false
        if (step != other.step) return false

        return true
    }

    override fun hashCode(): Int {
        var result = first.hashCode()
        result = 31 * result + last.hashCode()
        result = 31 * result + step.hashCode()
        return result
    }

    public companion object {
        public fun fromClosedRange(rangeStart: LocalDate, rangeEnd: LocalDate, step: DatePeriod): LocalDateProgression = LocalDateProgression(rangeStart, rangeEnd, step)
    }
}

public class LocalDateRange(start: LocalDate, endInclusive: LocalDate) : LocalDateProgression(start, endInclusive, DatePeriod(days = 1)), ClosedRange<LocalDate> {
    override val start: LocalDate get() = first
    override val endInclusive: LocalDate get() = last

    @Suppress("ConvertTwoComparisonsToRangeCheck")
    override fun contains(value: LocalDate): Boolean = first <= value && value <= last

    override fun isEmpty(): Boolean = first > last

    override fun toString(): String = "$first..$last"

    public companion object {
        private val DATE_ONE = LocalDate(1, 1, 1)
        private val DATE_TWO = LocalDate(1, 1, 2)
        public val EMPTY: LocalDateRange = LocalDateRange(DATE_TWO, DATE_ONE)
    }
}

/**
 * On an arbitrarily long timeline, the average month will be 30.436875 days long (146097 days over 400 years).
 */
public fun DatePeriod.positive() : Boolean = totalMonths * 30.436875 + days > 0
public fun DatePeriod.negative() : Boolean = totalMonths * 30.436875 + days < 0

public infix fun LocalDateProgression.step(step: DatePeriod) : LocalDateProgression = LocalDateProgression.fromClosedRange(first, last, step)
public infix fun LocalDate.downTo(that: LocalDate) : LocalDateProgression = LocalDateProgression.fromClosedRange(this, that, DatePeriod(days = -1))

public operator fun LocalDate.rangeTo(that: LocalDate): LocalDateRange = LocalDateRange(this, that)