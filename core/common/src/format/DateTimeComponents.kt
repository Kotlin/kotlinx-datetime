/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.safeMultiply
import kotlin.native.concurrent.*
import kotlin.reflect.*

/**
 * A collection of date-time fields, used specifically for parsing and formatting.
 *
 * Its main purpose is to provide support for complex date-time formats that don't correspond to any of the standard
 * entities in the library. For example, a format that includes only the month and the day of the month, but not the
 * year, can not be represented and parsed as a [LocalDate], but it is valid for a [DateTimeComponents].
 *
 * Example:
 * ```
 * val input = "2020-03-16T23:59:59.999999999+03:00"
 * val bag = DateTimeComponents.Format.ISO_INSTANT.parse(input)
 * val localDateTime = bag.toLocalDateTime() // LocalDateTime(2020, 3, 16, 23, 59, 59, 999_999_999)
 * val instant = bag.toInstantUsingUtcOffset() // Instant.parse("2020-03-16T20:59:59.999999999Z")
 * val offset = bag.toUtcOffset() // UtcOffset(hours = 3)
 * ```
 *
 * Another purpose is to support parsing and formatting data with out-of-bounds values. For example, parsing
 * `23:59:60` as a [LocalTime] is not possible, but it is possible to parse it as a [DateTimeComponents], adjust the value by
 * setting [second] to `59`, and then convert it to a [LocalTime] via [toLocalTime].
 *
 * Example:
 * ```
 * val input = "23:59:60"
 * val extraDay: Boolean
 * val time = DateTimeComponents.Format {
 *   appendTime(LocalTime.Format.ISO)
 * }.parse(input).apply {
 *   if (hour == 23 && minute == 59 && second == 60) {
 *     hour = 0; minute = 0; second = 0; extraDay = true
 *   } else {
 *     extraDay = false
 *   }
 * }.toLocalTime()
 * ```
 *
 * Because this class has limited applications, constructing it directly is not possible.
 * For formatting, use the [format] overload that accepts a lambda with a [DateTimeComponents] receiver.
 *
 * Example:
 * ```
 * // Mon, 16 Mar 2020 23:59:59 +0300
 * DateTimeComponents.Format.RFC_1123.format {
 *    populateFrom(LocalDateTime(2020, 3, 16, 23, 59, 59, 999_999_999))
 *    populateFrom(UtcOffset(hours = 3))
 * }
 * ```
 *
 * Accessing the fields of this class is not thread-safe.
 * Make sure to apply proper synchronization if you are using a single instance from multiple threads.
 */
public class DateTimeComponents internal constructor(internal val contents: DateTimeComponentsContents = DateTimeComponentsContents()) {
    public companion object {
        /**
         * Creates a [DateTimeFormat] for [DateTimeComponents] values using [DateTimeFormatBuilder.WithDateTimeComponents].
         *
         * There is a collection of predefined formats in [DateTimeComponents.Formats].
         */
        @Suppress("FunctionName")
        public fun Format(block: DateTimeFormatBuilder.WithDateTimeComponents.() -> Unit): DateTimeFormat<DateTimeComponents> {
            val builder = DateTimeComponentsFormat.Builder(AppendableFormatStructure())
            block(builder)
            return DateTimeComponentsFormat(builder.build())
        }
    }

    /**
     * The entry point for parsing and formatting [DateTimeComponents] values.
     *
     * If predefined formats are not sufficient, use [DateTimeComponents.Format] to create a custom
     * [kotlinx.datetime.format.DateTimeFormat] for [DateTimeComponents] values.
     */
    public object Formats {

        /**
         * ISO 8601 extended format for dates and times with UTC offset.
         *
         * Examples of valid strings:
         * * `2020-01-01T23:59:59+01:00`
         * * `2020-01-01T23:59:59+01`
         * * `2020-01-01T23:59:59Z`
         *
         * This format uses the local date, local time, and UTC offset fields of [DateTimeComponents].
         *
         * See ISO-8601-1:2019, 5.4.2.1b), excluding the format without the offset.
         */
        public val ISO_DATE_TIME_OFFSET: DateTimeFormat<DateTimeComponents> = Format {
            appendDate(ISO_DATE)
            alternativeParsing({
                char('t')
            }) {
                char('T')
            }
            appendHour()
            char(':')
            appendMinute()
            char(':')
            appendSecond()
            optional {
                char('.')
                appendSecondFraction()
            }
            appendIsoOffset(
                zOnZero = true,
                useSeparator = true,
                outputMinute = WhenToOutput.IF_NONZERO,
                outputSecond = WhenToOutput.IF_NONZERO
            )
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
         */
        public val RFC_1123: DateTimeFormat<DateTimeComponents> = Format {
            alternativeParsing({
                // the day of week may be missing
            }) {
                appendDayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                chars(", ")
            }
            appendDayOfMonth(Padding.NONE)
            char(' ')
            appendMonthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            appendYear()
            char(' ')
            appendHour()
            char(':')
            appendMinute()
            optional {
                char(':')
                appendSecond()
            }
            chars(" ")
            alternativeParsing({
                chars("UT")
            }, {
                chars("Z")
            }) {
                optional("GMT") {
                    appendOffset(UtcOffset.Formats.FOUR_DIGITS)
                }
            }
        }
    }

    /**
     * Writes the contents of the specified [localTime] to this [DateTimeComponents].
     * The [localTime] is written to the [hour], [minute], [second] and [nanosecond] fields.
     *
     * If any of the fields are already set, they will be overwritten.
     */
    public fun populateFrom(localTime: LocalTime) { contents.time.populateFrom(localTime) }

    /**
     * Writes the contents of the specified [localDate] to this [DateTimeComponents].
     * The [localDate] is written to the [year], [monthNumber] and [dayOfMonth] fields.
     *
     * If any of the fields are already set, they will be overwritten.
     */
    public fun populateFrom(localDate: LocalDate) { contents.date.populateFrom(localDate) }

    /**
     * Writes the contents of the specified [localDateTime] to this [DateTimeComponents].
     * The [localDateTime] is written to the
     * [year], [monthNumber], [dayOfMonth], [hour], [minute], [second] and [nanosecond] fields.
     *
     * If any of the fields are already set, they will be overwritten.
     */
    public fun populateFrom(localDateTime: LocalDateTime) {
        contents.date.populateFrom(localDateTime.date)
        contents.time.populateFrom(localDateTime.time)
    }

    /**
     * Writes the contents of the specified [utcOffset] to this [DateTimeComponents].
     * The [utcOffset] is written to the [offsetTotalHours], [offsetMinutesOfHour] and [offsetSecondsOfMinute] fields.
     *
     * If any of the fields are already set, they will be overwritten.
     */
    public fun populateFrom(utcOffset: UtcOffset) { contents.offset.populateFrom(utcOffset) }

    /**
     * Writes the contents of the specified [instant] to this [DateTimeComponents].
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
    public var monthNumber: Int? by TwoDigitNumber(contents.date::monthNumber)

    /** Returns the month ([Month]) component of the date. */
    public var month: Month?
        get() = monthNumber?.let { Month(it) }
        set(value) {
            monthNumber = value?.number
        }

    /** Returns the day-of-month component of the date. */
    public var dayOfMonth: Int? by TwoDigitNumber(contents.date::dayOfMonth)

    /** Returns the day-of-week component of the date. */
    public var dayOfWeek: DayOfWeek?
        get() = contents.date.isoDayOfWeek?.let { DayOfWeek(it) }
        set(value) {
            contents.date.isoDayOfWeek = value?.isoDayNumber
        }
    // /** Returns the day-of-year component of the date. */
    // public var dayOfYear: Int

    /** Returns the hour-of-day time component of this date/time value. */
    public var hour: Int? by TwoDigitNumber(contents.time::hour)

    /** Returns the 12-hour time component of this date/time value. */
    public var hourOfAmPm: Int? by TwoDigitNumber(contents.time::hourOfAmPm)

    /** Returns the AM/PM state of the time component: `true` if PM, `false` if `AM`. */
    public var isPm: Boolean? by contents.time::isPm

    /** Returns the minute-of-hour time component of this date/time value. */
    public var minute: Int? by TwoDigitNumber(contents.time::minute)

    /** Returns the second-of-minute time component of this date/time value. */
    public var second: Int? by TwoDigitNumber(contents.time::second)

    /** Returns the nanosecond-of-second time component of this date/time value. */
    public var nanosecond: Int?
        get() = contents.time.nanosecond
        set(value) {
            require(value == null || value in 0..999_999_999) {
                "Nanosecond must be in the range [0, 999,999,999]."
            }
            contents.time.nanosecond = value
        }

    /** True if the offset is negative. */
    public var offsetIsNegative: Boolean? by contents.offset::isNegative

    /** The total amount of full hours in the UTC offset, in the range [0; 18]. */
    public var offsetTotalHours: Int? by TwoDigitNumber(contents.offset::totalHoursAbs)

    /** The amount of minutes that don't add to a whole hour in the UTC offset, in the range [0; 59]. */
    public var offsetMinutesOfHour: Int? by TwoDigitNumber(contents.offset::minutesOfHour)

    /** The amount of seconds that don't add to a whole minute in the UTC offset, in the range [0; 59]. */
    public var offsetSecondsOfMinute: Int? by TwoDigitNumber(contents.offset::secondsOfMinute)

    /** The timezone identifier, for example, "Europe/Berlin". */
    public var timeZoneId: String? by contents::timeZoneId

    /**
     * Builds a [UtcOffset] from the fields in this [DateTimeComponents].
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
     * Builds a [LocalDate] from the fields in this [DateTimeComponents].
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
    public fun toLocalDate(): LocalDate = contents.date.toLocalDate()

    /**
     * Builds a [LocalTime] from the fields in this [DateTimeComponents].
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
     * Builds a [LocalDateTime] from the fields in this [DateTimeComponents].
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
     * @see toLocalDate
     * @see toLocalTime
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
     * @throws IllegalArgumentException if any of the required fields are not present.
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
}

/**
 * Uses this format to format an unstructured [DateTimeComponents].
 *
 * [block] is called on an empty [DateTimeComponents] before formatting.
 *
 * Example:
 * ```
 * // Mon, 16 Mar 2020 23:59:59 +0300
 * DateTimeComponents.Format.RFC_1123.format {
 *    populateFrom(LocalDateTime(2020, 3, 16, 23, 59, 59, 999_999_999))
 *    populateFrom(UtcOffset(hours = 3))
 * }
 * ```
 */
public fun DateTimeFormat<DateTimeComponents>.format(block: DateTimeComponents.() -> Unit): String = format(DateTimeComponents().apply { block() })

/**
 * Parses a [DateTimeComponents] from [input] using the given format.
 * Equivalent to calling [DateTimeFormat.parse] on [format] with [input].
 *
 * [DateTimeComponents] does not perform any validation, so even invalid values may be parsed successfully if the string pattern
 * matches.
 *
 * @throws IllegalArgumentException if the text does not match the format.
 */
public fun DateTimeComponents.Companion.parse(input: CharSequence, format: DateTimeFormat<DateTimeComponents>): DateTimeComponents =
    format.parse(input)

internal class DateTimeComponentsContents internal constructor(
    val date: IncompleteLocalDate = IncompleteLocalDate(),
    val time: IncompleteLocalTime = IncompleteLocalTime(),
    val offset: IncompleteUtcOffset = IncompleteUtcOffset(),
    var timeZoneId: String? = null,
) : DateFieldContainer by date, TimeFieldContainer by time, UtcOffsetFieldContainer by offset,
    DateTimeFieldContainer, Copyable<DateTimeComponentsContents> {
    override fun copy(): DateTimeComponentsContents = DateTimeComponentsContents(date.copy(), time.copy(), offset.copy(), timeZoneId)

    override fun equals(other: Any?): Boolean =
        other is DateTimeComponentsContents && other.date == date && other.time == time &&
            other.offset == offset && other.timeZoneId == timeZoneId

    override fun hashCode(): Int =
        date.hashCode() xor time.hashCode() xor offset.hashCode() xor (timeZoneId?.hashCode() ?: 0)
}

@SharedImmutable
internal val timeZoneField = GenericFieldSpec(DateTimeComponentsContents::timeZoneId)

internal class TimeZoneIdDirective(private val knownZones: Set<String>) :
    StringFieldFormatDirective<DateTimeComponentsContents>(timeZoneField, knownZones) {

    override val builderRepresentation: String get() =
        "${DateTimeFormatBuilder.WithDateTimeComponents::appendTimeZoneId.name}()"

    override fun equals(other: Any?): Boolean = other is TimeZoneIdDirective && other.knownZones == knownZones
    override fun hashCode(): Int = knownZones.hashCode()
}

internal class DateTimeComponentsFormat(override val actualFormat: StringFormat<DateTimeComponentsContents>) :
    AbstractDateTimeFormat<DateTimeComponents, DateTimeComponentsContents>() {
    override fun intermediateFromValue(value: DateTimeComponents): DateTimeComponentsContents = value.contents

    override fun valueFromIntermediate(intermediate: DateTimeComponentsContents): DateTimeComponents = DateTimeComponents(intermediate)

    override fun newIntermediate(): DateTimeComponentsContents = DateTimeComponentsContents()

    class Builder(override val actualBuilder: AppendableFormatStructure<DateTimeComponentsContents>) :
        AbstractDateTimeFormatBuilder<DateTimeComponentsContents, Builder>, DateTimeFormatBuilder.WithDateTimeComponents {
        override fun appendYear(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(YearDirective(padding)))

        override fun appendYearTwoDigits(base: Int) =
            actualBuilder.add(BasicFormatStructure(ReducedYearDirective(base)))

        override fun appendMonthNumber(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(MonthDirective(padding)))

        override fun appendMonthName(names: MonthNames) =
            actualBuilder.add(BasicFormatStructure(MonthNameDirective(names)))

        override fun appendDayOfMonth(padding: Padding) = actualBuilder.add(BasicFormatStructure(DayDirective(padding)))
        override fun appendDayOfWeek(names: DayOfWeekNames) =
            actualBuilder.add(BasicFormatStructure(DayOfWeekDirective(names)))

        override fun appendHour(padding: Padding) = actualBuilder.add(BasicFormatStructure(HourDirective(padding)))
        override fun appendAmPmHour(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(AmPmHourDirective(padding)))

        override fun appendAmPmMarker(amString: String, pmString: String) =
            actualBuilder.add(BasicFormatStructure(AmPmMarkerDirective(amString, pmString)))

        override fun appendMinute(padding: Padding) = actualBuilder.add(BasicFormatStructure(MinuteDirective(padding)))
        override fun appendSecond(padding: Padding) = actualBuilder.add(BasicFormatStructure(SecondDirective(padding)))
        override fun appendSecondFraction(minLength: Int?, maxLength: Int?) =
            actualBuilder.add(BasicFormatStructure(FractionalSecondDirective(minLength, maxLength)))

        override fun appendOffsetTotalHours(padding: Padding) =
            actualBuilder.add(
                SignedFormatStructure(
                    BasicFormatStructure(UtcOffsetWholeHoursDirective(padding)),
                    withPlusSign = true
                )
            )

        override fun appendOffsetMinutesOfHour(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(UtcOffsetMinuteOfHourDirective(padding)))

        override fun appendOffsetSecondsOfMinute(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(UtcOffsetSecondOfMinuteDirective(padding)))

        override fun appendTimeZoneId() =
            actualBuilder.add(BasicFormatStructure(TimeZoneIdDirective(TimeZone.availableZoneIds)))

        @Suppress("NO_ELSE_IN_WHEN")
        override fun appendDate(dateFormat: DateTimeFormat<LocalDate>) = when (dateFormat) {
            is LocalDateFormat -> actualBuilder.add(dateFormat.actualFormat.directives)
        }

        @Suppress("NO_ELSE_IN_WHEN")
        override fun appendTime(format: DateTimeFormat<LocalTime>) = when (format) {
            is LocalTimeFormat -> actualBuilder.add(format.actualFormat.directives)
        }

        @Suppress("NO_ELSE_IN_WHEN")
        override fun appendOffset(format: DateTimeFormat<UtcOffset>) = when (format) {
            is UtcOffsetFormat -> actualBuilder.add(format.actualFormat.directives)
        }

        @Suppress("NO_ELSE_IN_WHEN")
        override fun appendDateTime(format: DateTimeFormat<LocalDateTime>) = when (format) {
            is LocalDateTimeFormat -> actualBuilder.add(format.actualFormat.directives)
        }

        @Suppress("NO_ELSE_IN_WHEN")
        override fun appendDateTimeComponents(format: DateTimeFormat<DateTimeComponents>) = when (format) {
            is DateTimeComponentsFormat -> actualBuilder.add(format.actualFormat.directives)
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
