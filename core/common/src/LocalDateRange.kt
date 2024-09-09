/*
 * Copyright 2019-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.clampToInt
import kotlinx.datetime.internal.safeAdd
import kotlinx.datetime.internal.safeMultiply
import kotlin.random.Random
import kotlin.random.nextLong

private class LocalDateProgressionIterator(private val iterator: LongIterator) : Iterator<LocalDate> {
    override fun hasNext(): Boolean = iterator.hasNext()
    override fun next(): LocalDate = LocalDate.fromEpochDays(iterator.next())
}

public open class LocalDateProgression
internal constructor(internal val longProgression: LongProgression) : Collection<LocalDate> {

    internal constructor(
        start: LocalDate,
        endInclusive: LocalDate,
        step: Long
    ) : this(LongProgression.fromClosedRange(start.toEpochDaysLong(), endInclusive.toEpochDaysLong(), step))

    public val first: LocalDate = LocalDate.fromEpochDays(longProgression.first)

    public val last: LocalDate = LocalDate.fromEpochDays(longProgression.last)

    override fun iterator(): Iterator<LocalDate> = LocalDateProgressionIterator(longProgression.iterator())

    public override fun isEmpty(): Boolean = longProgression.isEmpty()

    override fun toString(): String = if (longProgression.step > 0) "$first..$last step ${longProgression.step}D" else "$first downTo $last step ${longProgression.step}D"

    override val size: Int
        get() = longProgression.size

    override fun containsAll(elements: Collection<LocalDate>): Boolean = elements.all(::contains)

    override fun contains(element: LocalDate): Boolean = longProgression.contains(element.toEpochDaysLong())

    override fun equals(other: Any?): Boolean = other is LocalDateProgression && longProgression == other.longProgression

    override fun hashCode(): Int = longProgression.hashCode()

    public companion object {
        internal fun fromClosedRange(
            rangeStart: LocalDate,
            rangeEnd: LocalDate,
            stepValue: Long,
            stepUnit: DateTimeUnit.DayBased
        ): LocalDateProgression = LocalDateProgression(rangeStart, rangeEnd, safeMultiply(stepValue, stepUnit.days.toLong()))
    }
}

public class LocalDateRange(start: LocalDate, endInclusive: LocalDate) : LocalDateProgression(start, endInclusive, 1), ClosedRange<LocalDate>, OpenEndRange<LocalDate> {
    override val start: LocalDate get() = first
    override val endInclusive: LocalDate get() = last
    @Deprecated(
        "This throws an exception if the exclusive end if not inside " +
                "the platform-specific boundaries for LocalDate. " +
                "The 'endInclusive' property does not throw and should be preferred.",
        level = DeprecationLevel.WARNING
    )
    override val endExclusive: LocalDate get(){
        if (last == LocalDate.MAX) error("Cannot return the exclusive upper bound of a range that includes LocalDate.MAX.")
        return endInclusive.plus(1, DateTimeUnit.DAY)
    }

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

public fun LocalDateProgression.first(): LocalDate {
    if (isEmpty())
        throw NoSuchElementException("Progression $this is empty.")
    return this.first
}
public fun LocalDateProgression.last(): LocalDate {
    if (isEmpty())
        throw NoSuchElementException("Progression $this is empty.")
    return this.last
}
public fun LocalDateProgression.firstOrNull(): LocalDate? = if (isEmpty()) null else this.first

public fun LocalDateProgression.lastOrNull(): LocalDate? = if (isEmpty()) null else this.last

public fun LocalDateProgression.reversed(): LocalDateProgression = LocalDateProgression(longProgression.reversed())

public fun LocalDateProgression.step(value: Int, unit: DateTimeUnit.DayBased) : LocalDateProgression = step(value.toLong(), unit)
public fun LocalDateProgression.step(value: Long, unit: DateTimeUnit.DayBased) : LocalDateProgression = LocalDateProgression(longProgression.step(safeMultiply(value, unit.days.toLong())))

public infix fun LocalDate.downTo(that: LocalDate) : LocalDateProgression = LocalDateProgression.fromClosedRange(this, that, -1, DateTimeUnit.DAY)

public operator fun LocalDate.rangeTo(that: LocalDate): LocalDateRange = LocalDateRange(this, that)
public operator fun LocalDate.rangeUntil(that: LocalDate) : LocalDateRange = rangeTo(that.minus(1, DateTimeUnit.DAY))

public fun LocalDateProgression.random(random: Random = Random) : LocalDate = longProgression.random(random).let(LocalDate.Companion::fromEpochDays)

public fun LocalDateProgression.randomOrNull(random: Random = Random) : LocalDate? = longProgression.randomOrNull(random)
    ?.let(LocalDate.Companion::fromEpochDays)

internal fun LongProgression.random(random: Random = Random) : Long = random.nextLong(0L..(last - first) / step) * step + first

internal fun LongProgression.randomOrNull(random: Random = Random) : Long? = if (isEmpty()) null else random(random)

internal fun LongProgression.contains(element: Long) : Boolean = element in first..last && (element - first) % step == 0L

internal val LongProgression.size: Int
    get() = if(isEmpty()) 0 else try {
        (safeAdd(last, -first) / step + 1).clampToInt()
    } catch (e: ArithmeticException) {
        Int.MAX_VALUE
    }