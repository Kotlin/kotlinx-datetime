/*
 * Copyright 2019-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.random.Random
import kotlin.random.nextInt

private class LocalDateProgressionIterator(private val iterator: IntIterator) : Iterator<LocalDate> {
    override fun hasNext(): Boolean = iterator.hasNext()
    override fun next(): LocalDate = LocalDate.fromEpochDays(iterator.next())
}

public open class LocalDateProgression
internal constructor(internal val intProgression: IntProgression) : Iterable<LocalDate> {

    internal constructor(
        start: LocalDate,
        endInclusive: LocalDate,
        step: Int
    ) : this(IntProgression.fromClosedRange(start.toEpochDays(), endInclusive.toEpochDays(), step))

    public val first: LocalDate = LocalDate.fromEpochDays(intProgression.first)
    public val last: LocalDate = LocalDate.fromEpochDays(intProgression.last)

    override fun iterator(): Iterator<LocalDate> = LocalDateProgressionIterator(intProgression.iterator())

    public open fun isEmpty(): Boolean = intProgression.isEmpty()

    override fun toString(): String = if (intProgression.step > 0) "$first..$last step ${intProgression.step}" else "$first downTo $last step ${intProgression.step}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as LocalDateProgression

        return intProgression == other.intProgression
    }

    override fun hashCode(): Int {
        return intProgression.hashCode()
    }


    public companion object {
        public fun fromClosedRange(
            rangeStart: LocalDate,
            rangeEnd: LocalDate,
            stepValue: Int,
            stepUnit: DateTimeUnit.DayBased
        ): LocalDateProgression = LocalDateProgression(rangeStart, rangeEnd, stepValue * stepUnit.days)
    }
}

public class LocalDateRange(start: LocalDate, endInclusive: LocalDate) : LocalDateProgression(start, endInclusive, 1), ClosedRange<LocalDate>, OpenEndRange<LocalDate> {
    override val start: LocalDate get() = first
    override val endInclusive: LocalDate get() = last
    override val endExclusive: LocalDate get() = endInclusive.plus(1, DateTimeUnit.DAY)

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

public fun LocalDateProgression.reversed(): LocalDateProgression = LocalDateProgression(intProgression.reversed())

public fun LocalDateProgression.step(value: Int, unit: DateTimeUnit.DayBased) : LocalDateProgression = LocalDateProgression(intProgression.step(value * unit.days))
public fun LocalDateProgression.step(value: Long, unit: DateTimeUnit.DayBased) : LocalDateProgression = step(value.toInt(), unit)

public infix fun LocalDate.downTo(that: LocalDate) : LocalDateProgression = LocalDateProgression.fromClosedRange(this, that, -1, DateTimeUnit.DAY)
public infix fun LocalDate.downUntil(that: LocalDate) : LocalDateProgression = downTo(that.plus(1, DateTimeUnit.DAY))

public operator fun LocalDate.rangeTo(that: LocalDate): LocalDateRange = LocalDateRange(this, that)
public operator fun LocalDate.rangeUntil(that: LocalDate) : LocalDateRange = rangeTo(that.minus(1, DateTimeUnit.DAY))

public fun LocalDateProgression.random(randomIntFunction: (IntRange) -> Int) : LocalDate = intProgression.random(randomIntFunction)
    .let(LocalDate.Companion::fromEpochDays)

public fun LocalDateProgression.random(random: Random = Random) : LocalDate = intProgression.random(random).let(LocalDate.Companion::fromEpochDays)

public fun LocalDateProgression.randomOrNull(randomIntFunction: (range: IntRange) -> Int) : LocalDate? = intProgression.randomOrNull(randomIntFunction)
    ?.let(LocalDate.Companion::fromEpochDays)

public fun LocalDateProgression.randomOrNull(random: Random = Random) : LocalDate? = intProgression.randomOrNull(random)
    ?.let(LocalDate.Companion::fromEpochDays)

public inline fun IntProgression.random(func: (IntRange) -> Int) : Int = func(0..(last - first) / step) * step + first

public fun IntProgression.random(random: Random = Random) : Int = random(random::nextInt)

public inline fun IntProgression.randomOrNull(randomIntFunction: (range: IntRange) -> Int) : Int? = if (isEmpty()) null else random(randomIntFunction)

public fun IntProgression.randomOrNull(random: Random = Random) : Int? = if (isEmpty()) null else random(random)
