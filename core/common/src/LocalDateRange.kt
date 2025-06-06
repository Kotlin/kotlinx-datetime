/*
 * Copyright 2019-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.*
import kotlin.random.Random

private class LocalDateProgressionIterator(private val iterator: LongIterator) : Iterator<LocalDate> {
    override fun hasNext(): Boolean = iterator.hasNext()
    override fun next(): LocalDate = LocalDate.fromEpochDays(iterator.next())
}

/**
 * A progression of values of type [LocalDate].
 *
 * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.progressionWithStep
 * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.reversedProgression
 * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.firstAndLast
 */
public open class LocalDateProgression
internal constructor(internal val longProgression: LongProgression) : Collection<LocalDate> {

    internal constructor(
        start: LocalDate,
        endInclusive: LocalDate,
        step: Long
    ) : this(LongProgression.fromClosedRange(start.toEpochDays(), endInclusive.toEpochDays(), step))

    /**
     * Returns the first [LocalDate] of the progression
     */
    public val first: LocalDate = LocalDate.fromEpochDays(longProgression.first)

    /**
     * Returns the last [LocalDate] of the progression
     */
    public val last: LocalDate = LocalDate.fromEpochDays(longProgression.last)

    /**
     * Returns an [Iterator] that traverses the progression from [first] to [last]
     */
    override fun iterator(): Iterator<LocalDate> = LocalDateProgressionIterator(longProgression.iterator())

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
        if (longProgression.step > 0) "$first..$last step ${longProgression.step}D"
        else "$first downTo $last step ${longProgression.step}D"

    /**
     * Returns the number of dates in the progression.
     * Returns [Int.MAX_VALUE] if the number of dates overflows [Int]
     */
    override val size: Int
        get() = longProgression.sizeUnsafe

    /**
     * Returns true iff every element in [elements] is a member of the progression.
     */
    override fun containsAll(elements: Collection<LocalDate>): Boolean =
        (elements as Collection<*>).all { it is LocalDate && contains(it) }

    /**
     * Returns true iff [value] is a member of the progression.
     */
    override fun contains(value: LocalDate): Boolean {
        @Suppress("USELESS_CAST")
        if ((value as Any?) !is LocalDate) return false

        return longProgression.containsUnsafe(value.toEpochDays())
    }

    override fun equals(other: Any?): Boolean =
        other is LocalDateProgression && longProgression == other.longProgression

    override fun hashCode(): Int = longProgression.hashCode()

    public companion object {
        internal fun fromClosedRange(
            rangeStart: LocalDate,
            rangeEnd: LocalDate,
            stepValue: Long,
            stepUnit: DateTimeUnit.DayBased
        ): LocalDateProgression =
            LocalDateProgression(rangeStart, rangeEnd, safeMultiplyOrClamp(stepValue, stepUnit.days.toLong()))
    }
}

/**
 * A range of values of type [LocalDate].
 *
 * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.simpleRangeCreation
 */
public class LocalDateRange(start: LocalDate, endInclusive: LocalDate) : LocalDateProgression(start, endInclusive, 1),
    ClosedRange<LocalDate>, OpenEndRange<LocalDate> {
    /**
     * Returns the lower bound of the range, inclusive.
     */
    override val start: LocalDate get() = first

    /**
     * Returns the upper bound of the range, inclusive.
     */
    override val endInclusive: LocalDate get() = last

    /**
     * Returns the upper bound of the range, exclusive.
     */
    @Deprecated(
        "This throws an exception if the exclusive end if not inside " +
                "the platform-specific boundaries for LocalDate. " +
                "The 'endInclusive' property does not throw and should be preferred.",
        level = DeprecationLevel.WARNING
    )
    override val endExclusive: LocalDate
        get() {
            if (last == LocalDate.MAX)
                error("Cannot return the exclusive upper bound of a range that includes LocalDate.MAX.")
            return endInclusive.plus(1, DateTimeUnit.DAY)
        }

    /**
     * Returns true iff [value] is contained within the range.
     * i.e. [value] is between [start] and [endInclusive].
     */
    @Suppress("ConvertTwoComparisonsToRangeCheck")
    override fun contains(value: LocalDate): Boolean {
        @Suppress("USELESS_CAST")
        if ((value as Any?) !is LocalDate) return false

        return first <= value && value <= last
    }

    /**
     * Returns true iff there are no dates contained within the range.
     */
    override fun isEmpty(): Boolean = first > last

    /**
     * Returns a string representation of the range using the range operator notation.
     */
    override fun toString(): String = "$first..$last"

    public companion object {
        /** An empty range of values of type LocalDate. */
        public val EMPTY: LocalDateRange = LocalDateRange(LocalDate(1970, 1, 2), LocalDate(1970, 1, 1))

        internal fun fromRangeUntil(start: LocalDate, endExclusive: LocalDate): LocalDateRange {
            return if (endExclusive == LocalDate.MIN) EMPTY else fromRangeTo(
                start,
                endExclusive.minus(1, DateTimeUnit.DAY)
            )
        }

        internal fun fromRangeTo(start: LocalDate, endInclusive: LocalDate): LocalDateRange {
            return LocalDateRange(start, endInclusive)
        }
    }
}

/**
 * Returns the first [LocalDate] of the [LocalDateProgression].
 *
 * @throws NoSuchElementException if the progression is empty.
 *
 * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.firstAndLast
 */
public fun LocalDateProgression.first(): LocalDate {
    if (isEmpty())
        throw NoSuchElementException("Progression $this is empty.")
    return this.first
}

/**
 * Returns the last [LocalDate] of the [LocalDateProgression].
 *
 * @throws NoSuchElementException if the progression is empty.
 *
 * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.firstAndLast
 */
public fun LocalDateProgression.last(): LocalDate {
    if (isEmpty())
        throw NoSuchElementException("Progression $this is empty.")
    return this.last
}

/**
 * Returns the first [LocalDate] of the [LocalDateProgression], or null if the progression is empty.
 *
 * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.firstAndLast
 */
public fun LocalDateProgression.firstOrNull(): LocalDate? = if (isEmpty()) null else this.first

/**
 * Returns the last [LocalDate] of the [LocalDateProgression], or null if the progression is empty.
 *
 * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.firstAndLast
 */
public fun LocalDateProgression.lastOrNull(): LocalDate? = if (isEmpty()) null else this.last

/**
 * Returns a reversed [LocalDateProgression], i.e. one that goes from [last] to [first].
 * The sign of the step is switched, in order to reverse the direction of the progression.
 *
 * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.reversedProgression
 */
public fun LocalDateProgression.reversed(): LocalDateProgression = LocalDateProgression(longProgression.reversed())

/**
 * Returns a [LocalDateProgression] with the same start and end, but a changed step value.
 *
 * **Pitfall**: the value parameter represents the magnitude of the step,
 * not the direction, and therefore must be positive.
 * Its sign will be matched to the sign of the existing step, in order to maintain the direction of the progression.
 * If you wish to switch the direction of the progression, use [LocalDateProgression.reversed]
 *
 * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.progressionWithStep
 */
public fun LocalDateProgression.step(value: Int, unit: DateTimeUnit.DayBased): LocalDateProgression =
    step(value.toLong(), unit)

/**
 * Returns a [LocalDateProgression] with the same start and end, but a changed step value.
 *
 * **Pitfall**: the value parameter represents the magnitude of the step,
 * not the direction, and therefore must be positive.
 * Its sign will be matched to the sign of the existing step, in order to maintain the direction of the progression.
 * If you wish to switch the direction of the progression, use [LocalDateProgression.reversed]
 *
 * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.progressionWithStep
 */
public fun LocalDateProgression.step(value: Long, unit: DateTimeUnit.DayBased): LocalDateProgression =
    LocalDateProgression(longProgression.step(safeMultiplyOrClamp(value, unit.days.toLong())))

/**
 * Creates a [LocalDateProgression] from `this` down to [that], inclusive.
 *
 * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.simpleRangeCreation
 */
public infix fun LocalDate.downTo(that: LocalDate): LocalDateProgression =
    LocalDateProgression.fromClosedRange(this, that, -1, DateTimeUnit.DAY)

/**
 * Returns a random [LocalDate] within the bounds of the [LocalDateProgression].
 *
 * Takes the step into account;
 * will not return any value within the range that would be skipped over by the progression.
 *
 * @throws NoSuchElementException if the progression is empty.
 *
 * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.random
 */
public fun LocalDateProgression.random(random: Random = Random): LocalDate =
    if (isEmpty()) throw NoSuchElementException("Cannot get random in empty range: $this")
    else longProgression.randomUnsafe(random).let(LocalDate.Companion::fromEpochDays)

/**
 * Returns a random [LocalDate] within the bounds of the [LocalDateProgression] or null if the progression is empty.
 *
 * Takes the step into account;
 * will not return any value within the range that would be skipped over by the progression.
 *
 * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.random
 */
public fun LocalDateProgression.randomOrNull(random: Random = Random): LocalDate? = longProgression.randomUnsafeOrNull(random)
    ?.let(LocalDate.Companion::fromEpochDays)