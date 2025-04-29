/*
 * Copyright 2019-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.*
import kotlin.random.Random

private class YearMonthProgressionIterator(private val iterator: LongIterator) : Iterator<YearMonth> {
    override fun hasNext(): Boolean = iterator.hasNext()
    override fun next(): YearMonth = YearMonth.fromProlepticMonth(iterator.next())
}

/**
 * A progression of values of type [YearMonth].
 *
 * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.progressionWithStep
 * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.reversedProgression
 * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.firstAndLast
 */
public open class YearMonthProgression
internal constructor(internal val longProgression: LongProgression) : Collection<YearMonth> {

    internal constructor(
        start: YearMonth,
        endInclusive: YearMonth,
        step: Long
    ) : this(LongProgression.fromClosedRange(start.prolepticMonth, endInclusive.prolepticMonth, step))

    /**
     * Returns the first [YearMonth] of the progression
     */
    public val first: YearMonth = YearMonth.fromProlepticMonth(longProgression.first)

    /**
     * Returns the last [YearMonth] of the progression
     */
    public val last: YearMonth = YearMonth.fromProlepticMonth(longProgression.last)

    /**
     * Returns an [Iterator] that traverses the progression from [first] to [last]
     */
    override fun iterator(): Iterator<YearMonth> = YearMonthProgressionIterator(longProgression.iterator())

    /**
     * Returns true iff the progression contains no values.
     * i.e. [first] < [last] if step is positive, or [first] > [last] if step is negative.
     */
    public override fun isEmpty(): Boolean = longProgression.isEmpty()

    /**
     * Returns a string representation of the progression.
     * Uses the range operator notation if the progression is increasing, and `downTo` if it is decreasing.
     * The step is referenced in days.
     */
    override fun toString(): String =
        if (longProgression.step > 0) "$first..$last step ${longProgression.step}M"
        else "$first downTo $last step ${longProgression.step}M"

    /**
     * Returns the number of months in the progression.
     * Returns [Int.MAX_VALUE] if the number of months overflows [Int]
     */
    override val size: Int
        get() = longProgression.sizeUnsafe

    /**
     * Returns true iff every element in [elements] is a member of the progression.
     */
    override fun containsAll(elements: Collection<YearMonth>): Boolean =
        (elements as Collection<*>).all { it is YearMonth && contains(it) }

    /**
     * Returns true iff [value] is a member of the progression.
     */
    override fun contains(value: YearMonth): Boolean {
        @Suppress("USELESS_CAST")
        if ((value as Any?) !is YearMonth) return false

        return longProgression.containsUnsafe(value.prolepticMonth)
    }

    override fun equals(other: Any?): Boolean =
        other is YearMonthProgression && longProgression == other.longProgression

    override fun hashCode(): Int = longProgression.hashCode()

    public companion object {
        internal fun fromClosedRange(
            rangeStart: YearMonth,
            rangeEnd: YearMonth,
            stepValue: Long,
            stepUnit: DateTimeUnit.MonthBased
        ): YearMonthProgression =
            YearMonthProgression(rangeStart, rangeEnd, safeMultiplyOrClamp(stepValue, stepUnit.months.toLong()))
    }
}

/**
 * A range of values of type [YearMonth].
 *
 * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.simpleRangeCreation
 */
public class YearMonthRange(start: YearMonth, endInclusive: YearMonth) : YearMonthProgression(start, endInclusive, 1),
    ClosedRange<YearMonth>, OpenEndRange<YearMonth> {
    /**
     * Returns the lower bound of the range, inclusive.
     */
    override val start: YearMonth get() = first

    /**
     * Returns the upper bound of the range, inclusive.
     */
    override val endInclusive: YearMonth get() = last

    /**
     * Returns the upper bound of the range, exclusive.
     */
    @Deprecated(
        "This throws an exception if the exclusive end if not inside " +
                "the platform-specific boundaries for YearMonth. " +
                "The 'endInclusive' property does not throw and should be preferred.",
        level = DeprecationLevel.WARNING
    )
    override val endExclusive: YearMonth
        get() {
            if (last == YearMonth.MAX)
                error("Cannot return the exclusive upper bound of a range that includes YearMonth.MAX.")
            return endInclusive.plus(1, DateTimeUnit.MONTH)
        }

    /**
     * Returns true iff [value] is contained within the range.
     * i.e. [value] is between [start] and [endInclusive].
     */
    @Suppress("ConvertTwoComparisonsToRangeCheck")
    override fun contains(value: YearMonth): Boolean {
        @Suppress("USELESS_CAST")
        if ((value as Any?) !is YearMonth) return false

        return first <= value && value <= last
    }

    /**
     * Returns true iff there are no months contained within the range.
     */
    override fun isEmpty(): Boolean = first > last

    /**
     * Returns a string representation of the range using the range operator notation.
     */
    override fun toString(): String = "$first..$last"

    public companion object {
        /** An empty range of values of type YearMonth. */
        public val EMPTY: YearMonthRange = YearMonthRange(YearMonth(0, 2), YearMonth(0, 1))

        internal fun fromRangeUntil(start: YearMonth, endExclusive: YearMonth): YearMonthRange {
            return if (endExclusive == YearMonth.MIN) EMPTY else fromRangeTo(
                start,
                endExclusive.minus(1, DateTimeUnit.MONTH)
            )
        }

        internal fun fromRangeTo(start: YearMonth, endInclusive: YearMonth): YearMonthRange {
            return YearMonthRange(start, endInclusive)
        }
    }
}

/**
 * Returns the first [YearMonth] of the [YearMonthProgression].
 *
 * @throws NoSuchElementException if the progression is empty.
 *
 * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.firstAndLast
 */
public fun YearMonthProgression.first(): YearMonth {
    if (isEmpty())
        throw NoSuchElementException("Progression $this is empty.")
    return this.first
}

/**
 * Returns the last [YearMonth] of the [YearMonthProgression].
 *
 * @throws NoSuchElementException if the progression is empty.
 *
 * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.firstAndLast
 */
public fun YearMonthProgression.last(): YearMonth {
    if (isEmpty())
        throw NoSuchElementException("Progression $this is empty.")
    return this.last
}

/**
 * Returns the first [YearMonth] of the [YearMonthProgression], or null if the progression is empty.
 *
 * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.firstAndLast
 */
public fun YearMonthProgression.firstOrNull(): YearMonth? = if (isEmpty()) null else this.first

/**
 * Returns the last [YearMonth] of the [YearMonthProgression], or null if the progression is empty.
 *
 * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.firstAndLast
 */
public fun YearMonthProgression.lastOrNull(): YearMonth? = if (isEmpty()) null else this.last

/**
 * Returns a reversed [YearMonthProgression], i.e. one that goes from [last] to [first].
 * The sign of the step is switched, in order to reverse the direction of the progression.
 *
 * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.reversedProgression
 */
public fun YearMonthProgression.reversed(): YearMonthProgression = YearMonthProgression(longProgression.reversed())

/**
 * Returns a [YearMonthProgression] with the same start and end, but a changed step value.
 *
 * **Pitfall**: the value parameter represents the magnitude of the step,
 * not the direction, and therefore must be positive.
 * Its sign will be matched to the sign of the existing step, in order to maintain the direction of the progression.
 * If you wish to switch the direction of the progression, use [YearMonthProgression.reversed]
 *
 * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.progressionWithStep
 */
public fun YearMonthProgression.step(value: Int, unit: DateTimeUnit.MonthBased): YearMonthProgression =
    step(value.toLong(), unit)

/**
 * Returns a [YearMonthProgression] with the same start and end, but a changed step value.
 *
 * **Pitfall**: the value parameter represents the magnitude of the step,
 * not the direction, and therefore must be positive.
 * Its sign will be matched to the sign of the existing step, in order to maintain the direction of the progression.
 * If you wish to switch the direction of the progression, use [YearMonthProgression.reversed]
 *
 * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.progressionWithStep
 */
public fun YearMonthProgression.step(value: Long, unit: DateTimeUnit.MonthBased): YearMonthProgression =
    YearMonthProgression(longProgression.step(safeMultiplyOrClamp(value, unit.months.toLong())))

/**
 * Creates a [YearMonthProgression] from `this` down to [that], inclusive.
 *
 * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.simpleRangeCreation
 */
public infix fun YearMonth.downTo(that: YearMonth): YearMonthProgression =
    YearMonthProgression.fromClosedRange(this, that, -1, DateTimeUnit.MONTH)

/**
 * Returns a random [YearMonth] within the bounds of the [YearMonthProgression].
 *
 * Takes the step into account;
 * will not return any value within the range that would be skipped over by the progression.
 *
 * @throws NoSuchElementException if the progression is empty.
 *
 * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.random
 */
public fun YearMonthProgression.random(random: Random = Random): YearMonth =
    if (isEmpty()) throw NoSuchElementException("Cannot get random in empty range: $this")
    else longProgression.randomUnsafe(random).let(YearMonth.Companion::fromProlepticMonth)

/**
 * Returns a random [YearMonth] within the bounds of the [YearMonthProgression] or null if the progression is empty.
 *
 * Takes the step into account;
 * will not return any value within the range that would be skipped over by the progression.
 *
 * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.random
 */
public fun YearMonthProgression.randomOrNull(random: Random = Random): YearMonth? = longProgression.randomUnsafeOrNull(random)
    ?.let(YearMonth.Companion::fromProlepticMonth)