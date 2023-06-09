/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format.migration

import kotlinx.datetime.format.*
import kotlin.native.concurrent.*

/**
 * Appends a Unicode date/time format string to the [FormatBuilder].
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
 *   fields they contain have their default values. See [FormatBuilder.appendAlternatives] for more details.
 * * One or more `p` characters before a field specifies that the field should be padded with zeroes to the length
 *   equal to the number of `p` characters. This is also supported by the Java Time's `DateTimeFormatter` class.
 *
 * @throws IllegalArgumentException if the pattern is invalid or contains unsupported directives.
 * @throws IllegalArgumentException if the builder is incompatible with the specified directives.
 * @throws UnsupportedOperationException if the kotlinx-datetime library does not support the specified directives.
 */
public fun FormatBuilder.appendUnicodeFormatString(pattern: String) {
    val builder = this
    val directives = parseUnicodeFormat(pattern)
    fun rec(format: UnicodeFormat) {
        when (format) {
            is UnicodeStringLiteral -> builder.appendLiteral(format.literal)
            is UnicodeFormatSequence -> format.formats.forEach { rec(it) }
            is UnicodeOptionalGroup -> builder.appendOptional { rec(format.format) }
            is UnicodeDirective -> {
                when (format) {
                    is TimeBasedUnicodeDirective -> {
                        require(builder is TimeFormatBuilderFields)
                        format.addToFormat(builder)
                    }

                    is DateBasedUnicodeDirective -> {
                        require(builder is DateFormatBuilder)
                        format.addToFormat(builder)
                    }

                    is ZoneBasedUnicodeDirective -> {
                        require(builder is ValueBagFormatBuilder)
                        format.addToFormat(builder)
                    }

                    is OffsetBasedUnicodeDirective -> {
                        require(builder is UtcOffsetFormatBuilderFields)
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


internal sealed interface UnicodeFormat

internal data class UnicodeOptionalGroup(val format: UnicodeFormat) : UnicodeFormat {
    override fun toString(): String = "[$format]"
}

internal sealed interface UnicodeDirective : UnicodeFormat
internal sealed class AbstractUnicodeDirective(val formatLength: Int) : UnicodeDirective {
    abstract val formatLetter: Char
    override fun toString(): String = "$formatLetter".repeat(formatLength)
    override fun equals(other: Any?): Boolean =
        other is AbstractUnicodeDirective && formatLetter == other.formatLetter && formatLength == other.formatLength

    override fun hashCode(): Int = formatLetter.hashCode() * 31 + formatLength
}

internal sealed interface DateBasedUnicodeDirective : UnicodeDirective {
    fun addToFormat(builder: DateFormatBuilder)
}

internal sealed interface TimeBasedUnicodeDirective : UnicodeDirective {
    fun addToFormat(builder: TimeFormatBuilderFields)
}

internal sealed interface ZoneBasedUnicodeDirective : UnicodeDirective {
    fun addToFormat(builder: ValueBagFormatBuilder)
}

internal sealed interface OffsetBasedUnicodeDirective : UnicodeDirective {
    fun addToFormat(builder: UtcOffsetFormatBuilderFields)
}

/*
The code that translates Unicode directives to the kotlinx-datetime format is based on these references:
* https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/time/format/DateTimeFormatterBuilder.html#appendPattern(java.lang.String)
* https://unicode-org.github.io/icu/userguide/format_parse/datetime/#datetime-format-syntax
 */

private class Era(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'G'
    override fun addToFormat(builder: DateFormatBuilder) = localizedDirective()
}

private class Year(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'u'
    override fun addToFormat(builder: DateFormatBuilder) {
        when (formatLength) {
            1, 3 -> builder.appendYear(minDigits = formatLength)
            2 -> TODO() // builder.appendReducedYear(digits = 2, baseValue = 2000)
            else -> builder.appendYear(minDigits = formatLength, outputPlusOnExceededPadding = true)
        }
    }
}

private class YearOfEra(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'y'
    override fun addToFormat(builder: DateFormatBuilder) = localizedDirective(
        "The locale-invariant ISO year directive '${"u".repeat(formatLength)}' can be used instead."
    )
}

private class CyclicYearName(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'U'
    override fun addToFormat(builder: DateFormatBuilder) = unsupportedDirective("cyclic-year")
}

// https://cldr.unicode.org/development/development-process/design-proposals/pattern-character-for-related-year
private class RelatedGregorianYear(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'r'
    override fun addToFormat(builder: DateFormatBuilder) = unsupportedDirective("related-gregorian-year")
}

private class DayOfYear(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'D'
    override fun addToFormat(builder: DateFormatBuilder) = unsupportedDirective("day-of-year")
}

private class MonthOfYear(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'M'
    override fun addToFormat(builder: DateFormatBuilder) {
        when (formatLength) {
            1, 2 -> builder.appendMonthNumber(minLength = formatLength)
            3, 4, 5 -> localizedDirective()
            else -> unknownLength()
        }
    }
}

private class StandaloneMonthOfYear(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'L'
    override fun addToFormat(builder: DateFormatBuilder) {
        when (formatLength) {
            1, 2 -> builder.appendMonthNumber(minLength = formatLength)
            3, 4, 5 -> localizedDirective()
            else -> unknownLength()
        }
    }
}

private class DayOfMonth(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'd'
    override fun addToFormat(builder: DateFormatBuilder) = builder.appendDayOfMonth(minLength = formatLength)
}

private class ModifiedJulianDay(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'g'
    override fun addToFormat(builder: DateFormatBuilder) = unsupportedDirective("modified-julian-day")
}

private class QuarterOfYear(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'Q'
    override fun addToFormat(builder: DateFormatBuilder) {
        when (formatLength) {
            1, 2 -> unsupportedDirective("quarter-of-year")
            3, 4, 5 -> localizedDirective()
            else -> unknownLength()
        }
    }
}

private class StandaloneQuarterOfYear(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'q'
    override fun addToFormat(builder: DateFormatBuilder) {
        when (formatLength) {
            1, 2 -> unsupportedDirective("standalone-quarter-of-year")
            3, 4, 5 -> localizedDirective()
            else -> unknownLength()
        }
    }
}

private class WeekBasedYear(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'Y'
    override fun addToFormat(builder: DateFormatBuilder) = unsupportedDirective("week-based-year")
}

private class WeekOfWeekBasedYear(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'w'
    override fun addToFormat(builder: DateFormatBuilder) = unsupportedDirective("week-of-week-based-year")
}

private class WeekOfMonth(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'W'
    override fun addToFormat(builder: DateFormatBuilder) = unsupportedDirective("week-of-month")
}

private class DayOfWeekDirective(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'E'
    override fun addToFormat(builder: DateFormatBuilder) = localizedDirective()
}

private class LocalizedDayOfWeek(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'e'
    override fun addToFormat(builder: DateFormatBuilder) = localizedDirective()
}

private class StandaloneLocalizedDayOfWeek(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'c'
    override fun addToFormat(builder: DateFormatBuilder) = localizedDirective()
}

private class DayOfWeekInMonth(length: Int) : AbstractUnicodeDirective(length), DateBasedUnicodeDirective {
    override val formatLetter = 'F'
    override fun addToFormat(builder: DateFormatBuilder) = unsupportedDirective("day-of-week-in-month")
}

private class AmPmMarker(length: Int) : AbstractUnicodeDirective(length), TimeBasedUnicodeDirective {
    override val formatLetter = 'a'
    override fun addToFormat(builder: TimeFormatBuilderFields) = localizedDirective()
}

private class HourOfDay(length: Int) : AbstractUnicodeDirective(length), TimeBasedUnicodeDirective {
    override val formatLetter = 'H'
    override fun addToFormat(builder: TimeFormatBuilderFields) = builder.appendHour(minLength = formatLength)
}

private class MinuteOfHour(length: Int) : AbstractUnicodeDirective(length), TimeBasedUnicodeDirective {
    override val formatLetter = 'm'
    override fun addToFormat(builder: TimeFormatBuilderFields) = builder.appendMinute(minLength = formatLength)
}

private class SecondOfMinute(length: Int) : AbstractUnicodeDirective(length), TimeBasedUnicodeDirective {
    override val formatLetter = 's'
    override fun addToFormat(builder: TimeFormatBuilderFields) = builder.appendSecond(minLength = formatLength)
}

private class FractionOfSecond(length: Int) : AbstractUnicodeDirective(length), TimeBasedUnicodeDirective {
    override val formatLetter = 'S'
    override fun addToFormat(builder: TimeFormatBuilderFields) =
        builder.appendSecondFraction(minLength = formatLength, maxLength = formatLength)
}

private class MilliOfDay(length: Int) : AbstractUnicodeDirective(length), TimeBasedUnicodeDirective {
    override val formatLetter = 'A'
    override fun addToFormat(builder: TimeFormatBuilderFields) = unsupportedDirective("millisecond-of-day")
}

private class NanoOfSecond(length: Int) : AbstractUnicodeDirective(length), TimeBasedUnicodeDirective {
    override val formatLetter = 'n'
    override fun addToFormat(builder: TimeFormatBuilderFields) =
        unsupportedDirective("nano-of-second", "Maybe you meant 'S' instead of 'n'?")
}

private class NanoOfDay(length: Int) : AbstractUnicodeDirective(length), TimeBasedUnicodeDirective {
    override val formatLetter = 'N'
    override fun addToFormat(builder: TimeFormatBuilderFields) = unsupportedDirective("nanosecond-of-day")
}

private class TimeZoneId(length: Int) : AbstractUnicodeDirective(length), ZoneBasedUnicodeDirective {
    override val formatLetter = 'V'
    override fun addToFormat(builder: ValueBagFormatBuilder) = builder.appendTimeZoneId()
}

private class GenericTimeZoneName(length: Int) : AbstractUnicodeDirective(length), ZoneBasedUnicodeDirective {
    override val formatLetter = 'v'
    override fun addToFormat(builder: ValueBagFormatBuilder) = localizedDirective()
}

private class TimeZoneName(length: Int) : AbstractUnicodeDirective(length), ZoneBasedUnicodeDirective {
    override val formatLetter = 'z'
    override fun addToFormat(builder: ValueBagFormatBuilder) =
        localizedDirective("Format 'V' can be used to format time zone IDs in a locale-invariant manner.")
}

private class LocalizedZoneOffset(length: Int) : AbstractUnicodeDirective(length), OffsetBasedUnicodeDirective {
    override val formatLetter = 'O'
    override fun addToFormat(builder: UtcOffsetFormatBuilderFields) = localizedDirective()
}

private class ZoneOffset1(length: Int) : AbstractUnicodeDirective(length), OffsetBasedUnicodeDirective {
    override val formatLetter = 'X'
    override fun addToFormat(builder: UtcOffsetFormatBuilderFields) {
        when (formatLength) {
            1 -> builder.appendIsoOffset(zOnZero = true, useSeparator = false, outputMinute = WhenToOutput.IF_NONZERO, outputSecond = WhenToOutput.NEVER)
            2 -> builder.appendIsoOffset(zOnZero = true, useSeparator = false, outputMinute = WhenToOutput.ALWAYS, outputSecond = WhenToOutput.NEVER)
            3 -> builder.appendIsoOffset(zOnZero = true, useSeparator = true, outputMinute = WhenToOutput.ALWAYS, outputSecond = WhenToOutput.NEVER)
            4 -> builder.appendIsoOffset(zOnZero = true, useSeparator = false, outputMinute = WhenToOutput.ALWAYS, outputSecond = WhenToOutput.IF_NONZERO)
            5 -> builder.appendIsoOffset(zOnZero = true, useSeparator = true, outputMinute = WhenToOutput.ALWAYS, outputSecond = WhenToOutput.IF_NONZERO)
            else -> unknownLength()
        }
    }
}

private class ZoneOffset2(length: Int) : AbstractUnicodeDirective(length), OffsetBasedUnicodeDirective {
    override val formatLetter = 'x'
    override fun addToFormat(builder: UtcOffsetFormatBuilderFields) {
        when (formatLength) {
            1 -> builder.appendIsoOffset(zOnZero = false, useSeparator = false, outputMinute = WhenToOutput.IF_NONZERO, outputSecond = WhenToOutput.NEVER)
            2 -> builder.appendIsoOffset(zOnZero = false, useSeparator = false, outputMinute = WhenToOutput.ALWAYS, outputSecond = WhenToOutput.NEVER)
            3 -> builder.appendIsoOffset(zOnZero = false, useSeparator = true, outputMinute = WhenToOutput.ALWAYS, outputSecond = WhenToOutput.NEVER)
            4 -> builder.appendIsoOffset(zOnZero = false, useSeparator = false, outputMinute = WhenToOutput.ALWAYS, outputSecond = WhenToOutput.IF_NONZERO)
            5 -> builder.appendIsoOffset(zOnZero = false, useSeparator = true, outputMinute = WhenToOutput.ALWAYS, outputSecond = WhenToOutput.IF_NONZERO)
            else -> unknownLength()
        }
    }
}

private class ZoneOffset3(length: Int) : AbstractUnicodeDirective(length), OffsetBasedUnicodeDirective {
    override val formatLetter = 'Z'
    override fun addToFormat(builder: UtcOffsetFormatBuilderFields) {
        when (formatLength) {
            1, 2, 3 -> builder.appendIsoOffset(zOnZero = false, useSeparator = false, outputMinute = WhenToOutput.ALWAYS, outputSecond = WhenToOutput.NEVER)
            4 -> LocalizedZoneOffset(4).addToFormat(builder)
            5 -> builder.appendIsoOffset(zOnZero = false, useSeparator = true, outputMinute = WhenToOutput.ALWAYS, outputSecond = WhenToOutput.IF_NONZERO)
            else -> unknownLength()
        }
    }
}

private class UnknownUnicodeDirective(override val formatLetter: Char, length: Int) : AbstractUnicodeDirective(length)

internal fun unicodeDirective(char: Char, length: Int): UnicodeFormat = when (char) {
    'G' -> Era(length)
    'y' -> YearOfEra(length)
    'Y' -> WeekBasedYear(length)
    'u' -> Year(length)
    'U' -> CyclicYearName(length)
    'r' -> RelatedGregorianYear(length)
    'Q' -> QuarterOfYear(length)
    'q' -> StandaloneQuarterOfYear(length)
    'M' -> MonthOfYear(length)
    'L' -> StandaloneMonthOfYear(length)
    'w' -> WeekOfWeekBasedYear(length)
    'W' -> WeekOfMonth(length)
    'd' -> DayOfMonth(length)
    'D' -> DayOfYear(length)
    'F' -> DayOfWeekInMonth(length)
    'g' -> ModifiedJulianDay(length)
    'E' -> DayOfWeekDirective(length)
    'e' -> LocalizedDayOfWeek(length)
    'c' -> StandaloneLocalizedDayOfWeek(length)
    'a' -> AmPmMarker(length)
    'H' -> HourOfDay(length)
    'm' -> MinuteOfHour(length)
    's' -> SecondOfMinute(length)
    'S' -> FractionOfSecond(length)
    'A' -> MilliOfDay(length)
    'n' -> NanoOfSecond(length)
    'N' -> NanoOfDay(length)
    'V' -> TimeZoneId(length)
    'v' -> GenericTimeZoneName(length)
    'z' -> TimeZoneName(length)
    'O' -> LocalizedZoneOffset(length)
    'X' -> ZoneOffset1(length)
    'x' -> ZoneOffset2(length)
    'Z' -> ZoneOffset3(length)
    else -> UnknownUnicodeDirective(char, length)
}

private data class UnicodeStringLiteral(val literal: String) : UnicodeFormat {
    override fun toString(): String = if (literal == "'") "''" else
        if (literal.any { it.isLetter() }) "'$literal'"
        else if (literal.isEmpty()) ""
        else literal
}

private data class UnicodeFormatSequence(val formats: List<UnicodeFormat>) : UnicodeFormat {
    override fun toString(): String = formats.joinToString("")
}

internal fun directivesInFormat(format: UnicodeFormat): List<UnicodeDirective> = when (format) {
    is UnicodeDirective -> listOf(format)
    is UnicodeFormatSequence -> format.formats.flatMapTo(mutableListOf()) { directivesInFormat(it) }
    is UnicodeOptionalGroup -> directivesInFormat(format.format)
    is UnicodeStringLiteral -> listOf()
}

@SharedImmutable
private val nonPlainCharacters = ('a'..'z') + ('A'..'Z') + listOf('[', ']', '\'')

internal fun parseUnicodeFormat(format: String): UnicodeFormat {
    val groups: MutableList<MutableList<UnicodeFormat>?> = mutableListOf(mutableListOf())
    var insideLiteral = false
    var literal = ""
    var lastCharacter: Char? = null
    var lastCharacterCount = 0
    for (character in format) {
        if (character == lastCharacter) {
            ++lastCharacterCount
        } else if (insideLiteral) {
            if (character == '\'') {
                groups.last()?.add(UnicodeStringLiteral(literal.ifEmpty { "'" }))
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
                groups.last()?.add(UnicodeStringLiteral(literal))
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
                    val group = groups.removeLast() ?: throw IllegalArgumentException("Unmatched closing bracket")
                    groups.last()?.add(UnicodeOptionalGroup(UnicodeFormatSequence(group)))
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
        groups.last()?.add(UnicodeStringLiteral(literal))
    }
    return UnicodeFormatSequence(groups.removeLast() ?: throw IllegalArgumentException("Unmatched opening bracket"))
}

private fun unsupportedDirective(fieldName: String, recommendation: String? = null): Nothing =
    throw UnsupportedOperationException(
        "kotlinx.datetime formatting does not support the $fieldName field. " +
            (if (recommendation != null) "$recommendation " else "") +
            "Please report your use case to https://github.com/Kotlin/kotlinx-datetime/issues"
    )

private fun AbstractUnicodeDirective.unknownLength(): Nothing =
    throw IllegalArgumentException("Unknown length $formatLength for the $formatLetter directive")

private fun AbstractUnicodeDirective.localizedDirective(recommendation: String? = null): Nothing =
    throw IllegalArgumentException("The directive '$this' is locale-dependent, but locales are not supported in Kotlin"
        + if (recommendation != null) ". $recommendation" else "")
