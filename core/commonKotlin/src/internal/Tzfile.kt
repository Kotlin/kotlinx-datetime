/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*

internal class TzFileData(
    val leapSecondRules: List<LeapSecondRule>,
    val transitions: List<Transition>,
    val states: List<ClockState>,
) {
    /**
     * The list of rules for inserting leap seconds.
     */
    class LeapSecondRule(
        /**
         * The time at which a new leap second is to be inserted.
         */
        val time: Long,
        /**
         * The total number of leap seconds to be applied after [time] and before the next rule.
         */
        val total: Int
    )

    class ClockState(
        val offset: TzFileOffset,
        val isDst: Boolean,
        val abbreviation: String,
    )

    class Transition(
        val time: Long,
        val stateIndex: Int
    )
}

internal class TzFile(val data: TzFileData, val rules: PosixTzString?) {
    fun toTimeZoneRules(): TimeZoneRules {
        val tzOffsets = buildList {
            add(data.states[0].offset)
            data.transitions.forEach { add(data.states[it.stateIndex].offset) }
        }
        val offsets = tzOffsets.map { it.toUtcOffset() }
        return TimeZoneRules(data.transitions.map { it.time }, offsets, rules?.toRecurringZoneRules())
    }
}

/**
 * An extension of [UtcOffset] to support the full range of offsets used in `tzfile`, which is:
 *
 * ```
 * The tt_utoff value is never equal to -2**31, to let 32-bit clients negate it without overflow.
 * Also, in realistic applications tt_utoff is in the range [-89999, 93599] (i.e., more than -25 hours and less than 26
 * hours); this allows easy support by implementations that already support the POSIX-required range
 * [-24:59:59, 25:59:59].
 * ```
 */
internal class TzFileOffset(val totalSeconds: Int) {
    /**
     * Converts this offset to a [UtcOffset].
     *
     * @throws IllegalArgumentException if the offset is not in the range [-18 hours, +18 hours].
     */
    fun toUtcOffset(): UtcOffset = UtcOffset(seconds = totalSeconds)
}

// https://datatracker.ietf.org/doc/html/rfc8536
internal fun readTzFile(data: ByteArray): TzFile {
    class Header(
        val version: Int?,
        val ttisutcnt: Int,
        val ttisstdcnt: Int,
        val leapcnt: Int,
        val timecnt: Int,
        val typecnt: Int,
        val charcnt: Int
    ) {
        override fun toString(): String = "Header(version=$version, ttisutcnt=$ttisutcnt, ttisstdcnt=$ttisstdcnt, " +
            "leapcnt=$leapcnt, timecnt=$timecnt, typecnt=$typecnt, charcnt=$charcnt)"
    }

    class Ttinfo(val utoff: Int, val isdst: Boolean, val abbrind: UByte) {
        override fun toString(): String = "Ttinfo(utoff=$utoff, isdst=$isdst, abbrind=$abbrind)"
    }

    inline fun BinaryDataReader.readData(header: Header, readTime: () -> Long): TzFileData {
        val transitionTimes = List(header.timecnt) { readTime() }
        val transitionTypes = List(header.timecnt) { readByte() }
        val ttinfos = List(header.typecnt) {
            Ttinfo(
                readInt(),
                readByte() != 0.toByte(),
                readUnsignedByte()
            )
        }
        val abbreviations = List(header.charcnt) { readByte() }
        fun abbreviationForIndex(startIndex: UByte): String = abbreviations.drop(startIndex.toInt())
            .takeWhile { byte -> byte != 0.toByte() }.toByteArray().decodeToString()
        val leapSecondRules = List(header.leapcnt) {
            TzFileData.LeapSecondRule(
                readTime(),
                readInt()
            )
        }
        // The following fields are not used in practice. See https://datatracker.ietf.org/doc/html/rfc8536#section-3.2,
        // near the end of the section, `A given pair of standard/wall and UT/local indicators...`
        repeat(header.ttisstdcnt) { readByte() }
        repeat(header.ttisutcnt) { readByte() }
        return TzFileData(
            leapSecondRules,
            transitionTimes.zip(transitionTypes) { time, type -> TzFileData.Transition(time, type.toInt()) },
            ttinfos.map { TzFileData.ClockState(TzFileOffset(it.utoff), it.isdst, abbreviationForIndex(it.abbrind)) }
        )
    }

    fun BinaryDataReader.read32BitData(header: Header): TzFileData = readData(header) { readInt().toLong() }

    fun BinaryDataReader.read64BitData(header: Header): TzFileData = readData(header) { readLong() }

    inline fun BinaryDataReader.readFooter() = check(readByte() == '\n'.code.toByte()).let {
        PosixTzString.readIfPresent(this)
    }

    val reader = BinaryDataReader(data)

    fun readHeader(): Header {
        val magic = reader.readUtf8String(4)
        check(magic == "TZif") { "Invalid tzfile magic: '$magic', expected 'TZif'" }
        val version = when (reader.readByte()) {
            0.toByte() -> 1
            0x32.toByte() -> 2
            0x33.toByte() -> 3
            else -> null
        }
        reader.skip(15)
        return Header(
            version,
            reader.readInt(),
            reader.readInt(),
            reader.readInt(),
            reader.readInt(),
            reader.readInt(),
            reader.readInt()
        ).also {
            check(it.ttisutcnt == 0 || it.ttisutcnt == it.typecnt)
            check(it.ttisstdcnt == 0 || it.ttisstdcnt == it.typecnt)
        }
    }

    val header = readHeader()
    return when (header.version) {
        1 -> {
            TzFile(reader.read32BitData(header), null)
        }
        else -> {
            reader.read32BitData(header) // skipped
            val newHeader = readHeader()
            val parsedData = reader.read64BitData(newHeader)
            val footer = reader.readFooter()
            TzFile(parsedData, footer)
        }
    }
}

internal class PosixTzString(
    private val standardTime: Pair<String, UtcOffset>,
    private val daylightTime: Pair<String, UtcOffset>?,
    private val rules: Pair<MonthDayTime, MonthDayTime>?,
) {
    companion object {
        /**
         * Reads a POSIX TZ string from the [reader] if it is present, or returns `null` if it is not.
         *
         * The string format is described in https://pubs.opengroup.org/onlinepubs/9699919799/, section 8.3,
         * with additional extensions in https://datatracker.ietf.org/doc/html/rfc8536#section-3.3.1
         *
         * @throws IllegalArgumentException if the string is invalid
         * @throws IllegalStateException if the string is invalid
         */
        fun readIfPresent(reader: BinaryDataReader): PosixTzString? = reader.readPosixTzString()
    }

    fun toRecurringZoneRules(): RecurringZoneRules? {
        /**
         * In theory, it's possible to have a DST transition but no start/end date.
         * In this case, the behavior is not specified, and on Linux, the rules for America/New_York are used
         * to determine the start/end dates (see `tzset(3)`, search for `posixrules`).
         * The <https://github.com/HowardHinnant/date> library takes the lack of start/end dates
         * to mean that the standard offset is always in effect, which seems to be a much more reasonable interpretation.
         */
        if (daylightTime == null || rules == null) return null
        val (start, end) = rules
        val rule1 = RecurringZoneRules.Rule(start, standardTime.second, daylightTime.second)
        val rule2 = RecurringZoneRules.Rule(end, daylightTime.second, standardTime.second)
        return RecurringZoneRules(listOf(rule1, rule2))
    }
}
private fun BinaryDataReader.readPosixTzString(): PosixTzString? {
    var c = readAsciiChar()
    fun readName(): String? {
        if (c == '\n') return null
        val name = StringBuilder()
        if (c == '<') {
            c = readAsciiChar()
            while (c != '>') {
                check(c.isLetterOrDigit() || c == '-' || c == '+') { "Invalid char '$c' in the std name in POSIX TZ string" }
                name.append(c)
                c = readAsciiChar()
            }
            c = readAsciiChar()
        } else {
            while (c.isLetter()) {
                name.append(c)
                c = readAsciiChar()
            }
        }
        check(name.isNotEmpty()) { "Empty std name in POSIX TZ string" }
        return name.toString()
    }

    fun readOffset(): UtcOffset? {
        if (c == '\n') return null
        val offsetIsNegative: Boolean
        when (c) {
            '-' -> {
                offsetIsNegative = true
                c = readAsciiChar()
            }

            '+' -> {
                offsetIsNegative = false
                c = readAsciiChar()
            }

            else -> {
                if (!c.isDigit()) return null
                offsetIsNegative = false
            }
        }
        val sign = if (offsetIsNegative) 1 else -1 // not a typo: the sign is inverted in the rules
        var hours = c.digitToInt()
        c = readAsciiChar()
        if (c.isDigit()) {
            hours = hours * 10 + c.digitToInt()
            c = readAsciiChar()
        }
        if (c != ':') return UtcOffset(sign * hours)
        val minutes = readAsciiChar().digitToInt() * 10 + readAsciiChar().digitToInt()
        c = readAsciiChar()
        if (c != ':') return UtcOffset(sign * hours, sign * minutes)
        val seconds = readAsciiChar().digitToInt() * 10 + readAsciiChar().digitToInt()
        c = readAsciiChar()
        return UtcOffset(sign * hours, sign * minutes, sign * seconds)
    }

    fun readDate(): DateOfYear? {
        if (c == '\n') return null
        check(c == ',') { "Invalid char '$c' in POSIX TZ string after the DST offset" }
        c = readAsciiChar()
        return when (c) {
            'J' -> {
                c = readAsciiChar()
                var result = 0
                while (c.isDigit()) {
                    result = result * 10 + c.digitToInt()
                    c = readAsciiChar()
                }
                JulianDayOfYearSkippingLeapDate(result)
            }
            'M' -> {
                c = readAsciiChar()
                var month = c.digitToInt()
                c = readAsciiChar()
                if (c.isDigit()) {
                    month = month * 10 + c.digitToInt()
                    c = readAsciiChar()
                }
                check(c == '.') { "Invalid char '$c' in POSIX TZ string after M$month" }
                c = readAsciiChar()
                val week = c.digitToInt()
                check(week in 1..5) { "Invalid week number '$week' in POSIX TZ string after M$month" }
                c = readAsciiChar()
                check(c == '.') { "Invalid char '$c' in POSIX TZ string after M$month.$week" }
                c = readAsciiChar()
                val dayOfWeek = when (val n = c.digitToInt()) {
                    0 -> DayOfWeek.SUNDAY
                    else -> DayOfWeek(n)
                }
                val dayOfMonth: MonthDayOfYear.TransitionDay = when (week) {
                    5 -> MonthDayOfYear.TransitionDay.Last(dayOfWeek, null)
                    else -> MonthDayOfYear.TransitionDay.Nth(dayOfWeek, week)
                }
                c = readAsciiChar()
                MonthDayOfYear(Month(month), dayOfMonth)
            }
            else -> {
                check(c.isDigit()) { "Invalid char '$c' in POSIX TZ string after the DST offset" }
                var result = 0
                while (c.isDigit()) {
                    result = result * 10 + c.digitToInt()
                    c = readAsciiChar()
                }
                JulianDayOfYear(result)
            }
        }
    }

    fun readTime(): MonthDayTime.TransitionLocaltime? {
        if (c != '/') return null
        c = readAsciiChar()
        val hourIsNegative: Boolean
        when (c) {
            '-' -> {
                hourIsNegative = true
                c = readAsciiChar()
            }
            else -> {
                if (!c.isDigit()) return null
                hourIsNegative = false
            }
        }
        var hour = c.digitToInt()
        c = readAsciiChar()
        while (c.isDigit()) {
            hour = hour * 10 + c.digitToInt()
            c = readAsciiChar()
        }
        hour *= if (hourIsNegative) -1 else 1
        if (c != ':') return MonthDayTime.TransitionLocaltime(hour, 0, 0)
        val minutes = readAsciiChar().digitToInt() * 10 + readAsciiChar().digitToInt()
        c = readAsciiChar()
        if (c != ':') return MonthDayTime.TransitionLocaltime(hour, minutes, 0)
        val seconds = readAsciiChar().digitToInt() * 10 + readAsciiChar().digitToInt()
        c = readAsciiChar()
        return MonthDayTime.TransitionLocaltime(hour, minutes, seconds)
    }

    val std = readName() ?: return null
    val stdOffset = readOffset() ?: throw IllegalArgumentException("Could not parse the std offset in POSIX TZ string")
    val dst = readName() ?: return PosixTzString(std to stdOffset, null, null)
    val dstOffset = readOffset() ?: UtcOffset(seconds = stdOffset.totalSeconds + 3600)
    val startDate = readDate() ?: return PosixTzString(std to stdOffset, dst to dstOffset, null)
    val startTime = readTime() ?: MonthDayTime.TransitionLocaltime(2, 0, 0)
    val endDate = readDate() ?: throw IllegalArgumentException("Could not parse the end date in POSIX TZ string")
    val endTime = readTime() ?: MonthDayTime.TransitionLocaltime(2, 0, 0)
    val start = MonthDayTime(startDate, startTime, MonthDayTime.OffsetResolver.WallClockOffset)
    val end = MonthDayTime(endDate, endTime, MonthDayTime.OffsetResolver.WallClockOffset)
    return PosixTzString(std to stdOffset, dst to dstOffset, start to end)
}
