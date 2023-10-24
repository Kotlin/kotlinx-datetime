/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*

/**
 * Common functions for all format builders.
 */
public sealed interface DateTimeFormatBuilder {
    /**
     * Appends a literal string to the format.
     * When formatting, the string is appended to the result as is,
     * and when parsing, the string is expected to be present in the input.
     */
    public fun chars(value: String)

    /**
     * Functions specific to the date-time format builders containing the local-date fields.
     */
    public sealed interface WithDate : DateTimeFormatBuilder {
        /**
         * Appends a year number to the format.
         *
         * By default, for years [-9999..9999], it's formatted as a decimal number, zero-padded to four digits, though
         * this padding can be disabled or changed to space padding by passing [padding].
         * For years outside this range, it's formatted as a decimal number with a leading sign, so the year 12345
         * is formatted as "+12345".
         */
        public fun appendYear(padding: Padding = Padding.ZERO)

        /**
         * Appends the last two digits of the ISO year.
         *
         * [base] is the base year for the two-digit year.
         * For example, if [base] is 1960, then this format correctly works with years [1960..2059].
         *
         * On formatting, when given a year in the valid range, it returns the last two digits of the year,
         * so 1993 becomes "93". When given a year outside the valid range, it returns the full year number
         * with a leading sign, so 1850 becomes "+1850", and -200 becomes "-200".
         *
         * On parsing, it accepts either a two-digit year or a full year number with a leading sign.
         * When given a two-digit year, it returns a year in the valid range, so "93" becomes 1993,
         * and when given a full year number with a leading sign, it returns the full year number,
         * so "+1850" becomes 1850.
         */
        public fun appendYearTwoDigits(base: Int)

        /**
         * Appends a month-of-year number to the format, from 1 to 12.
         */
        public fun appendMonthNumber(padding: Padding = Padding.ZERO)

        /**
         * Appends a month name to the format (for example, "January").
         *
         * Example:
         * ```
         * appendMonthName(MonthNames.ENGLISH_FULL)
         * ```
         */
        public fun appendMonthName(names: MonthNames)

        /**
         * Appends a day-of-month number to the format, from 1 to 31.
         *
         * By default, it's padded with zeros to two digits. This can be changed by passing [padding].
         */
        public fun appendDayOfMonth(padding: Padding = Padding.ZERO)

        /**
         * Appends a day-of-week name to the format (for example, "Thursday").
         *
         * Example:
         * ```
         * appendDayOfWeek(DayOfWeekNames.ENGLISH_FULL)
         * ```
         */
        public fun appendDayOfWeek(names: DayOfWeekNames)

        /**
         * Appends an existing [DateTimeFormat] for the date part.
         *
         * Example:
         * ```
         * appendDate(LocalDate.Format.ISO)
         * ```
         */
        public fun appendDate(dateFormat: DateTimeFormat<LocalDate>)
    }

    /**
     * Functions specific to the date-time format builders containing the local-time fields.
     */
    public sealed interface WithTime : DateTimeFormatBuilder {
        /**
         * Appends the number of hours.
         *
         * By default, it's zero-padded to two digits, but this can be changed with [padding].
         */
        public fun appendHour(padding: Padding = Padding.ZERO)

        /**
         * Appends the number of hours in the 12-hour clock.
         *
         * By default, it's zero-padded to two digits, but this can be changed with [padding].
         */
        public fun appendAmPmHour(padding: Padding = Padding.ZERO)

        /**
         * Appends the AM/PM marker, using the specified strings.
         *
         * [amString] is used for the AM marker (0-11 hours), [pmString] is used for the PM marker (12-23 hours).
         */
        public fun appendAmPmMarker(amString: String, pmString: String)

        /**
         * Appends the number of minutes.
         *
         * By default, it's zero-padded to two digits, but this can be changed with [padding].
         */
        public fun appendMinute(padding: Padding = Padding.ZERO)

        /**
         * Appends the number of seconds.
         *
         * By default, it's zero-padded to two digits, but this can be changed with [padding].
         *
         * This field has the default value of 0. If you want to omit it, use [optional].
         */
        public fun appendSecond(padding: Padding = Padding.ZERO)

        /**
         * Appends the fractional part of the second without the leading dot.
         *
         * When formatting, the decimal fraction will add trailing zeroes to the specified [minLength] and will round the
         * number to fit in the specified [maxLength]. If [minLength] is `null`, the fraction will be formatted with
         * enough trailing zeros to make the number of digits displayed a multiple of three (e.g. `123.450`) for
         * readability. Explicitly set [minLength] to `1` to disable this behavior.
         *
         * This field has the default value of 0. If you want to omit it, use [optional].
         *
         * @throws IllegalArgumentException if [minLength] is greater than [maxLength] or if either is not in the range 1..9.
         */
        public fun appendSecondFraction(minLength: Int? = null, maxLength: Int? = null)

        /**
         * Appends an existing [DateTimeFormat] for the time part.
         *
         * Example:
         * ```
         * appendTime(LocalTime.Format.ISO)
         * ```
         */
        public fun appendTime(format: DateTimeFormat<LocalTime>)
    }

    /**
     * Functions specific to the date-time format builders containing the local-date and local-time fields.
     */
    public sealed interface WithDateTime : WithDate, WithTime {
        /**
         * Appends an existing [DateTimeFormat] for the date-time part.
         *
         * Example:
         * ```
         * appendDateTime(LocalDateTime.Format.ISO)
         * ```
         */
        public fun appendDateTime(format: DateTimeFormat<LocalDateTime>)
    }

    /**
     * Functions specific to the date-time format builders containing the UTC-offset fields.
     */
    public sealed interface WithUtcOffset : DateTimeFormatBuilder {
        /**
         * Appends the total hours of the UTC offset, with a sign.
         *
         * By default, it's zero-padded to two digits, but this can be changed with [padding].
         *
         * This field has the default value of 0. If you want to omit it, use [optional].
         */
        public fun appendOffsetHours(padding: Padding = Padding.ZERO)

        /**
         * Appends the minute-of-hour of the UTC offset.
         *
         * By default, it's zero-padded to two digits, but this can be changed with [padding].
         *
         * This field has the default value of 0. If you want to omit it, use [optional].
         */
        public fun appendOffsetMinutesOfHour(padding: Padding = Padding.ZERO)

        /**
         * Appends the second-of-minute of the UTC offset.
         *
         * By default, it's zero-padded to two digits, but this can be changed with [padding].
         *
         * This field has the default value of 0. If you want to omit it, use [optional].
         */
        public fun appendOffsetSecondsOfMinute(padding: Padding = Padding.ZERO)

        /**
         * Appends an existing [DateTimeFormat] for the UTC offset part.
         *
         * Example:
         * ```
         * appendOffset(UtcOffset.Format.COMPACT)
         * ```
         */
        public fun appendOffset(format: DateTimeFormat<UtcOffset>)
    }

    /**
     * Builder for formats for values that have all the date-time components.
     */
    public sealed interface WithDateTimeComponents : WithDateTime, WithUtcOffset {
        /**
         * Appends the IANA time zone identifier, for example, "Europe/Berlin".
         *
         * When formatting, the timezone identifier is supplied as is, without any validation.
         * On parsing, [TimeZone.availableZoneIds] is used to validate the identifier.
         */
        public fun appendTimeZoneId()

        /**
         * Appends an existing [DateTimeFormat].
         *
         * Example:
         * ```
         * appendDateTimeComponents(DateTimeComponents.Format.RFC_1123)
         * ```
         */
        public fun appendDateTimeComponents(format: DateTimeFormat<DateTimeComponents>)
    }
}

/**
 * Appends a format along with other ways to parse the same portion of the value.
 *
 * When parsing, the first, [primaryFormat] is used to parse the value, and if parsing fails using that, the formats
 * from [alternativeFormats] are tried in order.
 *
 * When formatting, the [primaryFormat] is used to format the value.
 */
@Suppress("UNCHECKED_CAST")
public fun <T: DateTimeFormatBuilder> T.alternativeParsing(
    vararg alternativeFormats: T.() -> Unit,
    primaryFormat: T.() -> Unit
): Unit = when (this) {
    is AbstractDateTimeFormatBuilder<*, *> ->
        appendAlternativeParsingImpl(*alternativeFormats as Array<out AbstractDateTimeFormatBuilder<*, *>.() -> Unit>,
            mainFormat = primaryFormat as (AbstractDateTimeFormatBuilder<*, *>.() -> Unit))
    else -> throw IllegalStateException("impossible")
}

/**
 * Appends an optional section to the format.
 *
 * When parsing, the section is parsed if it is present in the input.
 *
 * When formatting, the section is formatted if the value of any field in the block is not equal to the default value.
 * Only [optional] calls where all the fields have default values are permitted when formatting.
 *
 * Example:
 * ```
 * appendHours()
 * char(':')
 * appendMinutes()
 * optional {
 *   char(':')
 *   appendSeconds()
 * }
 * ```
 *
 * Here, because seconds have the default value of zero, they are formatted only if they are not equal to zero.
 *
 * [ifZero] defines the string that is used if values are the default ones.
 */
@Suppress("UNCHECKED_CAST")
public fun <T: DateTimeFormatBuilder> T.optional(ifZero: String = "", format: T.() -> Unit): Unit = when (this) {
    is AbstractDateTimeFormatBuilder<*, *> -> appendOptionalImpl(onZero = ifZero, format as (AbstractDateTimeFormatBuilder<*, *>.() -> Unit))
    else -> throw IllegalStateException("impossible")
}

/**
 * Appends a literal character to the format.
 *
 * This is a shorthand for `chars(value.toString())`.
 */
public fun DateTimeFormatBuilder.char(value: Char): Unit = chars(value.toString())

internal interface AbstractDateTimeFormatBuilder<Target, ActualSelf> :
    DateTimeFormatBuilder where ActualSelf : AbstractDateTimeFormatBuilder<Target, ActualSelf> {

    val actualBuilder: AppendableFormatStructure<Target>
    fun createEmpty(): ActualSelf

    fun appendAlternativeParsingImpl(
        vararg otherFormats: ActualSelf.() -> Unit,
        mainFormat: ActualSelf.() -> Unit
    ) {
        val others = otherFormats.map { block ->
            createEmpty().also { block(it) }.actualBuilder.build()
        }
        val main = createEmpty().also { mainFormat(it) }.actualBuilder.build()
        actualBuilder.add(AlternativesParsingFormatStructure(main, others))
    }

    fun appendOptionalImpl(
        onZero: String,
        format: ActualSelf.() -> Unit
    ) {
        actualBuilder.add(OptionalFormatStructure(onZero, createEmpty().also { format(it) }.actualBuilder.build()))
    }

    override fun chars(value: String) = actualBuilder.add(ConstantFormatStructure(value))

    fun withSharedSignImpl(outputPlus: Boolean, block: ActualSelf.() -> Unit) {
        actualBuilder.add(
            SignedFormatStructure(
                createEmpty().also { block(it) }.actualBuilder.build(),
                outputPlus
            )
        )
    }

    fun build(): StringFormat<Target> = StringFormat(actualBuilder.build())
}

internal interface Copyable<Self> {
    fun copy(): Self
}

internal inline fun<T> StringFormat<T>.builderString(constants: List<Pair<String, StringFormat<*>>>): String =
    directives.builderString(constants)

private fun<T> FormatStructure<T>.builderString(constants: List<Pair<String, StringFormat<*>>>): String = when (this) {
    is BasicFormatStructure -> directive.builderRepresentation
    is ConstantFormatStructure -> if (string.length == 1) {
        "${DateTimeFormatBuilder::char.name}(${string[0].toKotlinCode()})"
    } else {
        "${DateTimeFormatBuilder::chars.name}(${string.toKotlinCode()})"
    }
    is SignedFormatStructure -> {
        if (format is BasicFormatStructure && format.directive is UtcOffsetWholeHoursDirective) {
            format.directive.builderRepresentation
        } else {
            buildString {
                if (withPlusSign) appendLine("withSharedSign(outputPlus = true) {")
                else appendLine("withSharedSign {")
                appendLine(format.builderString(constants).prependIndent(CODE_INDENT))
                append("}")
            }
        }
    }
    is OptionalFormatStructure -> buildString {
        if (onZero == "") {
            appendLine("${DateTimeFormatBuilder::optional.name} {")
        } else {
            appendLine("${DateTimeFormatBuilder::optional.name}(${onZero.toKotlinCode()}) {")
        }
        val subformat = format.builderString(constants)
        if (subformat.isNotEmpty()) {
            appendLine(subformat.prependIndent(CODE_INDENT))
        }
        append("}")
    }
    is AlternativesParsingFormatStructure -> buildString {
        append("${DateTimeFormatBuilder::alternativeParsing.name}(")
        for (alternative in formats) {
            appendLine("{")
            val subformat = alternative.builderString(constants)
            if (subformat.isNotEmpty()) {
                appendLine(subformat.prependIndent(CODE_INDENT))
            }
            append("}, ")
        }
        if (this[length - 2] == ',') {
            repeat(2) {
                deleteAt(length - 1)
            }
        }
        appendLine(") {")
        appendLine(mainFormat.builderString(constants).prependIndent(CODE_INDENT))
        append("}")
    }
    is ConcatenatedFormatStructure -> buildString {
        if (formats.isNotEmpty()) {
            var index = 0
            loop@while (index < formats.size) {
                searchConstant@for (constant in constants) {
                    val constantDirectives = constant.second.directives.formats
                    if (formats.size - index >= constantDirectives.size) {
                        for (i in constantDirectives.indices) {
                            if (formats[index + i] != constantDirectives[i]) {
                                continue@searchConstant
                            }
                        }
                        append(constant.first)
                        index += constantDirectives.size
                        continue@loop
                    }
                }
                if (index == formats.size - 1) {
                    append(formats.last().builderString(constants))
                } else {
                    appendLine(formats[index].builderString(constants))
                }
                ++index
            }
        }
    }
}

private const val CODE_INDENT = "    "
