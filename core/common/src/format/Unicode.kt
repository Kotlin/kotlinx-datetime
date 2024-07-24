/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlin.native.concurrent.*

/**
 * Marks declarations in the datetime library that use format strings to define datetime formats.
 *
 * Format strings are discouraged, because they require gaining proficiency in another tiny language.
 * When possible, please use the builder-style Kotlin API instead.
 * If the format string is a constant, the corresponding builder-style Kotlin code can be obtained by calling
 * [DateTimeFormat.formatAsKotlinBuilderDsl] on the resulting format. For example:
 * ```
 * DateTimeFormat.formatAsKotlinBuilderDsl(LocalTime.Format { byUnicodePattern("HH:mm") })
 * ```
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Using format strings is discouraged." +
            " If the format string is a constant, the corresponding builder-style Kotlin code can be obtained by calling" +
            " `DateTimeFormat.formatAsKotlinBuilderDsl` on the resulting format."
)
public annotation class FormatStringsInDatetimeFormats

/**
 * Appends a Unicode datetime format string to the [DateTimeFormatBuilder].
 *
 * This is the format string syntax used by the Java Time's `DateTimeFormatter` class, Swift's and Objective-C's
 * `NSDateFormatter` class, and the ICU library.
 * The syntax is specified at
 * <https://unicode-org.github.io/icu/userguide/format_parse/datetime/#datetime-format-syntax>.
 *
 * Currently, locale-aware directives are not supported, due to no locale support in Kotlin.
 *
 * In addition to the standard syntax, this function also supports the following extensions:
 * * `[]` denote optional sections. For example, `hh:mm[:ss]` will allow parsing seconds optionally.
 *   This is similar to what is supported by the Java Time's `DateTimeFormatter` class.
 *
 * Usage example:
 * ```
 * DateTimeComponents.Format {
 *   // 2023-01-20T23:53:16.312+03:30[Asia/Tehran]
 *   byUnicodePattern("uuuu-MM-dd'T'HH:mm[:ss[.SSS]]xxxxx'['VV']'")
 * }
 * ```
 *
 * The list of supported directives is as follows:
 *
 * | **Directive**       | **Meaning**                                                                             |
 * | ------------------- | --------------------------------------------------------------------------------------- |
 * | `'string'`          | literal `string`, without quotes                                                        |
 * | `'''`               | literal char `'`                                                                        |
 * | `[fmt]`             | equivalent to `fmt` during formatting, but during parsing also accepts the empty string |
 * | `u`                 | ISO year without padding                                                                |
 * | `uu`                | last two digits of the ISO year, with the base year 2000                                |
 * | `uuuu`              | ISO year, zero-padded to four digits                                                    |
 * | `M`, `L`            | month number (1-12), without padding                                                    |
 * | `MM`, `LL`          | month number (01-12), zero-padded to two digits                                         |
 * | `d`                 | day-of-month (1-31), without padding                                                    |
 * | `H`                 | hour-of-day (0-23), without padding                                                     |
 * | `HH`                | hour-of-day (00-23), zero-padded to two digits                                          |
 * | `m`                 | minute-of-hour (0-59), without padding                                                  |
 * | `mm`                | minute-of-hour (00-59), zero-padded to two digits                                       |
 * | `s`                 | second-of-hour (0-59), without padding                                                  |
 * | `ss`                | second-of-hour (00-59), zero-padded to two digits                                       |
 * | `S`, `SS`, `SSS`... | fraction-of-second without a leading dot, with as many digits as the format length      |
 * | `VV`                | timezone name (for example, `Europe/Berlin`)                                            |
 *
 * The UTC offset is formatted using one of the following directives. In every one of these formats, hours, minutes,
 * and seconds are zero-padded to two digits. Also, hours are unconditionally present.
 * 
 * | **Directive**          | **Minutes** | **Seconds** | **Separator** | **Representation of zero** |
 * | ---------------------- | ----------- | ----------- | ------------- | -------------------------- |
 * | `X`                    | unless zero | never       | none          | `Z`                        |
 * | `XX`                   | always      | never       | none          | `Z`                        |
 * | `XXX`                  | always      | never       | colon         | `Z`                        |
 * | `XXXX`                 | always      | unless zero | none          | `Z`                        |
 * | `XXXXX`, `ZZZZZ`       | always      | unless zero | colon         | `Z`                        |
 * | `x`                    | unless zero | never       | none          | `+00`                      |
 * | `xx`, `Z`, `ZZ`, `ZZZ` | always      | never       | none          | `+0000`                    |
 * | `xxx`                  | always      | never       | colon         | `+00:00`                   |
 * | `xxxx`                 | always      | unless zero | none          | `+0000`                    |
 * | `xxxxx`                | always      | unless zero | colon         | `+00:00`                   |
 *
 * Additionally, because the `y` directive is very often used instead of `u`, they are taken to mean the same.
 * This may lead to unexpected results if the year is negative: `y` would always produce a positive number, whereas
 * `u` may sometimes produce a negative one. For example:
 * ```
 * LocalDate(-10, 1, 5).format { byUnicodeFormat("yyyy-MM-dd") } // -0010-01-05
 * LocalDate(-10, 1, 5).toJavaLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) // 0011-01-05
 * ```
 *
 * Note that, when the format includes the era directive, [byUnicodePattern] will fail with an exception, so almost all
 * of the intentional usages of `y` will correctly report an error instead of behaving slightly differently.
 *
 * @throws IllegalArgumentException if the pattern is invalid or contains unsupported directives.
 * @throws IllegalArgumentException if the builder is incompatible with the specified directives.
 * @throws UnsupportedOperationException if the kotlinx-datetime library does not support the specified directives.
 * @sample kotlinx.datetime.test.samples.format.UnicodeSamples.byUnicodePattern
 */
@FormatStringsInDatetimeFormats
public fun DateTimeFormatBuilder.byUnicodePattern(pattern: String) {
    val directives = UnicodeFormat.parse(pattern)
    fun rec(builder: DateTimeFormatBuilder, format: UnicodeFormat) {
        when (format) {
            is UnicodeFormat.StringLiteral -> builder.chars(format.literal)
            is UnicodeFormat.Sequence -> format.formats.forEach { rec(builder, it) }
            is UnicodeFormat.OptionalGroup -> builder.alternativeParsing({}) {
                rec(this, format.format)
            }
            is UnicodeFormat.Directive -> {
                when (format) {
                    is UnicodeFormat.Directive.TimeBased -> {
                        require(builder is DateTimeFormatBuilder.WithTime) {
                            "A time-based directive $format was used in a format builder that doesn't support time components"
                        }
                        format.addToFormat(builder)
                    }

                    is UnicodeFormat.Directive.DateBased -> {
                        require(builder is DateTimeFormatBuilder.WithDate) {
                            "A date-based directive $format was used in a format builder that doesn't support date components"
                        }
                        format.addToFormat(builder)
                    }

                    is UnicodeFormat.Directive.ZoneBased -> {
                        require(builder is DateTimeFormatBuilder.WithDateTimeComponents) {
                            "A time-zone-based directive $format was used in a format builder that doesn't support time-zone components"
                        }
                        format.addToFormat(builder)
                    }

                    is UnicodeFormat.Directive.OffsetBased -> {
                        require(builder is DateTimeFormatBuilder.WithUtcOffset) {
                            "A UTC-offset-based directive $format was used in a format builder that doesn't support UTC offset components"
                        }
                        format.addToFormat(builder)
                    }

                    is UnknownUnicodeDirective -> {
                        throw IllegalArgumentException("The meaning of the directive '$format' is unknown")
                    }
                }
            }
        }
    }
    rec(this, directives)
}

/*
The code that translates Unicode directives to the kotlinx-datetime format is based on these references:
* https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/time/format/DateTimeFormatterBuilder.html#appendPattern(java.lang.String)
* https://unicode-org.github.io/icu/userguide/format_parse/datetime/#datetime-format-syntax
 */
internal sealed interface UnicodeFormat {
    companion object {
        fun parse(pattern: String): UnicodeFormat {
            val groups: MutableList<MutableList<UnicodeFormat>?> = mutableListOf(mutableListOf())
            var insideLiteral = false
            var literal = ""
            var lastCharacter: Char? = null
            var lastCharacterCount = 0
            for (character in pattern) {
                if (character == lastCharacter) {
                    ++lastCharacterCount
                } else if (insideLiteral) {
                    if (character == '\'') {
                        groups.last()?.add(StringLiteral(literal.ifEmpty { "'" }))
                        insideLiteral = false
                        literal = ""
                    } else literal += character
                } else {
                    if (lastCharacterCount > 0) {
                        groups.last()?.add(unicodeDirective(lastCharacter!!, lastCharacterCount))
                        lastCharacter = null
                        lastCharacterCount = 0
                    }
                    if (character !in nonPlainCharacters) {
                        literal += character
                        continue
                    }
                    if (literal != "") {
                        groups.last()?.add(StringLiteral(literal))
                        literal = ""
                    }
                    when (character) {
                        '\'' -> {
                            insideLiteral = true
                            literal = ""
                        }

                        '[' -> {
                            groups.add(mutableListOf())
                        }

                        ']' -> {
                            val group =
                                groups.removeLast() ?: throw IllegalArgumentException("Unmatched closing bracket")
                            groups.last()?.add(OptionalGroup(Sequence(group)))
                        }

                        else -> {
                            lastCharacter = character
                            lastCharacterCount = 1
                        }
                    }
                }
            }
            if (lastCharacterCount > 0) {
                groups.last()?.add(unicodeDirective(lastCharacter!!, lastCharacterCount))
            }
            if (literal != "") {
                groups.last()?.add(StringLiteral(literal))
            }
            return Sequence(groups.removeLast() ?: throw IllegalArgumentException("Unmatched opening bracket"))
        }
    }

    data class OptionalGroup(val format: UnicodeFormat) : UnicodeFormat {
        override fun toString(): String = "[$format]"
    }

    data class Sequence(val formats: List<UnicodeFormat>) : UnicodeFormat {
        override fun toString(): String = formats.joinToString("")
    }

    data class StringLiteral(val literal: String) : UnicodeFormat {
        override fun toString(): String = if (literal == "'") "''" else
            if (literal.any { it.isLetter() }) "'$literal'"
            else if (literal.isEmpty()) ""
            else literal
    }

    sealed class Directive : UnicodeFormat {
        abstract val formatLength: Int
        abstract val formatLetter: Char
        override fun toString(): String = "$formatLetter".repeat(formatLength)
        override fun equals(other: Any?): Boolean =
            other is Directive && formatLetter == other.formatLetter && formatLength == other.formatLength

        override fun hashCode(): Int = formatLetter.hashCode() * 31 + formatLength

        sealed class DateBased : Directive() {
            abstract fun addToFormat(builder: DateTimeFormatBuilder.WithDate)

            class Era(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'G'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = localizedDirective()
            }

            class Year(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'u'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) {
                    when (formatLength) {
                        1 -> builder.year(padding = Padding.NONE)
                        2 -> builder.yearTwoDigits(baseYear = 2000)
                        3 -> unsupportedPadding(formatLength)
                        4 -> builder.year(padding = Padding.ZERO)
                        else -> unsupportedPadding(formatLength)
                    }
                }
            }

            class YearOfEra(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'y'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = when (formatLength) {
                    1 -> builder.yearOfEra(padding = Padding.NONE)
                    2 -> builder.yearOfEraTwoDigits(baseYear = 2000)
                    3 -> unsupportedPadding(formatLength)
                    4 -> builder.yearOfEra(padding = Padding.ZERO)
                    else -> unsupportedPadding(formatLength)
                }
            }

            class CyclicYearName(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'U'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = unsupportedDirective("cyclic-year")
            }

            // https://cldr.unicode.org/development/development-process/design-proposals/pattern-character-for-related-year
            class RelatedGregorianYear(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'r'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) =
                    unsupportedDirective("related-gregorian-year")
            }

            class DayOfYear(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'D'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = unsupportedDirective("day-of-year")
            }

            class MonthOfYear(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'M'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) {
                    when (formatLength) {
                        1 -> builder.monthNumber(Padding.NONE)
                        2 -> builder.monthNumber(Padding.ZERO)
                        3, 4, 5 -> localizedDirective()
                        else -> unknownLength()
                    }
                }
            }

            class StandaloneMonthOfYear(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'L'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) {
                    when (formatLength) {
                        1 -> builder.monthNumber(Padding.NONE)
                        2 -> builder.monthNumber(Padding.ZERO)
                        3, 4, 5 -> localizedDirective()
                        else -> unknownLength()
                    }
                }
            }

            class DayOfMonth(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'd'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = when (formatLength) {
                    1 -> builder.day(Padding.NONE)
                    2 -> builder.day(Padding.ZERO)
                    else -> unknownLength()
                }
            }

            class ModifiedJulianDay(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'g'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) =
                    unsupportedDirective("modified-julian-day")
            }

            class QuarterOfYear(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'Q'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) {
                    when (formatLength) {
                        1, 2 -> unsupportedDirective("quarter-of-year")
                        3, 4, 5 -> localizedDirective()
                        else -> unknownLength()
                    }
                }
            }

            class StandaloneQuarterOfYear(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'q'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) {
                    when (formatLength) {
                        1, 2 -> unsupportedDirective("standalone-quarter-of-year")
                        3, 4, 5 -> localizedDirective()
                        else -> unknownLength()
                    }
                }
            }

            class WeekBasedYear(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'Y'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) =
                    unsupportedDirective("week-based-year")
            }

            class WeekOfWeekBasedYear(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'w'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) =
                    unsupportedDirective("week-of-week-based-year")
            }

            class WeekOfMonth(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'W'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) =
                    unsupportedDirective("week-of-month")
            }

            class DayOfWeek(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'E'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = localizedDirective()
            }

            class LocalizedDayOfWeek(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'e'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = localizedDirective()
            }

            class StandaloneLocalizedDayOfWeek(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'c'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = localizedDirective()
            }

            class DayOfWeekInMonth(override val formatLength: Int) : DateBased() {
                override val formatLetter = 'F'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) =
                    unsupportedDirective("day-of-week-in-month")
            }

        }

        sealed class TimeBased : Directive() {
            abstract fun addToFormat(builder: DateTimeFormatBuilder.WithTime)

            class AmPmMarker(override val formatLength: Int) : TimeBased() {
                override val formatLetter = 'a'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) = localizedDirective()
            }

            class AmPmHourOfDay(override val formatLength: Int) : TimeBased() {
                override val formatLetter = 'h'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) = localizedDirective()
            }

            class HourOfDay(override val formatLength: Int) : TimeBased() {
                override val formatLetter = 'H'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) = when (formatLength) {
                    1 -> builder.hour(Padding.NONE)
                    2 -> builder.hour(Padding.ZERO)
                    else -> unknownLength()
                }
            }

            class MinuteOfHour(override val formatLength: Int) : TimeBased() {
                override val formatLetter = 'm'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) = when (formatLength) {
                    1 -> builder.minute(Padding.NONE)
                    2 -> builder.minute(Padding.ZERO)
                    else -> unknownLength()
                }
            }


            sealed class WithSecondPrecision : TimeBased() {
                class SecondOfMinute(override val formatLength: Int) : WithSecondPrecision() {
                    override val formatLetter = 's'
                    override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) = when (formatLength) {
                        1 -> builder.second(Padding.NONE)
                        2 -> builder.second(Padding.ZERO)
                        else -> unknownLength()
                    }
                }

            }

            sealed class WithSubsecondPrecision : WithSecondPrecision() {
                class FractionOfSecond(override val formatLength: Int) : WithSubsecondPrecision() {
                    override val formatLetter = 'S'
                    override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) =
                        builder.secondFraction(formatLength)
                }

                class MilliOfDay(override val formatLength: Int) : WithSubsecondPrecision() {
                    override val formatLetter = 'A'
                    override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) =
                        unsupportedDirective("millisecond-of-day")
                }

                class NanoOfSecond(override val formatLength: Int) : WithSubsecondPrecision() {
                    override val formatLetter = 'n'
                    override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) =
                        unsupportedDirective("nano-of-second", "Maybe you meant 'S' instead of 'n'?")
                }

                class NanoOfDay(override val formatLength: Int) : WithSubsecondPrecision() {
                    override val formatLetter = 'N'
                    override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) =
                        unsupportedDirective("nanosecond-of-day")
                }
            }
        }

        sealed class ZoneBased : Directive() {
            abstract fun addToFormat(builder: DateTimeFormatBuilder.WithDateTimeComponents)


            class TimeZoneId(override val formatLength: Int) : ZoneBased() {
                override val formatLetter = 'V'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDateTimeComponents) = when (formatLength) {
                    2 -> builder.timeZoneId()
                    else -> unknownLength()
                }
            }

            class GenericTimeZoneName(override val formatLength: Int) : ZoneBased() {
                override val formatLetter = 'v'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDateTimeComponents) = localizedDirective()
            }

            class TimeZoneName(override val formatLength: Int) : ZoneBased() {
                override val formatLetter = 'z'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDateTimeComponents) =
                    localizedDirective("Format 'V' can be used to format time zone IDs in a locale-invariant manner.")
            }
        }

        sealed class OffsetBased : Directive() {
            abstract fun addToFormat(builder: DateTimeFormatBuilder.WithUtcOffset)

            abstract fun outputMinutes(): WhenToOutput

            abstract fun outputSeconds(): WhenToOutput

            fun DateTimeFormatBuilder.WithUtcOffset.offset(zOnZero: Boolean, useSeparator: Boolean) {
                isoOffset(zOnZero, useSeparator, outputMinutes(), outputSeconds())
            }

            class LocalizedZoneOffset(override val formatLength: Int) : OffsetBased() {
                override val formatLetter = 'O'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithUtcOffset) = localizedDirective()

                override fun outputMinutes(): WhenToOutput = localizedDirective()

                override fun outputSeconds(): WhenToOutput = localizedDirective()
            }

            class ZoneOffset1(override val formatLength: Int) : OffsetBased() {
                override val formatLetter = 'X'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithUtcOffset) {
                    when (formatLength) {
                        1 -> builder.offset(zOnZero = true, useSeparator = false)
                        2 -> builder.offset(zOnZero = true, useSeparator = false)
                        3 -> builder.offset(zOnZero = true, useSeparator = true)
                        4 -> builder.offset(zOnZero = true, useSeparator = false)
                        5 -> builder.offset(zOnZero = true, useSeparator = true)
                        else -> unknownLength()
                    }
                }

                override fun outputMinutes(): WhenToOutput =
                    if (formatLength == 1) WhenToOutput.IF_NONZERO else WhenToOutput.ALWAYS

                override fun outputSeconds(): WhenToOutput =
                    if (formatLength <= 3) WhenToOutput.NEVER else WhenToOutput.IF_NONZERO
            }

            class ZoneOffset2(override val formatLength: Int) : OffsetBased() {
                override val formatLetter = 'x'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithUtcOffset) {
                    when (formatLength) {
                        1 -> builder.offset(zOnZero = false, useSeparator = false)
                        2 -> builder.offset(zOnZero = false, useSeparator = false)
                        3 -> builder.offset(zOnZero = false, useSeparator = true)
                        4 -> builder.offset(zOnZero = false, useSeparator = false)
                        5 -> builder.offset(zOnZero = false, useSeparator = true)
                        else -> unknownLength()
                    }
                }

                override fun outputMinutes(): WhenToOutput =
                    if (formatLength == 1) WhenToOutput.IF_NONZERO else WhenToOutput.ALWAYS

                override fun outputSeconds(): WhenToOutput =
                    if (formatLength <= 3) WhenToOutput.NEVER else WhenToOutput.IF_NONZERO
            }

            class ZoneOffset3(override val formatLength: Int) : OffsetBased() {
                override val formatLetter = 'Z'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithUtcOffset) {
                    when (formatLength) {
                        1, 2, 3 -> builder.offset(zOnZero = false, useSeparator = false)
                        4 -> LocalizedZoneOffset(4).addToFormat(builder)
                        5 -> builder.offset(zOnZero = false, useSeparator = true)
                        else -> unknownLength()
                    }
                }

                override fun outputMinutes(): WhenToOutput = WhenToOutput.ALWAYS

                override fun outputSeconds(): WhenToOutput =
                    if (formatLength <= 3) WhenToOutput.NEVER else WhenToOutput.IF_NONZERO
            }
        }
    }
}

private class UnknownUnicodeDirective(override val formatLetter: Char, override val formatLength: Int) : UnicodeFormat.Directive()

private fun unicodeDirective(char: Char, formatLength: Int): UnicodeFormat = when (char) {
    'G' -> UnicodeFormat.Directive.DateBased.Era(formatLength)
    'y' -> UnicodeFormat.Directive.DateBased.YearOfEra(formatLength)
    'Y' -> UnicodeFormat.Directive.DateBased.WeekBasedYear(formatLength)
    'u' -> UnicodeFormat.Directive.DateBased.Year(formatLength)
    'U' -> UnicodeFormat.Directive.DateBased.CyclicYearName(formatLength)
    'r' -> UnicodeFormat.Directive.DateBased.RelatedGregorianYear(formatLength)
    'Q' -> UnicodeFormat.Directive.DateBased.QuarterOfYear(formatLength)
    'q' -> UnicodeFormat.Directive.DateBased.StandaloneQuarterOfYear(formatLength)
    'M' -> UnicodeFormat.Directive.DateBased.MonthOfYear(formatLength)
    'L' -> UnicodeFormat.Directive.DateBased.StandaloneMonthOfYear(formatLength)
    'w' -> UnicodeFormat.Directive.DateBased.WeekOfWeekBasedYear(formatLength)
    'W' -> UnicodeFormat.Directive.DateBased.WeekOfMonth(formatLength)
    'd' -> UnicodeFormat.Directive.DateBased.DayOfMonth(formatLength)
    'D' -> UnicodeFormat.Directive.DateBased.DayOfYear(formatLength)
    'F' -> UnicodeFormat.Directive.DateBased.DayOfWeekInMonth(formatLength)
    'g' -> UnicodeFormat.Directive.DateBased.ModifiedJulianDay(formatLength)
    'E' -> UnicodeFormat.Directive.DateBased.DayOfWeek(formatLength)
    'e' -> UnicodeFormat.Directive.DateBased.LocalizedDayOfWeek(formatLength)
    'c' -> UnicodeFormat.Directive.DateBased.StandaloneLocalizedDayOfWeek(formatLength)
    'a' -> UnicodeFormat.Directive.TimeBased.AmPmMarker(formatLength)
    'h' -> UnicodeFormat.Directive.TimeBased.AmPmHourOfDay(formatLength)
    'H' -> UnicodeFormat.Directive.TimeBased.HourOfDay(formatLength)
    'm' -> UnicodeFormat.Directive.TimeBased.MinuteOfHour(formatLength)
    's' -> UnicodeFormat.Directive.TimeBased.WithSecondPrecision.SecondOfMinute(formatLength)
    'S' -> UnicodeFormat.Directive.TimeBased.WithSubsecondPrecision.FractionOfSecond(formatLength)
    'A' -> UnicodeFormat.Directive.TimeBased.WithSubsecondPrecision.MilliOfDay(formatLength)
    'n' -> UnicodeFormat.Directive.TimeBased.WithSubsecondPrecision.NanoOfSecond(formatLength)
    'N' -> UnicodeFormat.Directive.TimeBased.WithSubsecondPrecision.NanoOfDay(formatLength)
    'V' -> UnicodeFormat.Directive.ZoneBased.TimeZoneId(formatLength)
    'v' -> UnicodeFormat.Directive.ZoneBased.GenericTimeZoneName(formatLength)
    'z' -> UnicodeFormat.Directive.ZoneBased.TimeZoneName(formatLength)
    'O' -> UnicodeFormat.Directive.OffsetBased.LocalizedZoneOffset(formatLength)
    'X' -> UnicodeFormat.Directive.OffsetBased.ZoneOffset1(formatLength)
    'x' -> UnicodeFormat.Directive.OffsetBased.ZoneOffset2(formatLength)
    'Z' -> UnicodeFormat.Directive.OffsetBased.ZoneOffset3(formatLength)
    else -> UnknownUnicodeDirective(char, formatLength)
}

private val nonPlainCharacters = ('a'..'z') + ('A'..'Z') + listOf('[', ']', '\'')

private fun unsupportedDirective(fieldName: String, recommendation: String? = null): Nothing =
    throw UnsupportedOperationException(
        "kotlinx.datetime formatting does not support the $fieldName field. " +
                (if (recommendation != null) "$recommendation " else "") +
                "Please report your use case to https://github.com/Kotlin/kotlinx-datetime/issues"
    )

private fun UnicodeFormat.Directive.unknownLength(): Nothing =
    throw IllegalArgumentException("Unknown length $formatLength for the $formatLetter directive")

private fun UnicodeFormat.Directive.localizedDirective(recommendation: String? = null): Nothing =
    throw IllegalArgumentException(
        "The directive '$this' is locale-dependent, but locales are not supported in Kotlin"
                + if (recommendation != null) ". $recommendation" else ""
    )

private fun UnicodeFormat.Directive.unsupportedPadding(digits: Int): Nothing =
    throw UnsupportedOperationException("Padding do $digits digits is not supported for the $formatLetter directive")
