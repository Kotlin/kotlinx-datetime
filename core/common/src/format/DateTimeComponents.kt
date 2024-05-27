/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.parser.Copyable
import kotlinx.datetime.internal.safeMultiply
import kotlin.reflect.*

/**
 * A collection of date-time fields, used specifically for parsing and formatting.
 *
 * Its main purpose is to provide support for complex date-time formats that don't correspond to any of the standard
 * entities in the library. For example, a format that includes only the month and the day of the month but not the
 * year cannot be represented and parsed as a [LocalDate], but it is valid for a [DateTimeComponents].
 * See sample 1.
 *
 * Another purpose is to support parsing and formatting data with out-of-bounds values. For example, parsing
 * `23:59:60` as a [LocalTime] is not possible, but it is possible to parse it as a [DateTimeComponents], adjust the value by
 * setting [second] to `59`, and then convert it to a [LocalTime] via [toLocalTime].
 * See sample 2.
 *
 * Because this class has limited applications, constructing it directly is not possible.
 * For formatting, use the [format] overload that accepts a lambda with a [DateTimeComponents] receiver.
 * See sample 3.
 *
 * Accessing the fields of this class is not thread-safe.
 * Make sure to apply proper synchronization if you are using a single instance from multiple threads.
 *
 * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.parsingComplexInput
 * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.parsingInvalidInput
 * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.simpleFormatting
 */
public class DateTimeComponents internal constructor(internal val contents: DateTimeComponentsContents = DateTimeComponentsContents()) {
    public companion object {
        /**
         * Creates a [DateTimeFormat] for [DateTimeComponents] values using [DateTimeFormatBuilder.WithDateTimeComponents].
         *
         * There is a collection of predefined formats in [DateTimeComponents.Formats].
         *
         * @throws IllegalArgumentException if parsing using this format is ambiguous.
         * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.customFormat
         */
        @Suppress("FunctionName")
        public fun Format(block: DateTimeFormatBuilder.WithDateTimeComponents.() -> Unit): DateTimeFormat<DateTimeComponents> {
            val builder = DateTimeComponentsFormat.Builder(AppendableFormatStructure())
            block(builder)
            return DateTimeComponentsFormat(builder.build())
        }
    }

    /**
     * A collection of formats for parsing and formatting [DateTimeComponents] values.
     *
     * If predefined formats are not sufficient, use [DateTimeComponents.Format] to create a custom
     * [kotlinx.datetime.format.DateTimeFormat] for [DateTimeComponents] values.
     */
    public object Formats {

        /**
         * ISO 8601 extended format for dates and times with UTC offset.
         *
         * For specifying the time zone offset, the format uses the [UtcOffset.Formats.ISO] format, except that during
         * parsing, specifying the minutes of the offset is optional (so offsets like `+03` are also allowed).
         *
         * This format differs from [LocalTime.Formats.ISO] in its time part in that
         * specifying the seconds is *not* optional.
         *
         * Examples of instants in the ISO 8601 format:
         * - `2020-08-30T18:43:00Z`
         * - `2020-08-30T18:43:00.50Z`
         * - `2020-08-30T18:43:00.123456789Z`
         * - `2020-08-30T18:40:00+03:00`
         * - `2020-08-30T18:40:00+03:30:20`
         * * `2020-01-01T23:59:59.123456789+01`
         * * `+12020-01-31T23:59:59Z`
         *
         * This format uses the local date, local time, and UTC offset fields of [DateTimeComponents].
         *
         * See ISO-8601-1:2019, 5.4.2.1b), excluding the format without the offset.
         *
         * Guaranteed to parse all strings that [Instant.toString] produces.
         *
         * Typically, to use this format, you can simply call [Instant.toString] and [Instant.parse].
         * However, by accessing this format directly, you can obtain the UTC offset, which is not returned from [Instant.parse],
         * or specify the UTC offset to be formatted.
         *
         * ```
         * val components = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET.parse("2020-08-30T18:43:00.123456789+03:00")
         * val instant = components.toInstantUsingOffset() // 2020-08-30T15:43:00.123456789Z
         * val localDateTime = components.toLocalDateTime() // 2020-08-30T18:43:00.123456789
         * val offset = components.toUtcOffset() // UtcOffset(hours = 3)
         * ```
         *
         * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.Formats.iso
         */
        public val ISO_DATE_TIME_OFFSET: DateTimeFormat<DateTimeComponents> = Format {
            date(ISO_DATE)
            alternativeParsing({
                char('t')
            }) {
                char('T')
            }
            hour()
            char(':')
            minute()
            char(':')
            second()
            optional {
                char('.')
                secondFraction(1, 9)
            }
            alternativeParsing({
                offsetHours()
            }) {
                offset(UtcOffset.Formats.ISO)
            }
        }

        /**
         * RFC 1123 format for dates and times with UTC offset.
         *
         * Examples of valid strings:
         * * `Mon, 30 Jun 2008 11:05:30 GMT`
         * * `Mon, 30 Jun 2008 11:05:30 -0300`
         * * `30 Jun 2008 11:05:30 UT`
         *
         * North American and military time zone abbreviations are not supported.
         *
         * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.Formats.rfc1123parsing
         * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.Formats.rfc1123formatting
         */
        public val RFC_1123: DateTimeFormat<DateTimeComponents> = Format {
            alternativeParsing({
                // the day of week may be missing
            }) {
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                chars(", ")
            }
            day(Padding.NONE)
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            year()
            char(' ')
            hour()
            char(':')
            minute()
            optional {
                char(':')
                second()
            }
            chars(" ")
            alternativeParsing({
                chars("UT")
            }, {
                chars("Z")
            }) {
                optional("GMT") {
                    offset(UtcOffset.Formats.FOUR_DIGITS)
                }
            }
        }
    }

    /**
     * Writes the contents of the specified [localTime] to this [DateTimeComponents].
     * The [localTime] is written to the [hour], [hourOfAmPm], [amPm], [minute], [second] and [nanosecond] fields.
     *
     * If any of the fields are already set, they will be overwritten.
     *
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.time
     */
    public fun setTime(localTime: LocalTime) {
        contents.time.populateFrom(localTime)
    }

    /**
     * Writes the contents of the specified [localDate] to this [DateTimeComponents].
     * The [localDate] is written to the [year], [monthNumber], [day], and [dayOfWeek] fields.
     *
     * If any of the fields are already set, they will be overwritten.
     *
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.date
     */
    public fun setDate(localDate: LocalDate) {
        contents.date.populateFrom(localDate)
    }

    /**
     * Writes the contents of the specified [localDateTime] to this [DateTimeComponents].
     * The [localDateTime] is written to the
     * [year], [monthNumber], [day], [dayOfWeek],
     * [hour], [hourOfAmPm], [amPm], [minute], [second] and [nanosecond] fields.
     *
     * If any of the fields are already set, they will be overwritten.
     *
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.setDateTime
     */
    public fun setDateTime(localDateTime: LocalDateTime) {
        contents.date.populateFrom(localDateTime.date)
        contents.time.populateFrom(localDateTime.time)
    }

    /**
     * Writes the contents of the specified [utcOffset] to this [DateTimeComponents].
     * The [utcOffset] is written to the [offsetHours], [offsetMinutesOfHour], [offsetSecondsOfMinute], and
     * [offsetIsNegative] fields.
     *
     * If any of the fields are already set, they will be overwritten.
     *
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.offset
     */
    public fun setOffset(utcOffset: UtcOffset) {
        contents.offset.populateFrom(utcOffset)
    }

    /**
     * Writes the contents of the specified [instant] to this [DateTimeComponents].
     *
     * This method is almost always equivalent to the following code:
     * ```
     * setDateTime(instant.toLocalDateTime(offset))
     * setOffset(utcOffset)
     * ```
     * However, this also works for instants that are too large to be represented as a [LocalDateTime].
     *
     * If any of the fields are already set, they will be overwritten.
     *
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.setDateTimeOffsetInstant
     */
    public fun setDateTimeOffset(instant: Instant, utcOffset: UtcOffset) {
        val smallerInstant = Instant.fromEpochSeconds(
            instant.epochSeconds % SECONDS_PER_10000_YEARS, instant.nanosecondsOfSecond
        )
        setDateTime(smallerInstant.toLocalDateTime(utcOffset))
        setOffset(utcOffset)
        year = year!! + ((instant.epochSeconds / SECONDS_PER_10000_YEARS) * 10000).toInt()
    }

    /**
     * Writes the contents of the specified [localDateTime] and [utcOffset] to this [DateTimeComponents].
     *
     * A shortcut for calling [setDateTime] and [setOffset] separately.
     *
     * If [localDateTime] is obtained from an [Instant] using [LocalDateTime.toInstant], it is recommended to use
     * [setDateTimeOffset] that accepts an [Instant] directly.
     *
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.setDateTimeOffset
     */
    public fun setDateTimeOffset(localDateTime: LocalDateTime, utcOffset: UtcOffset) {
        setDateTime(localDateTime)
        setOffset(utcOffset)
    }

    /**
     * The year component of the date.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.date
     */
    public var year: Int? by contents.date::year

    /**
     * The number-of-month (1..12) component of the date.
     * @throws IllegalArgumentException during assignment if the value is outside the `0..99` range.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.date
     */
    public var monthNumber: Int? by TwoDigitNumber(contents.date::monthNumber)

    /**
     * The month ([Month]) component of the date.
     *
     * This is a view of [monthNumber].
     * Setting it will set [monthNumber], and getting it will return a [Month] instance if [monthNumber] is a valid
     * month.
     *
     * @throws IllegalArgumentException during getting if [monthNumber] is outside the `1..12` range.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.date
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.setMonth
     */
    public var month: Month?
        get() = monthNumber?.let { Month(it) }
        set(value) {
            monthNumber = value?.number
        }

    /**
     * The day-of-month component of the date.
     * @throws IllegalArgumentException during assignment if the value is outside the `0..99` range.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.date
     */
    public var day: Int? by TwoDigitNumber(contents.date::day)

    /** @suppress */
    @Deprecated("Use 'day' instead")
    public var dayOfMonth: Int? by TwoDigitNumber(contents.date::day)

    /**
     * The day-of-week component of the date.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.dayOfWeek
     */
    public var dayOfWeek: DayOfWeek?
        get() = contents.date.isoDayOfWeek?.let { DayOfWeek(it) }
        set(value) {
            contents.date.isoDayOfWeek = value?.isoDayNumber
        }
    // /** Returns the day-of-year component of the date. */
    // public var dayOfYear: Int

    /**
     * The hour-of-day (0..23) time component.
     * @throws IllegalArgumentException during assignment if the value is outside the `0..99` range.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.time
     */
    public var hour: Int? by TwoDigitNumber(contents.time::hour)

    /**
     * The 12-hour (1..12) time component.
     * @throws IllegalArgumentException during assignment if the value is outside the `0..99` range.
     * @see amPm
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.timeAmPm
     */
    public var hourOfAmPm: Int? by TwoDigitNumber(contents.time::hourOfAmPm)

    /**
     * The AM/PM state of the time component.
     * @see hourOfAmPm
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.timeAmPm
     */
    public var amPm: AmPmMarker? by contents.time::amPm

    /**
     * The minute-of-hour component.
     * @throws IllegalArgumentException during assignment if the value is outside the `0..99` range.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.time
     */
    public var minute: Int? by TwoDigitNumber(contents.time::minute)

    /**
     * The second-of-minute component.
     * @throws IllegalArgumentException during assignment if the value is outside the `0..99` range.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.time
     */
    public var second: Int? by TwoDigitNumber(contents.time::second)

    /**
     * The nanosecond-of-second component.
     * @throws IllegalArgumentException during assignment if the value is outside the `0..999_999_999` range.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.time
     */
    public var nanosecond: Int?
        get() = contents.time.nanosecond
        set(value) {
            require(value == null || value in 0..999_999_999) {
                "Nanosecond must be in the range [0, 999_999_999]."
            }
            contents.time.nanosecond = value
        }

    /**
     * True if the offset is negative.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.offset
     */
    public var offsetIsNegative: Boolean? by contents.offset::isNegative

    /**
     * The total amount of full hours in the UTC offset, in the range [0; 18].
     * @throws IllegalArgumentException during assignment if the value is outside the `0..99` range.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.offset
     */
    public var offsetHours: Int? by TwoDigitNumber(contents.offset::totalHoursAbs)

    /**
     * The amount of minutes that don't add to a whole hour in the UTC offset, in the range [0; 59].
     * @throws IllegalArgumentException during assignment if the value is outside the `0..99` range.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.offset
     */
    public var offsetMinutesOfHour: Int? by TwoDigitNumber(contents.offset::minutesOfHour)

    /**
     * The amount of seconds that don't add to a whole minute in the UTC offset, in the range [0; 59].
     * @throws IllegalArgumentException during assignment if the value is outside the `0..99` range.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.offset
     */
    public var offsetSecondsOfMinute: Int? by TwoDigitNumber(contents.offset::secondsOfMinute)

    /**
     * The timezone identifier, for example, "Europe/Berlin".
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.timeZoneId
     */
    public var timeZoneId: String? by contents::timeZoneId

    /**
     * Builds a [UtcOffset] from the fields in this [DateTimeComponents].
     *
     * This method uses the following fields:
     * * [offsetIsNegative] (default value is `false`)
     * * [offsetHours] (default value is 0)
     * * [offsetMinutesOfHour] (default value is 0)
     * * [offsetSecondsOfMinute] (default value is 0)
     *
     * @throws IllegalArgumentException if any of the fields has an out-of-range value.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.toUtcOffset
     */
    public fun toUtcOffset(): UtcOffset = contents.offset.toUtcOffset()

    /**
     * Builds a [LocalDate] from the fields in this [DateTimeComponents].
     *
     * This method uses the following fields:
     * * [year]
     * * [monthNumber]
     * * [day]
     *
     * Also, [dayOfWeek] is checked for consistency with the other fields.
     *
     * @throws IllegalArgumentException if any of the fields is missing or invalid.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.toLocalDate
     */
    public fun toLocalDate(): LocalDate = contents.date.toLocalDate()

    /**
     * Builds a [LocalTime] from the fields in this [DateTimeComponents].
     *
     * This method uses the following fields:
     * * [hour], [hourOfAmPm], and [amPm]
     * * [minute]
     * * [second] (default value is 0)
     * * [nanosecond] (default value is 0)
     *
     * @throws IllegalArgumentException if hours or minutes are not present, if any of the fields are invalid, or
     * [hourOfAmPm] and [amPm] are inconsistent with [hour].
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.toLocalTime
     */
    public fun toLocalTime(): LocalTime = contents.time.toLocalTime()

    /**
     * Builds a [LocalDateTime] from the fields in this [DateTimeComponents].
     *
     * This method uses the following fields:
     * * [year]
     * * [monthNumber]
     * * [day]
     * * [hour], [hourOfAmPm], and [amPm]
     * * [minute]
     * * [second] (default value is 0)
     * * [nanosecond] (default value is 0)
     *
     * Also, [dayOfWeek] is checked for consistency with the other fields.
     *
     * @throws IllegalArgumentException if any of the required fields are not present,
     * any of the fields are invalid, or there's inconsistency.
     *
     * @see toLocalDate
     * @see toLocalTime
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.toLocalDateTime
     */
    public fun toLocalDateTime(): LocalDateTime = toLocalDate().atTime(toLocalTime())

    /**
     * Builds an [Instant] from the fields in this [DateTimeComponents].
     *
     * Uses the fields required for [toLocalDateTime] and [toUtcOffset].
     *
     * Almost always equivalent to `toLocalDateTime().toInstant(toUtcOffset())`, but also accounts for cases when
     * the year is outside the range representable by [LocalDate] but not outside the range representable by [Instant].
     *
     * @throws IllegalArgumentException if any of the required fields are not present, out-of-range, or inconsistent
     * with one another.
     * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.toInstantUsingOffset
     */
    public fun toInstantUsingOffset(): Instant {
        val offset = toUtcOffset()
        val time = toLocalTime()
        val truncatedDate = contents.date.copy()
        /**
         * 10_000 is a number that is both
         * * guaranteed to be representable as the number of years in a [LocalDate],
         * * and is a multiple of 400, which is the number of years in a leap cycle, which means that taking the
         *   remainder of the year after dividing by 40_000 will not change the leap year status of the year.
         */
        truncatedDate.year = requireParsedField(truncatedDate.year, "year") % 10_000
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
}

/**
 * Uses this format to format an unstructured [DateTimeComponents].
 *
 * [block] is called on an initially-empty [DateTimeComponents] before formatting.
 *
 * Example:
 * ```
 * // Mon, 16 Mar 2020 23:59:59 +0300
 * DateTimeComponents.Formats.RFC_1123.format {
 *    setDateTime(LocalDateTime(2020, 3, 16, 23, 59, 59, 999_999_999))
 *    setOffset(UtcOffset(hours = 3))
 * }
 * ```
 *
 * @throws IllegalStateException if some values needed for the format are not present or can not be formatted:
 * for example, trying to format [DateTimeFormatBuilder.WithDate.monthName] using a [DateTimeComponents.monthNumber]
 * value of 20.
 * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.formatting
 */
public fun DateTimeFormat<DateTimeComponents>.format(block: DateTimeComponents.() -> Unit): String =
    format(DateTimeComponents().apply { block() })

/**
 * Parses a [DateTimeComponents] from [input] using the given format.
 * Equivalent to calling [DateTimeFormat.parse] on [format] with [input].
 *
 * [DateTimeComponents] does not perform any validation, so even invalid values may be parsed successfully if the string pattern
 * matches.
 *
 * @throws IllegalArgumentException if the text does not match the format.
 * @sample kotlinx.datetime.test.samples.format.DateTimeComponentsSamples.parsing
 */
public fun DateTimeComponents.Companion.parse(
    input: CharSequence,
    format: DateTimeFormat<DateTimeComponents>
): DateTimeComponents =
    format.parse(input)

internal class DateTimeComponentsContents internal constructor(
    val date: IncompleteLocalDate = IncompleteLocalDate(),
    val time: IncompleteLocalTime = IncompleteLocalTime(),
    val offset: IncompleteUtcOffset = IncompleteUtcOffset(),
    var timeZoneId: String? = null,
) : DateFieldContainer by date, TimeFieldContainer by time, UtcOffsetFieldContainer by offset,
    DateTimeFieldContainer, Copyable<DateTimeComponentsContents> {
    override fun copy(): DateTimeComponentsContents =
        DateTimeComponentsContents(date.copy(), time.copy(), offset.copy(), timeZoneId)

    override fun equals(other: Any?): Boolean =
        other is DateTimeComponentsContents && other.date == date && other.time == time &&
            other.offset == offset && other.timeZoneId == timeZoneId

    override fun hashCode(): Int =
        date.hashCode() xor time.hashCode() xor offset.hashCode() xor (timeZoneId?.hashCode() ?: 0)
}

internal val timeZoneField = GenericFieldSpec(PropertyAccessor(DateTimeComponentsContents::timeZoneId))

internal class TimeZoneIdDirective(private val knownZones: Set<String>) :
    StringFieldFormatDirective<DateTimeComponentsContents>(timeZoneField, knownZones) {

    override val builderRepresentation: String
        get() =
            "${DateTimeFormatBuilder.WithDateTimeComponents::timeZoneId.name}()"

    override fun equals(other: Any?): Boolean = other is TimeZoneIdDirective && other.knownZones == knownZones
    override fun hashCode(): Int = knownZones.hashCode()
}

internal class DateTimeComponentsFormat(override val actualFormat: CachedFormatStructure<DateTimeComponentsContents>) :
    AbstractDateTimeFormat<DateTimeComponents, DateTimeComponentsContents>() {
    override fun intermediateFromValue(value: DateTimeComponents): DateTimeComponentsContents = value.contents

    override fun valueFromIntermediate(intermediate: DateTimeComponentsContents): DateTimeComponents =
        DateTimeComponents(intermediate)

    override val emptyIntermediate get() = emptyDateTimeComponentsContents

    class Builder(override val actualBuilder: AppendableFormatStructure<DateTimeComponentsContents>) :
        AbstractDateTimeFormatBuilder<DateTimeComponentsContents, Builder>, AbstractWithDateTimeBuilder,
        AbstractWithOffsetBuilder, DateTimeFormatBuilder.WithDateTimeComponents {
        override fun addFormatStructureForDateTime(structure: FormatStructure<DateTimeFieldContainer>) {
            actualBuilder.add(structure)
        }

        override fun addFormatStructureForOffset(structure: FormatStructure<UtcOffsetFieldContainer>) {
            actualBuilder.add(structure)
        }

        override fun timeZoneId() =
            actualBuilder.add(BasicFormatStructure(TimeZoneIdDirective(TimeZone.availableZoneIds)))

        @Suppress("NO_ELSE_IN_WHEN")
        override fun dateTimeComponents(format: DateTimeFormat<DateTimeComponents>) = when (format) {
            is DateTimeComponentsFormat -> actualBuilder.add(format.actualFormat)
        }

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }
}

private class TwoDigitNumber(private val reference: KMutableProperty0<Int?>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = reference.getValue(thisRef, property)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int?) {
        require(value === null || value in 0..99) { "${property.name} must be a two-digit number, got '$value'" }
        reference.setValue(thisRef, property, value)
    }
}

private val emptyDateTimeComponentsContents = DateTimeComponentsContents()
