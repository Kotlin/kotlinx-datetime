/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlin.native.concurrent.*

/**
 * Marks declarations in the datetime library that use format strings to define datetime formats.
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
 * Appends a Unicode date/time format string to the [DateTimeFormatBuilder].
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
 *   This is similar to what is supported by the Java Time's `DateTimeFormatter` class, but with the difference that,
 *   for formatting, the optional sections have a different meaning: they are not included in the output if all the
 *   fields they contain have their default values. See [DateTimeFormatBuilder.optional] for more details.
 * * One or more `p` characters before a field specifies that the field should be padded with zeroes to the length
 *   equal to the number of `p` characters. This is also supported by the Java Time's `DateTimeFormatter` class.
 *
 * @throws IllegalArgumentException if the pattern is invalid or contains unsupported directives.
 * @throws IllegalArgumentException if the builder is incompatible with the specified directives.
 * @throws UnsupportedOperationException if the kotlinx-datetime library does not support the specified directives.
 */
@FormatStringsInDatetimeFormats
public fun DateTimeFormatBuilder.byUnicodePattern(pattern: String) {
    val builder = this
    val directives = UnicodeFormat.parse(pattern)
    fun rec(format: UnicodeFormat) {
        when (format) {
            is UnicodeFormat.StringLiteral -> builder.chars(format.literal)
            is UnicodeFormat.Sequence -> format.formats.forEach { rec(it) }
            is UnicodeFormat.OptionalGroup -> builder.alternativeParsing({}) { rec(format.format) }
            is UnicodeFormat.Directive -> {
                when (format) {
                    is UnicodeFormat.Directive.TimeBased -> {
                        require(builder is DateTimeFormatBuilder.WithTime)
                        format.addToFormat(builder)
                    }

                    is UnicodeFormat.Directive.DateBased -> {
                        require(builder is DateTimeFormatBuilder.WithDate)
                        format.addToFormat(builder)
                    }

                    is UnicodeFormat.Directive.ZoneBased -> {
                        require(builder is DateTimeFormatBuilder.WithDateTimeComponents)
                        format.addToFormat(builder)
                    }

                    is UnicodeFormat.Directive.OffsetBased -> {
                        require(builder is DateTimeFormatBuilder.WithUtcOffset)
                        format.addToFormat(builder)
                    }

                    is UnknownUnicodeDirective -> {
                        throw IllegalArgumentException("The meaning of the directive '$format' is unknown")
                    }
                }
            }
        }
    }
    rec(directives)
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

    sealed interface Directive : UnicodeFormat {
        sealed interface DateBased : Directive {
            fun addToFormat(builder: DateTimeFormatBuilder.WithDate)

            class Era(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'G'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = localizedDirective()
            }

            class Year(length: Int) : AbstractUnicodeDirective(length), DateBased {
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

            class YearOfEra(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'y'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = localizedDirective(
                    "The locale-invariant ISO year directive '${"u".repeat(formatLength)}' can be used instead."
                )
            }

            class CyclicYearName(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'U'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = unsupportedDirective("cyclic-year")
            }

            // https://cldr.unicode.org/development/development-process/design-proposals/pattern-character-for-related-year
            class RelatedGregorianYear(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'r'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) =
                    unsupportedDirective("related-gregorian-year")
            }

            class DayOfYear(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'D'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = unsupportedDirective("day-of-year")
            }

            class MonthOfYear(length: Int) : AbstractUnicodeDirective(length), DateBased {
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

            class StandaloneMonthOfYear(length: Int) : AbstractUnicodeDirective(length), DateBased {
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

            class DayOfMonth(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'd'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = when (formatLength) {
                    1 -> builder.dayOfMonth(Padding.NONE)
                    2 -> builder.dayOfMonth(Padding.ZERO)
                    else -> unknownLength()
                }
            }

            class ModifiedJulianDay(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'g'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) =
                    unsupportedDirective("modified-julian-day")
            }

            class QuarterOfYear(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'Q'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) {
                    when (formatLength) {
                        1, 2 -> unsupportedDirective("quarter-of-year")
                        3, 4, 5 -> localizedDirective()
                        else -> unknownLength()
                    }
                }
            }

            class StandaloneQuarterOfYear(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'q'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) {
                    when (formatLength) {
                        1, 2 -> unsupportedDirective("standalone-quarter-of-year")
                        3, 4, 5 -> localizedDirective()
                        else -> unknownLength()
                    }
                }
            }

            class WeekBasedYear(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'Y'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) =
                    unsupportedDirective("week-based-year")
            }

            class WeekOfWeekBasedYear(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'w'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) =
                    unsupportedDirective("week-of-week-based-year")
            }

            class WeekOfMonth(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'W'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) =
                    unsupportedDirective("week-of-month")
            }

            class DayOfWeek(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'E'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = localizedDirective()
            }

            class LocalizedDayOfWeek(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'e'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = localizedDirective()
            }

            class StandaloneLocalizedDayOfWeek(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'c'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) = localizedDirective()
            }

            class DayOfWeekInMonth(length: Int) : AbstractUnicodeDirective(length), DateBased {
                override val formatLetter = 'F'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDate) =
                    unsupportedDirective("day-of-week-in-month")
            }

        }

        sealed interface TimeBased : Directive {
            fun addToFormat(builder: DateTimeFormatBuilder.WithTime)

            class AmPmMarker(length: Int) : AbstractUnicodeDirective(length), TimeBased {
                override val formatLetter = 'a'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) = localizedDirective()
            }

            class AmPmHourOfDay(length: Int) : AbstractUnicodeDirective(length), TimeBased {
                override val formatLetter = 'h'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) = localizedDirective()
            }

            class HourOfDay(length: Int) : AbstractUnicodeDirective(length), TimeBased {
                override val formatLetter = 'H'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) = when (formatLength) {
                    1 -> builder.hour(Padding.NONE)
                    2 -> builder.hour(Padding.ZERO)
                    else -> unknownLength()
                }
            }

            class MinuteOfHour(length: Int) : AbstractUnicodeDirective(length), TimeBased {
                override val formatLetter = 'm'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) = when (formatLength) {
                    1 -> builder.minute(Padding.NONE)
                    2 -> builder.minute(Padding.ZERO)
                    else -> unknownLength()
                }
            }


            sealed interface WithSecondPrecision : TimeBased {
                class SecondOfMinute(length: Int) : AbstractUnicodeDirective(length), WithSecondPrecision {
                    override val formatLetter = 's'
                    override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) = when (formatLength) {
                        1 -> builder.second(Padding.NONE)
                        2 -> builder.second(Padding.ZERO)
                        else -> unknownLength()
                    }
                }

            }

            sealed interface WithSubsecondPrecision : WithSecondPrecision {
                class FractionOfSecond(length: Int) : AbstractUnicodeDirective(length), WithSubsecondPrecision {
                    override val formatLetter = 'S'
                    override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) =
                        builder.secondFraction(minLength = formatLength, maxLength = formatLength)
                }

                class MilliOfDay(length: Int) : AbstractUnicodeDirective(length), WithSubsecondPrecision {
                    override val formatLetter = 'A'
                    override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) =
                        unsupportedDirective("millisecond-of-day")
                }

                class NanoOfSecond(length: Int) : AbstractUnicodeDirective(length), WithSubsecondPrecision {
                    override val formatLetter = 'n'
                    override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) =
                        unsupportedDirective("nano-of-second", "Maybe you meant 'S' instead of 'n'?")
                }

                class NanoOfDay(length: Int) : AbstractUnicodeDirective(length), WithSubsecondPrecision {
                    override val formatLetter = 'N'
                    override fun addToFormat(builder: DateTimeFormatBuilder.WithTime) =
                        unsupportedDirective("nanosecond-of-day")
                }
            }
        }

        sealed interface ZoneBased : Directive {
            fun addToFormat(builder: DateTimeFormatBuilder.WithDateTimeComponents)


            class TimeZoneId(length: Int) : AbstractUnicodeDirective(length), ZoneBased {
                override val formatLetter = 'V'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDateTimeComponents) = builder.timeZoneId()
            }

            class GenericTimeZoneName(length: Int) : AbstractUnicodeDirective(length), ZoneBased {
                override val formatLetter = 'v'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDateTimeComponents) = localizedDirective()
            }

            class TimeZoneName(length: Int) : AbstractUnicodeDirective(length), ZoneBased {
                override val formatLetter = 'z'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithDateTimeComponents) =
                    localizedDirective("Format 'V' can be used to format time zone IDs in a locale-invariant manner.")
            }
        }

        sealed interface OffsetBased : Directive {
            fun addToFormat(builder: DateTimeFormatBuilder.WithUtcOffset)

            fun outputMinutes(): WhenToOutput

            fun outputSeconds(): WhenToOutput

            fun DateTimeFormatBuilder.WithUtcOffset.offset(zOnZero: Boolean, useSeparator: Boolean) {
                isoOffset(zOnZero, useSeparator, outputMinutes(), outputSeconds())
            }

            class LocalizedZoneOffset(length: Int) : AbstractUnicodeDirective(length), OffsetBased {
                override val formatLetter = 'O'
                override fun addToFormat(builder: DateTimeFormatBuilder.WithUtcOffset) = localizedDirective()

                override fun outputMinutes(): WhenToOutput = localizedDirective()

                override fun outputSeconds(): WhenToOutput = localizedDirective()
            }

            class ZoneOffset1(length: Int) : AbstractUnicodeDirective(length), OffsetBased {
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

            class ZoneOffset2(length: Int) : AbstractUnicodeDirective(length), OffsetBased {
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

            class ZoneOffset3(length: Int) : AbstractUnicodeDirective(length), OffsetBased {
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

internal sealed class AbstractUnicodeDirective(val formatLength: Int) : UnicodeFormat.Directive {
    abstract val formatLetter: Char
    override fun toString(): String = "$formatLetter".repeat(formatLength)
    override fun equals(other: Any?): Boolean =
        other is AbstractUnicodeDirective && formatLetter == other.formatLetter && formatLength == other.formatLength

    override fun hashCode(): Int = formatLetter.hashCode() * 31 + formatLength
}

private class UnknownUnicodeDirective(override val formatLetter: Char, length: Int) : AbstractUnicodeDirective(length)

internal fun unicodeDirective(char: Char, length: Int): UnicodeFormat = when (char) {
    'G' -> UnicodeFormat.Directive.DateBased.Era(length)
    'y' -> UnicodeFormat.Directive.DateBased.YearOfEra(length)
    'Y' -> UnicodeFormat.Directive.DateBased.WeekBasedYear(length)
    'u' -> UnicodeFormat.Directive.DateBased.Year(length)
    'U' -> UnicodeFormat.Directive.DateBased.CyclicYearName(length)
    'r' -> UnicodeFormat.Directive.DateBased.RelatedGregorianYear(length)
    'Q' -> UnicodeFormat.Directive.DateBased.QuarterOfYear(length)
    'q' -> UnicodeFormat.Directive.DateBased.StandaloneQuarterOfYear(length)
    'M' -> UnicodeFormat.Directive.DateBased.MonthOfYear(length)
    'L' -> UnicodeFormat.Directive.DateBased.StandaloneMonthOfYear(length)
    'w' -> UnicodeFormat.Directive.DateBased.WeekOfWeekBasedYear(length)
    'W' -> UnicodeFormat.Directive.DateBased.WeekOfMonth(length)
    'd' -> UnicodeFormat.Directive.DateBased.DayOfMonth(length)
    'D' -> UnicodeFormat.Directive.DateBased.DayOfYear(length)
    'F' -> UnicodeFormat.Directive.DateBased.DayOfWeekInMonth(length)
    'g' -> UnicodeFormat.Directive.DateBased.ModifiedJulianDay(length)
    'E' -> UnicodeFormat.Directive.DateBased.DayOfWeek(length)
    'e' -> UnicodeFormat.Directive.DateBased.LocalizedDayOfWeek(length)
    'c' -> UnicodeFormat.Directive.DateBased.StandaloneLocalizedDayOfWeek(length)
    'a' -> UnicodeFormat.Directive.TimeBased.AmPmMarker(length)
    'h' -> UnicodeFormat.Directive.TimeBased.AmPmHourOfDay(length)
    'H' -> UnicodeFormat.Directive.TimeBased.HourOfDay(length)
    'm' -> UnicodeFormat.Directive.TimeBased.MinuteOfHour(length)
    's' -> UnicodeFormat.Directive.TimeBased.WithSecondPrecision.SecondOfMinute(length)
    'S' -> UnicodeFormat.Directive.TimeBased.WithSubsecondPrecision.FractionOfSecond(length)
    'A' -> UnicodeFormat.Directive.TimeBased.WithSubsecondPrecision.MilliOfDay(length)
    'n' -> UnicodeFormat.Directive.TimeBased.WithSubsecondPrecision.NanoOfSecond(length)
    'N' -> UnicodeFormat.Directive.TimeBased.WithSubsecondPrecision.NanoOfDay(length)
    'V' -> UnicodeFormat.Directive.ZoneBased.TimeZoneId(length)
    'v' -> UnicodeFormat.Directive.ZoneBased.GenericTimeZoneName(length)
    'z' -> UnicodeFormat.Directive.ZoneBased.TimeZoneName(length)
    'O' -> UnicodeFormat.Directive.OffsetBased.LocalizedZoneOffset(length)
    'X' -> UnicodeFormat.Directive.OffsetBased.ZoneOffset1(length)
    'x' -> UnicodeFormat.Directive.OffsetBased.ZoneOffset2(length)
    'Z' -> UnicodeFormat.Directive.OffsetBased.ZoneOffset3(length)
    else -> UnknownUnicodeDirective(char, length)
}

@SharedImmutable
private val nonPlainCharacters = ('a'..'z') + ('A'..'Z') + listOf('[', ']', '\'')

private fun unsupportedDirective(fieldName: String, recommendation: String? = null): Nothing =
    throw UnsupportedOperationException(
        "kotlinx.datetime formatting does not support the $fieldName field. " +
                (if (recommendation != null) "$recommendation " else "") +
                "Please report your use case to https://github.com/Kotlin/kotlinx-datetime/issues"
    )

private fun AbstractUnicodeDirective.unknownLength(): Nothing =
    throw IllegalArgumentException("Unknown length $formatLength for the $formatLetter directive")

private fun AbstractUnicodeDirective.localizedDirective(recommendation: String? = null): Nothing =
    throw IllegalArgumentException(
        "The directive '$this' is locale-dependent, but locales are not supported in Kotlin"
                + if (recommendation != null) ". $recommendation" else ""
    )

private fun AbstractUnicodeDirective.unsupportedPadding(digits: Int): Nothing =
    throw UnsupportedOperationException("Padding do $digits digits is not supported for the $formatLetter directive")
