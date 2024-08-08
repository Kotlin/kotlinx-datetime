/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format.test

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import java.text.ParsePosition
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(FormatStringsInDatetimeFormats::class)
class UnicodeFormatTest {

    @Test
    fun testTop100UnicodeFormats() {
        val nonLocalizedPatterns = listOf(
            "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "HH:mm", "HH:mm:ss", "dd/MM/yyyy", "yyyyMMdd", "dd.MM.yyyy",
            "yyyy-MM-dd HH:mm", "yyyy", "dd-MM-yyyy", "yyyyMMddHHmmss", "yyyy-MM-dd HH:mm:ss.SSS", "yyyy/MM/dd",
            "yyyy-MM-dd'T'HH:mm:ss", "MM/dd/yyyy", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "HH:mm:ss.SSS", "dd.MM.yyyy HH:mm", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM", "dd/MM/yyyy HH:mm:ss",
            "M/d/yyyy", "yyyy-MM-dd HH:mm:ss.S", "HH", "dd-MM-yyyy HH:mm:ss", "dd/MM/yyyy HH:mm", "dd",
            "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy.MM.dd", "HHmmss",
            "dd.MM.yyyy HH:mm:ss", "MM", "yyyy-MM-dd HH:mm:ss.SSSSSS", "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyMMddHHmmss", "MM/dd/yyyy HH:mm:ss",
            "yyyy-MM-dd-HH-mm-ss", "yyyyMM", "yyyyMMddHHmm", "H:mm",
            "dd-MM-yyyy HH:mm", "yyyyMMdd_HHmmss", "yyyy-MM-dd'T'HH:mm:ss.SSSX", "MM-dd-yyyy",
            "yyyy-MM-dd_HH-mm-ss", "mm", "dd/MM/yy", "ddMMyy",
            "uuuu-MM-dd", "dd.MM.yy", "yyyy-MM-dd'T'HH:mm", "yyyyMMdd-HHmmss",
            "uuuu-MM-dd'T'HH:mm:ss", "yyyy MM dd", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "uuuu-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-M-d", "d", "yyMMdd", "yyyyMMddHH",
            "HHmm", "MM/dd/yy",
            "yyyy_MM_dd_HH_mm_ss", "yyyy-MM-d 'at' HH:mm ", "yyyy:MM:dd HH:mm:ss",
            "yyyy年MM月dd日 HH:mm:ss", "yyyy年MM月dd日", "dd.MM.yyyy. HH:mm:ss", "ss", "ddMMyyyy",
            "yyyyMMdd'T'HHmmss'Z'", "yyyyMMdd'T'HHmmss", "yyyy-MM-dd'T'HH:mm:ssX",
        )
        val localizedPatterns = listOf(
            "MMMM", "hh:mm a", "h:mm a", "dd MMMM yyyy", "dd MMM yyyy", "yyyy-MM-dd hh:mm:ss", "d MMMM yyyy", "MMM",
            "MMM dd, yyyy", "dd-MMM-yyyy", "d MMM yyyy", "MMM yyyy", "MMMM yyyy", "EEE", "EEEE", "hh:mm:ss a",
            "d MMM uuuu HH:mm:ss ", "MMMM d, yyyy", "MMMM dd, yyyy", "yyyy-MM-dd HH:mm:ss z", "hh:mm", "MMM dd",
        )
        val unsupportedPatterns = listOf(
            "YYYY-MM-dd",
        )
        // "yyyyMMddHHmmssSSS" is also in the top-100 list, but parsing it fails on Java
        for (pattern in unsupportedPatterns) {
            assertFailsWith<UnsupportedOperationException> {
                DateTimeComponents.Format {
                    byUnicodePattern(pattern)
                }
            }
        }
        for (pattern in localizedPatterns) {
            val error = assertFailsWith<IllegalArgumentException> {
                DateTimeComponents.Format {
                    byUnicodePattern(pattern)
                }
            }
            assertContains(error.message!!, "locale-dependent")
        }
        for (pattern in nonLocalizedPatterns) {
            checkPattern(pattern)
        }
    }

    @Test
    fun testOptionalSection() {
        checkPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]X")
    }

    @Test
    fun testDayOfYearFormats() {
        checkPattern("yyyyDDDHHmm")
    }

    private fun checkPattern(pattern: String) {
        val unicodeFormat = UnicodeFormat.parse(pattern)
        val directives = directivesInFormat(unicodeFormat)
        val dates = when {
            directives.any {
                it is UnicodeFormat.Directive.DateBased.Year && it.formatLength == 2
                        || it is UnicodeFormat.Directive.DateBased.YearOfEra && it.formatLength == 2
            } -> interestingDates21stCentury

            directives.any { it is UnicodeFormat.Directive.DateBased.YearOfEra } -> interestingDatesPositive
            directives.any { it is UnicodeFormat.Directive.DateBased } -> interestingDates
            else -> listOf(LocalDate(1970, 1, 1))
        }
        val times = when {
            directives.any { it is UnicodeFormat.Directive.TimeBased.WithSubsecondPrecision } -> interestingTimes
            directives.any { it is UnicodeFormat.Directive.TimeBased.WithSecondPrecision } ->
                interestingTimesWithZeroNanoseconds

            directives.any { it is UnicodeFormat.Directive.TimeBased } -> interestingTimesWithZeroSeconds
            else -> listOf(LocalTime(0, 0))
        }
        val offsets = when {
            directives.any { it is UnicodeFormat.Directive.OffsetBased && it.outputSeconds() != WhenToOutput.NEVER } ->
                interestingOffsets

            directives.any { it is UnicodeFormat.Directive.OffsetBased && it.outputMinutes() != WhenToOutput.NEVER } ->
                interestingOffsetsWithZeroSeconds

            else -> listOf(UtcOffset.ZERO)
        }
        val zones = when {
            directives.any { it is UnicodeFormat.Directive.ZoneBased } -> TimeZone.availableZoneIds
            else -> setOf("Europe/Berlin")
        }
        val format = DateTimeComponents.Format { byUnicodePattern(pattern) }
        val javaFormat = DateTimeFormatter.ofPattern(pattern)
        for (date in dates) {
            for (time in times) {
                for (offset in offsets) {
                    for (zone in zones) {
                        try {
                            val components = DateTimeComponents().apply {
                                setDate(date); setTime(time); setOffset(offset); timeZoneId = zone
                            }
                            val stringFromKotlin = format.format(components)
                            val stringFromJava = javaFormat.format(components.temporalAccessor())
                            val parsePosition = ParsePosition(0)
                            val parsed = javaFormat.parseUnresolved(stringFromKotlin, parsePosition)
                                ?: throw IllegalStateException("$parsePosition on $stringFromKotlin")
                            val componentsFromJava = parsed.query(dateTimeComponentsTemporalQuery)
                            // the string produced by Kotlin is parsable by Java:
                            assertEquals(stringFromKotlin, format.format(componentsFromJava))
                            val newStringFromJava = javaFormat.format(parsed)
                            assertEquals(stringFromJava, newStringFromJava)
                            // the string produced by Java is the same as the one by Kotlin:
                            assertEquals(stringFromKotlin, stringFromJava)
                            val componentsFromKotlin = format.parse(stringFromKotlin)
                            // the string produced by either Java or Kotlin is parsable by Kotlin:
                            assertEquals(stringFromKotlin, format.format(componentsFromKotlin))
                        } catch (e: Exception) {
                            throw AssertionError("On $date, $time, $offset, $zone, with pattern $pattern", e)
                        }
                    }
                }
            }
        }
    }
}

private fun DateTimeComponents.temporalAccessor() = object : TemporalAccessor {
    override fun isSupported(field: TemporalField?): Boolean = true

    override fun getLong(field: TemporalField?): Long {
        if (field === ChronoField.OFFSET_SECONDS) {
            return toUtcOffset().totalSeconds.toLong()
        } else {
            return toLocalDateTime().toJavaLocalDateTime().atZone(ZoneId.of(timeZoneId)).getLong(field)
        }
    }

}

private val dateTimeComponentsTemporalQuery = TemporalQuery { accessor ->
    DateTimeComponents().apply {
        for ((field, setter) in listOf<Pair<TemporalField, (Int) -> Unit>>(
            ChronoField.YEAR_OF_ERA to { year = it },
            ChronoField.YEAR to { year = it },
            ChronoField.MONTH_OF_YEAR to { monthNumber = it },
            ChronoField.DAY_OF_MONTH to { dayOfMonth = it },
            ChronoField.DAY_OF_YEAR to { dayOfYear = it },
            ChronoField.DAY_OF_WEEK to { dayOfWeek = DayOfWeek(it) },
            ChronoField.AMPM_OF_DAY to { amPm = if (it == 1) AmPmMarker.PM else AmPmMarker.AM },
            ChronoField.CLOCK_HOUR_OF_AMPM to { hourOfAmPm = it },
            ChronoField.HOUR_OF_DAY to { hour = it },
            ChronoField.MINUTE_OF_HOUR to { minute = it },
            ChronoField.SECOND_OF_MINUTE to { second = it },
            ChronoField.NANO_OF_SECOND to { nanosecond = it },
            ChronoField.OFFSET_SECONDS to { setOffset(UtcOffset(seconds = it)) },
        )) {
            if (accessor.isSupported(field)) {
                setter(accessor[field])
            }
        }
        timeZoneId = accessor.query(TemporalQueries.zoneId())?.id
    }
}

internal fun directivesInFormat(format: UnicodeFormat): List<UnicodeFormat.Directive> = when (format) {
    is UnicodeFormat.Directive -> listOf(format)
    is UnicodeFormat.Sequence -> format.formats.flatMapTo(mutableListOf()) { directivesInFormat(it) }
    is UnicodeFormat.OptionalGroup -> directivesInFormat(format.format)
    is UnicodeFormat.StringLiteral -> listOf()
}

val interestingDates: List<LocalDate> = listOf(
    LocalDate(2008, 7, 5),
    LocalDate(2007, 12, 31),
    LocalDate(1980, 12, 31),
    LocalDate(999, 11, 30),
    LocalDate(-1, 1, 2),
    LocalDate(9999, 10, 31),
    LocalDate(-9999, 9, 30),
    LocalDate(10000, 8, 1),
    LocalDate(-10000, 7, 1),
    LocalDate(123456, 6, 1),
    LocalDate(-123456, 5, 1),
)

val interestingDatesPositive: List<LocalDate> = listOf(
    LocalDate(2008, 7, 5),
    LocalDate(2007, 12, 31),
    LocalDate(1980, 12, 31),
    LocalDate(999, 11, 30),
    LocalDate(9999, 10, 31),
    LocalDate(10000, 8, 1),
    LocalDate(123456, 6, 1),
)

val interestingDates21stCentury: List<LocalDate> = listOf(
    LocalDate(2008, 7, 5),
    LocalDate(2007, 12, 31),
    LocalDate(2099, 1, 2),
    LocalDate(2034, 11, 30),
)

val interestingTimes: List<LocalTime> = listOf(
    LocalTime(0, 0, 0, 0),
    LocalTime(1, 0, 0, 0),
    LocalTime(23, 0, 0, 0),
    LocalTime(0, 1, 0, 0),
    LocalTime(12, 30, 0, 0),
    LocalTime(23, 59, 0, 0),
    LocalTime(0, 0, 1, 0),
    LocalTime(0, 0, 59, 0),
    LocalTime(0, 0, 0, 100000000),
    LocalTime(0, 0, 0, 10000000),
    LocalTime(0, 0, 0, 1000000),
    LocalTime(0, 0, 0, 100000),
    LocalTime(0, 0, 0, 10000),
    LocalTime(0, 0, 0, 1000),
    LocalTime(0, 0, 0, 100),
    LocalTime(0, 0, 0, 10),
    LocalTime(0, 0, 0, 1),
    LocalTime(0, 0, 0, 999999999),
    LocalTime(0, 0, 0, 998900000),
    LocalTime(0, 0, 0, 99999999),
    LocalTime(0, 0, 0, 9999999),
    LocalTime(0, 0, 0, 999999),
    LocalTime(0, 0, 0, 99999),
    LocalTime(0, 0, 0, 9999),
    LocalTime(0, 0, 0, 999),
    LocalTime(0, 0, 0, 99),
    LocalTime(0, 0, 0, 9),
)

val interestingTimesWithZeroNanoseconds: List<LocalTime> = listOf(
    LocalTime(0, 0, 0, 0),
    LocalTime(1, 0, 0, 0),
    LocalTime(23, 0, 0, 0),
    LocalTime(0, 1, 0, 0),
    LocalTime(12, 30, 0, 0),
    LocalTime(23, 59, 0, 0),
    LocalTime(0, 0, 1, 0),
    LocalTime(0, 0, 59, 0),
)

val interestingTimesWithZeroSeconds: List<LocalTime> = listOf(
    LocalTime(0, 0, 0, 0),
    LocalTime(1, 0, 0, 0),
    LocalTime(23, 0, 0, 0),
    LocalTime(0, 1, 0, 0),
    LocalTime(12, 30, 0, 0),
    LocalTime(23, 59, 0, 0),
)

val interestingOffsets: List<UtcOffset> = listOf(
    UtcOffset(-18),
    UtcOffset(-17, -59, -58),
    UtcOffset(-4, -3, -2),
    UtcOffset(0, 0, -1),
    UtcOffset(0, -1, 0),
    UtcOffset(0, -1, -1),
    UtcOffset(-1, 0, 0),
    UtcOffset(-1, 0, -1),
    UtcOffset(-1, -1, 0),
    UtcOffset(-1, -1, -1),
    UtcOffset(0, 0, 0),
    UtcOffset(0, 1, 0),
    UtcOffset(0, 1, 1),
    UtcOffset(1, 0, 0),
    UtcOffset(1, 0, 1),
    UtcOffset(1, 1, 0),
    UtcOffset(1, 1, 1),
    UtcOffset(4, 3, 2),
    UtcOffset(17, 59, 58),
    UtcOffset(18),
)

val interestingOffsetsWithZeroSeconds: List<UtcOffset> = listOf(
    UtcOffset(-18),
    UtcOffset(0, -1, 0),
    UtcOffset(-1, 0, 0),
    UtcOffset(-1, -1, 0),
    UtcOffset(0, 0, 0),
    UtcOffset(0, 1, 0),
    UtcOffset(1, 0, 0),
    UtcOffset(1, 1, 0),
    UtcOffset(18),
)
