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
     * A literal string.
     *
     * When formatting, the string is appended to the result as is,
     * and when parsing, the string is expected to be present in the input verbatim.
     */
    public fun chars(value: String)

    /**
     * Functions specific to the date-time format builders containing the local-date fields.
     */
    public sealed interface WithDate : DateTimeFormatBuilder {
        /**
         * A year number.
         *
         * By default, for years [-9999..9999], it's formatted as a decimal number, zero-padded to four digits, though
         * this padding can be disabled or changed to space padding by passing [padding].
         * For years outside this range, it's formatted as a decimal number with a leading sign, so the year 12345
         * is formatted as "+12345".
         */
        public fun year(padding: Padding = Padding.ZERO)

        /**
         * The last two digits of the ISO year.
         *
         * [baseYear] is the base year for the two-digit year.
         * For example, if [baseYear] is 1960, then this format correctly works with years [1960..2059].
         *
         * On formatting, when given a year in the valid range, it returns the last two digits of the year,
         * so 1993 becomes "93". When given a year outside the valid range, it returns the full year number
         * with a leading sign, so 1850 becomes "+1850", and -200 becomes "-200".
         *
         * On parsing, it accepts either a two-digit year or a full year number with a leading sign.
         * When given a two-digit year, it returns a year in the valid range, so "93" becomes 1993,
         * and when given a full year number with a leading sign, it parses the full year number,
         * so "+1850" becomes 1850.
         */
        public fun yearTwoDigits(baseYear: Int)

        /**
         * A month-of-year number, from 1 to 12.
         *
         * By default, it's padded with zeros to two digits. This can be changed by passing [padding].
         */
        public fun monthNumber(padding: Padding = Padding.ZERO)

        /**
         * A month name (for example, "January").
         *
         * Example:
         * ```
         * monthName(MonthNames.ENGLISH_FULL)
         * ```
         */
        public fun monthName(names: MonthNames)

        /**
         * A day-of-month number, from 1 to 31.
         *
         * By default, it's padded with zeros to two digits. This can be changed by passing [padding].
         */
        public fun dayOfMonth(padding: Padding = Padding.ZERO)

        /**
         * A day-of-week name (for example, "Thursday").
         *
         * Example:
         * ```
         * dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
         * ```
         */
        public fun dayOfWeek(names: DayOfWeekNames)

        /**
         * An existing [DateTimeFormat] for the date part.
         *
         * Example:
         * ```
         * date(LocalDate.Formats.ISO)
         * ```
         */
        public fun date(format: DateTimeFormat<LocalDate>)
    }
}

/**
 * A format along with other ways to parse the same portion of the value.
 *
 * When parsing, first, [primaryFormat] is used; if parsing the whole string fails using that, the formats
 * from [alternativeFormats] are tried in order.
 *
 * When formatting, the [primaryFormat] is used to format the value, and [alternativeFormats] are ignored.
 *
 * Example:
 * ```
 * alternativeParsing(
 *   { dayOfMonth(); char('-'); monthNumber() },
 *   { monthNumber(); char(' '); dayOfMonth() },
 * ) { monthNumber(); char('/'); dayOfMonth() }
 * ```
 *
 * This will always format a date as `MM/DD`, but will also accept `DD-MM` and `MM DD`.
 */
@Suppress("UNCHECKED_CAST")
public fun <T : DateTimeFormatBuilder> T.alternativeParsing(
    vararg alternativeFormats: T.() -> Unit,
    primaryFormat: T.() -> Unit
): Unit = when (this) {
    is AbstractDateTimeFormatBuilder<*, *> ->
        appendAlternativeParsingImpl(
            *alternativeFormats as Array<out AbstractDateTimeFormatBuilder<*, *>.() -> Unit>,
            mainFormat = primaryFormat as (AbstractDateTimeFormatBuilder<*, *>.() -> Unit)
        )

    else -> throw IllegalStateException("impossible")
}

/**
 * An optional section.
 *
 * When formatting, the section is formatted if the value of any field in the block is not equal to the default value.
 * Only [optional] calls where all the fields have default values are permitted.
 *
 * Example:
 * ```
 * offsetHours(); char(':'); offsetMinutesOfHour()
 * optional { char(':'); offsetSecondsOfMinute() }
 * ```
 *
 * Here, because seconds have the default value of zero, they are formatted only if they are not equal to zero, so the
 * UTC offset `+18:30:00` gets formatted as `"+18:30"`, but `+18:30:01` becomes `"+18:30:01"`.
 *
 * When parsing, either [format] or, if that fails, the literal [ifZero] are parsed. If the [ifZero] string is parsed,
 * the values in [format] get assigned their default values.
 *
 * [ifZero] defines the string that is used if values are the default ones.
 *
 * @throws IllegalArgumentException if not all fields used in [format] have a default value.
 */
@Suppress("UNCHECKED_CAST")
public fun <T : DateTimeFormatBuilder> T.optional(ifZero: String = "", format: T.() -> Unit): Unit = when (this) {
    is AbstractDateTimeFormatBuilder<*, *> -> appendOptionalImpl(
        onZero = ifZero,
        format as (AbstractDateTimeFormatBuilder<*, *>.() -> Unit)
    )

    else -> throw IllegalStateException("impossible")
}

/**
 * A literal character.
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

    fun build(): CachedFormatStructure<Target> = CachedFormatStructure(actualBuilder.build().formats)
}
