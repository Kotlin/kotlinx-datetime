/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.parser.*
import kotlinx.datetime.internal.safeMultiply
import kotlin.math.*

/**
 * A collection of date-time fields.
 *
 * Its main purpose is to provide support for complex date-time formats that don't correspond to any of the standard
 * entities in the library.
 *
 * Accessing the fields of this class is not thread-safe.
 * Make sure to apply proper synchronization if you are using a single instance from multiple threads.
 */
public class ValueBag internal constructor(internal val contents: ValueBagContents = ValueBagContents()) {
    public companion object;

    /**
     * Writes the contents of the specified [localTime] to this [ValueBag].
     * The [localTime] is written to the [hour], [minute], [second] and [nanosecond] fields.
     *
     * If any of the fields are already set, they will be overwritten.
     */
    public fun populateFrom(localTime: LocalTime) {
        hour = localTime.hour
        minute = localTime.minute
        second = localTime.second
        nanosecond = localTime.nanosecond
    }

    /**
     * Writes the contents of the specified [localDate] to this [ValueBag].
     * The [localDate] is written to the [year], [monthNumber] and [dayOfMonth] fields.
     *
     * If any of the fields are already set, they will be overwritten.
     */
    public fun populateFrom(localDate: LocalDate) {
        year = localDate.year
        monthNumber = localDate.monthNumber
        dayOfMonth = localDate.dayOfMonth
        dayOfWeek = localDate.dayOfWeek
    }

    /**
     * Writes the contents of the specified [localDateTime] to this [ValueBag].
     * The [localDateTime] is written to the
     * [year], [monthNumber], [dayOfMonth], [hour], [minute], [second] and [nanosecond] fields.
     *
     * If any of the fields are already set, they will be overwritten.
     */
    public fun populateFrom(localDateTime: LocalDateTime) {
        populateFrom(localDateTime.date)
        populateFrom(localDateTime.time)
    }

    /**
     * Writes the contents of the specified [utcOffset] to this [ValueBag].
     * The [utcOffset] is written to the [offsetTotalHours], [offsetMinutesOfHour] and [offsetSecondsOfMinute] fields.
     *
     * If any of the fields are already set, they will be overwritten.
     */
    public fun populateFrom(utcOffset: UtcOffset) {
        offsetIsNegative = utcOffset.totalSeconds < 0
        val seconds = utcOffset.totalSeconds.absoluteValue
        offsetTotalHours = seconds / 3600
        offsetMinutesOfHour = (seconds % 3600) / 60
        offsetSecondsOfMinute = seconds % 60
    }

    /**
     * Writes the contents of the specified [instant] to this [ValueBag].
     *
     * This method is almost always equivalent to the following code:
     * ```
     * populateFrom(instant.toLocalDateTime(offset))
     * populateFrom(offset)
     * ```
     * However, this also works for instants that are too large to be represented as a [LocalDateTime].
     *
     * If any of the fields are already set, they will be overwritten.
     */
    public fun populateFrom(instant: Instant, offset: UtcOffset) {
        val smallerInstant = Instant.fromEpochSeconds(
            instant.epochSeconds % SECONDS_PER_10000_YEARS, instant.nanosecondsOfSecond
        )
        populateFrom(smallerInstant.toLocalDateTime(offset))
        populateFrom(offset)
        year = year!! + ((instant.epochSeconds / SECONDS_PER_10000_YEARS) * 10000).toInt()
    }

    /** Returns the year component of the date. */
    public var year: Int? by contents.date::year

    /** Returns the number-of-month (1..12) component of the date. */
    public var monthNumber: Int? by contents.date::monthNumber

    /** Returns the month ([Month]) component of the date. */
    public var month: Month?
        get() = monthNumber?.let { Month(it) }
        set(value) {
            monthNumber = value?.number
        }

    /** Returns the day-of-month component of the date. */
    public var dayOfMonth: Int? by contents.date::dayOfMonth

    /** Returns the day-of-week component of the date. */
    public var dayOfWeek: DayOfWeek?
        get() = contents.date.isoDayOfWeek?.let { DayOfWeek(it) }
        set(value) {
            contents.date.isoDayOfWeek = value?.isoDayNumber
        }
    // /** Returns the day-of-year component of the date. */
    // public var dayOfYear: Int
    /** Returns the hour-of-day time component of this date/time value. */
    public var hour: Int? by contents.time::hour

    /** Returns the minute-of-hour time component of this date/time value. */
    public var minute: Int? by contents.time::minute

    /** Returns the second-of-minute time component of this date/time value. */
    public var second: Int? by contents.time::second

    /** Returns the nanosecond-of-second time component of this date/time value. */
    public var nanosecond: Int? by contents.time::nanosecond

    /** True if the offset is negative. */
    public var offsetIsNegative: Boolean? by contents.offset::isNegative

    /** The total amount of full hours in the UTC offset, in the range [0; 18]. */
    public var offsetTotalHours: Int? by contents.offset::totalHoursAbs

    /** The amount of minutes that don't add to a whole hour in the UTC offset, in the range [0; 59]. */
    public var offsetMinutesOfHour: Int? by contents.offset::minutesOfHour

    /** The amount of seconds that don't add to a whole minute in the UTC offset, in the range [0; 59]. */
    public var offsetSecondsOfMinute: Int? by contents.offset::secondsOfMinute

    public var timeZoneId: String? by contents::timeZoneId

    /**
     * Builds a [UtcOffset] from the fields in this [ValueBag].
     *
     * This method uses the following fields:
     * * [offsetTotalHours] (default value is 0)
     * * [offsetMinutesOfHour] (default value is 0)
     * * [offsetSecondsOfMinute] (default value is 0)
     *
     * Since all of these fields have default values, this method never fails.
     */
    public fun toUtcOffset(): UtcOffset = contents.offset.toUtcOffset()

    /**
     * Builds a [LocalDate] from the fields in this [ValueBag].
     *
     * This method uses the following fields:
     * * [year]
     * * [monthNumber]
     * * [dayOfMonth]
     *
     * Also, [dayOfWeek] is checked for consistency with the other fields.
     *
     * @throws IllegalArgumentException if any of the fields is missing or invalid.
     */
    public fun toLocaldate(): LocalDate = contents.date.toLocalDate()

    /**
     * Builds a [LocalTime] from the fields in this [ValueBag].
     *
     * This method uses the following fields:
     * * [hour]
     * * [minute]
     * * [second] (default value is 0)
     * * [nanosecond] (default value is 0)
     *
     * @throws IllegalArgumentException if hours or minutes are not present, or if any of the fields are invalid.
     */
    public fun toLocalTime(): LocalTime = contents.time.toLocalTime()

    /**
     * Builds a [LocalDateTime] from the fields in this [ValueBag].
     *
     * This method uses the following fields:
     * * [year]
     * * [monthNumber]
     * * [dayOfMonth]
     * * [hour]
     * * [minute]
     * * [second] (default value is 0)
     * * [nanosecond] (default value is 0)
     *
     * Also, [dayOfWeek] is checked for consistency with the other fields.
     *
     * @throws IllegalArgumentException if any of the required fields are not present,
     * or if any of the fields are invalid.
     *
     * @see toLocaldate
     * @see toLocalTime
     */
    public fun toLocalDateTime(): LocalDateTime = toLocaldate().atTime(toLocalTime())

    /**
     * Builds an [Instant] from the fields in this [ValueBag].
     *
     * Uses the fields required for [toLocalDateTime] and [toUtcOffset].
     *
     * Almost always equivalent to `toLocalDateTime().toInstant(toUtcOffset())`, but also accounts for cases when
     * the year is outside the range representable by [LocalDate] but not outside the range representable by [Instant].
     */
    public fun toInstantUsingUtcOffset(): Instant {
        val offset = toUtcOffset()
        val time = toLocalTime()
        val truncatedDate = contents.date.copy()
        /**
         * 10_000 is a number that is both
         * * guaranteed to be representable as the number of years in a [LocalDate],
         * * and is a multiple of 400, which is the number of years in a leap cycle, which means that taking the
         *   remainder of the year after dividing by 40_000 will not change the leap year status of the year.
         */
        truncatedDate.year = getParsedField(truncatedDate.year, "year") % 10_000
        val totalSeconds = try {
            val secDelta = safeMultiply((year!! / 10_000).toLong(), SECONDS_PER_10000_YEARS)
            val epochDays = truncatedDate.toLocalDate().toEpochDays().toLong()
            safeAdd(secDelta, epochDays * SECONDS_PER_DAY + time.toSecondOfDay() - offset.totalSeconds)
        } catch (e: ArithmeticException) {
            throw DateTimeFormatException("The parsed date is outside the range representable by Instant", e)
        }
        if (totalSeconds < Instant.MIN.epochSeconds || totalSeconds > Instant.MAX.epochSeconds)
            throw DateTimeFormatException("The parsed date is outside the range representable by Instant")
        return Instant.fromEpochSeconds(totalSeconds, nanosecond ?: 0)
    }

    override fun equals(other: Any?): Boolean = other is ValueBag && contents == other.contents

    override fun hashCode(): Int = contents.hashCode()
}

/**
 * Builder for [ValueBagFormat] values.
 */
@DateTimeBuilder
public interface ValueBagFormatBuilder : DateFormatBuilderFields, TimeFormatBuilderFields,
    UtcOffsetFormatBuilderFields, FormatBuilder<ValueBagFormatBuilder> {
    /**
     * Appends the IANA time zone identifier.
     */
    public fun appendTimeZoneId()

    /**
     * Appends a format string to the builder.
     *
     * For rules common for all format strings, see [FormatBuilder.appendFormatString].
     *
     * There are no special pattern letters for [ValueBagFormat], it can only embed nested formats using the
     * following names:
     * * `ld` for [LocalDate] format,
     * * `lt` for [LocalTime] format,
     * * `uo` for [UtcOffset] format.
     *
     * Example: `ld<mm'-'dd>', 'lt<hh':'mm>' ('uo<+(hhmm)>')'` can format a string like `12-25 03:04 (+0506)`.
     */
    // overriding the documentation.
    public override fun appendFormatString(formatString: String)
}

/**
 * A [Format] for [ValueBag] values.
 */
public class ValueBagFormat private constructor(private val actualFormat: Format<ValueBagContents>) :
    DateTimeFormat<ValueBag> by ValueBagFormatImpl(actualFormat) {
    public companion object {
        /**
         * Creates a [ValueBagFormat] using [ValueBagFormatBuilder].
         */
        public fun build(block: ValueBagFormatBuilder.() -> Unit): ValueBagFormat {
            val builder = Builder(AppendableFormatStructure(ValueBagFormatBuilderSpec))
            builder.block()
            return ValueBagFormat(builder.build())
        }

        /**
         * Creates a [ValueBagFormat] from a format string.
         *
         * Building a format is a relatively expensive operation, so it is recommended to store the result and avoid
         * rebuilding the format on every use.
         *
         * @see ValueBagFormatBuilder.appendFormatString for the format string syntax.
         */
        public fun fromFormatString(formatString: String): ValueBagFormat = build { appendFormatString(formatString) }

        /**
         * ISO-8601 extended format for dates and times with UTC offset.
         *
         * Examples of valid strings:
         * * `2020-01-01T23:59:59+01:00`
         * * `2020-01-01T23:59:59+01`
         * * `2020-01-01T23:59:59Z`
         *
         * This format uses the local date, local time, and UTC offset fields of [ValueBag].
         *
         * See ISO-8601-1:2019, 5.4.2.1b), excluding the format without the offset.
         */
        public val ISO_INSTANT: ValueBagFormat = build {
            appendYear(minDigits = 4, outputPlusOnExceededPadding = true)
            appendFormatString("ld<'-'mm'-'dd>('T'|'t')lt<hh':'mm':'ss(|'.'f)>uo<('Z'|'z')|+(HH(|':'mm(|':'ss)))>")
        }

        public val RFC_1123: ValueBagFormat = build {
            appendDayOfWeek(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
            appendLiteral(", ")
            appendDayOfMonth()
            appendLiteral(' ')
            appendMonthName(listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"))
            appendFormatString(" ld<yyyy> lt<hh':'mm':'ss> uo<'GMT'|+(HHmm)>")
        }

        internal val Cache = LruCache<String, ValueBagFormat>(16) { fromFormatString(it) }
    }

    private class Builder(override val actualBuilder: AppendableFormatStructure<ValueBagContents>) :
        AbstractFormatBuilder<ValueBagContents, ValueBagFormatBuilder, Builder>, ValueBagFormatBuilder {
        override fun appendYear(minDigits: Int, outputPlusOnExceededPadding: Boolean) =
            actualBuilder.add(BasicFormatStructure(YearDirective(minDigits, outputPlusOnExceededPadding)))

        override fun appendMonthNumber(minLength: Int) =
            actualBuilder.add(BasicFormatStructure(MonthDirective(minLength)))

        override fun appendMonthName(names: List<String>) =
            actualBuilder.add(BasicFormatStructure(MonthNameDirective(names)))

        override fun appendDayOfMonth(minLength: Int) = actualBuilder.add(BasicFormatStructure(DayDirective(minLength)))
        override fun appendDayOfWeek(names: List<String>) =
            actualBuilder.add(BasicFormatStructure(DayOfWeekDirective(names)))

        override fun appendHour(minLength: Int) = actualBuilder.add(BasicFormatStructure(HourDirective(minLength)))
        override fun appendAmPmHour(minLength: Int) =
            actualBuilder.add(BasicFormatStructure(AmPmHourDirective(minLength)))

        override fun appendAmPmMarker(amString: String, pmString: String) =
            actualBuilder.add(BasicFormatStructure(AmPmMarkerDirective(amString, pmString)))

        override fun appendMinute(minLength: Int) = actualBuilder.add(BasicFormatStructure(MinuteDirective(minLength)))
        override fun appendSecond(minLength: Int) = actualBuilder.add(BasicFormatStructure(SecondDirective(minLength)))
        override fun appendSecondFraction(minLength: Int, maxLength: Int?) =
            actualBuilder.add(BasicFormatStructure(FractionalSecondDirective(minLength, maxLength)))

        override fun appendOffsetTotalHours(minDigits: Int) =
            actualBuilder.add(BasicFormatStructure(UtcOffsetWholeHoursDirective(minDigits)))

        override fun appendOffsetMinutesOfHour(minDigits: Int) =
            actualBuilder.add(BasicFormatStructure(UtcOffsetMinuteOfHourDirective(minDigits)))

        override fun appendOffsetSecondsOfMinute(minDigits: Int) =
            actualBuilder.add(BasicFormatStructure(UtcOffsetSecondOfMinuteDirective(minDigits)))

        override fun appendTimeZoneId() =
            actualBuilder.add(BasicFormatStructure(TimeZoneIdDirective(TimeZone.availableZoneIds)))

        override fun appendFormatString(formatString: String) = super.appendFormatString(formatString)

        override fun createEmpty(): Builder = Builder(actualBuilder.createSibling())
        override fun castToGeneric(actualSelf: Builder): ValueBagFormatBuilder = this
    }

    override fun toString(): String = actualFormat.toString()

}

/**
 * Formats a [ValueBag] using the given format string.
 *
 * This method caches the format for the given string in a least-recently-used cache, so typically, only the first
 * invocation of this method for a given format string will require building the format.
 * However, if the program uses many format strings, this format may be evicted from the cache and require rebuilding.
 * In case more predictable performance is required, it is recommended to use the overload that accepts
 * a pre-built format. See [ValueBagFormat.fromFormatString] for more information.
 *
 * @see ValueBagFormatBuilder.appendFormatString for description of the format used.
 */
public fun ValueBag.format(formatString: String): String =
    ValueBagFormat.Cache.get(formatString).format(this)

public fun ValueBag.format(format: ValueBagFormat): String = format.format(this)

/**
 * Parses a [ValueBag] from [input] using the given format string.
 *
 * This method caches the format for the given string in a least-recently-used cache, so typically, only the first
 * invocation of this method for a given format string will require building the format.
 * However, if the program uses many format strings, this format may be evicted from the cache and require rebuilding.
 * In case more predictable performance is required, it is recommended to use the overload that accepts
 * a pre-built format. See [ValueBagFormat.fromFormatString] for more information.
 *
 * @see ValueBagFormatBuilder.appendFormatString for description of the format used.
 */
public fun ValueBag.Companion.parse(input: String, formatString: String): ValueBag =
    ValueBagFormat.Cache.get(formatString).parse(input)

/**
 * Parses a [ValueBag] from [input] using the given format.
 */
public fun ValueBag.Companion.parse(input: String, format: ValueBagFormat): ValueBag = try {
    format.parse(input)
} catch (e: ParseException) {
    throw DateTimeFormatException(e)
}

internal class ValueBagContents internal constructor(
    val date: IncompleteLocalDate = IncompleteLocalDate(),
    val time: IncompleteLocalTime = IncompleteLocalTime(),
    val offset: IncompleteUtcOffset = IncompleteUtcOffset(),
    var timeZoneId: String? = null,
) : DateFieldContainer by date, TimeFieldContainer by time, UtcOffsetFieldContainer by offset,
    Copyable<ValueBagContents> {
    override fun copy(): ValueBagContents = ValueBagContents(date.copy(), time.copy(), offset.copy(), timeZoneId)

    override fun equals(other: Any?): Boolean =
        other is ValueBagContents && other.date == date && other.time == time &&
            other.offset == offset && other.timeZoneId == timeZoneId

    override fun hashCode(): Int =
        date.hashCode() xor time.hashCode() xor offset.hashCode() xor (timeZoneId?.hashCode() ?: 0)
}

internal val timeZoneField = GenericFieldSpec(ValueBagContents::timeZoneId)

internal class TimeZoneIdDirective(knownZones: Set<String>) :
    StringFieldFormatDirective<ValueBagContents>(timeZoneField, knownZones) {

    override val formatStringRepresentation: Pair<String?, String>? = null

    override val builderRepresentation: String = "${ValueBagFormatBuilder::appendTimeZoneId.name}()"
}


internal object ValueBagFormatBuilderSpec : BuilderSpec<ValueBagContents>(
    mapOf(
        DateFormatBuilderSpec.name to DateFormatBuilderSpec,
        TimeFormatBuilderSpec.name to TimeFormatBuilderSpec,
        UtcOffsetFormatBuilderSpec.name to UtcOffsetFormatBuilderSpec,
    ),
    emptyMap()
)

private class ValueBagFormatImpl(actualFormat: Format<ValueBagContents>) :
    AbstractDateTimeFormat<ValueBag, ValueBagContents>(actualFormat) {
    override fun intermediateFromValue(value: ValueBag): ValueBagContents = value.contents

    override fun valueFromIntermediate(intermediate: ValueBagContents): ValueBag = ValueBag(intermediate)

    override fun newIntermediate(): ValueBagContents = ValueBagContents()
}
