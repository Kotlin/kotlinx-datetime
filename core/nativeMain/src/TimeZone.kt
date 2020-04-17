/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.cinterop.*
import platform.posix.*

class DateTimeException(str: String? = null) : Exception(str)

public actual open class TimeZone internal constructor(actual val id: String) {

    actual companion object {
        actual val SYSTEM: TimeZone
            get() = memScoped {
                val string = get_system_timezone() ?: throw RuntimeException("Failed to get the system timezone.")
                val kotlinString = string.toKString()
                free(string)
                TimeZone(kotlinString)
            }

        actual val UTC: TimeZone = ZoneOffset.UTC

        // org.threeten.bp.ZoneId#of(java.lang.String)
        actual fun of(zoneId: String): TimeZone {
            // TODO: normalize aliases?
            if (zoneId == "Z") {
                return UTC
            }
            if (zoneId.length == 1) {
                throw DateTimeException("Invalid zone: $zoneId")
            }
            if (zoneId.startsWith("+") || zoneId.startsWith("-")) {
                return ZoneOffset.of(zoneId)
            }
            if (zoneId == "UTC" || zoneId == "GMT" || zoneId == "UT") {
                return ZoneOffset(0, zoneId)
            }
            if (zoneId.startsWith("UTC+") || zoneId.startsWith("GMT+") ||
                zoneId.startsWith("UTC-") || zoneId.startsWith("GMT-")) {
                val offset = ZoneOffset.of(zoneId.substring(3))
                return if (offset.totalSeconds == 0) {
                    ZoneOffset(0, zoneId.substring(0, 3))
                } else ZoneOffset(offset.totalSeconds, zoneId.substring(0, 3) + offset.id)
            }
            if (zoneId.startsWith("UT+") || zoneId.startsWith("UT-")) {
                val offset = ZoneOffset.of(zoneId.substring(2))
                return if (offset.totalSeconds == 0) {
                    ZoneOffset(0, "UT")
                } else ZoneOffset(offset.totalSeconds, "UT" + offset.id)
            }
            if (!is_known_timezone(zoneId)) {
                throw IllegalArgumentException("Invalid timezone '$zoneId'")
            }
            return TimeZone(zoneId)
        }

        actual val availableZoneIds: Set<String>
            get() {
                val set = mutableSetOf<String>()
                val zones = available_zone_ids()
                    ?: throw RuntimeException("Failed to get the list of available timezones")
                var ptr = zones
                while (true) {
                    val cur = ptr.pointed.value ?: break
                    val zoneName = cur.toKString()
                    set.add(zoneName)
                    free(cur)
                    ptr = (ptr + 1)!!
                }
                free(zones)
                return set
            }
    }

    // org.threeten.bp.LocalDateTime#ofEpochSecond + org.threeten.bp.ZonedDateTime#create
    internal fun Instant.toZonedLocalDateTime(): ZonedDateTime {
        val localSecond: Long = epochSeconds + offset.totalSeconds // overflow caught later
        val localEpochDay: Long = floorDiv(localSecond, SECONDS_PER_DAY.toLong())
        val secsOfDay: Long = floorMod(localSecond, SECONDS_PER_DAY.toLong())
        val date: LocalDate = LocalDate.ofEpochDay(localEpochDay)
        val time: LocalTime = LocalTime.ofSecondOfDay(secsOfDay, nanos)
        return ZonedDateTime(LocalDateTime(date, time), this@TimeZone, offset)
    }

    actual fun Instant.toLocalDateTime(): LocalDateTime = toZonedLocalDateTime().dateTime

    actual open val Instant.offset: ZoneOffset
        get() {
            val offset = offset_at_instant(id, epochSeconds)
            if (offset == INT_MAX) {
                throw RuntimeException("Unable to acquire the offset at instant $this for zone ${this@TimeZone}")
            }
            return ZoneOffset(offset)
        }

    actual fun LocalDateTime.toInstant(): Instant {
        val zoned = atZone()
        return Instant(zoned.dateTime.toEpochSecond(zoned.offset), nanosecond)
    }

    internal open fun LocalDateTime.atZone(preferred: ZoneOffset? = null): ZonedDateTime = memScoped {
        val epochSeconds = toEpochSecond(ZoneOffset(0))
        val offset = alloc<IntVar>()
        offset.value = preferred?.totalSeconds ?: INT_MAX
        val transitionDuration = offset_at_datetime(id, epochSeconds, offset.ptr)
        if (offset.value == INT_MAX) {
            throw RuntimeException("Unable to acquire the offset at ${this@atZone} for zone ${this@TimeZone}")
        }
        val dateTime = this@atZone.plusSeconds(transitionDuration.toLong())
        ZonedDateTime(dateTime, this@TimeZone, ZoneOffset(offset.value))
    }

    // org.threeten.bp.ZoneId#equals
    override fun equals(other: Any?): Boolean =
        this === other || other is TimeZone && this.id == other.id

    // org.threeten.bp.ZoneId#hashCode
    override fun hashCode(): Int = id.hashCode()

    // org.threeten.bp.ZoneId#toString
    override fun toString(): String = id

}

public actual class ZoneOffset internal constructor(actual val totalSeconds: Int, id: String? = null) : TimeZone(id
    ?: zoneIdByOffset(totalSeconds)) {

    companion object {
        // org.threeten.bp.ZoneOffset#UTC
        val UTC = ZoneOffset(0)

        // org.threeten.bp.ZoneOffset#of
        internal fun of(offsetId: String): ZoneOffset {
            if (offsetId == "Z") {
                return UTC
            }

            // parse - +h, +hh, +hhmm, +hh:mm, +hhmmss, +hh:mm:ss
            val hours: Int
            val minutes: Int
            val seconds: Int
            when (offsetId.length) {
                2 -> return of(offsetId[0].toString() + "0" + offsetId[1])
                3 -> {
                    hours = parseNumber(offsetId, 1, false)
                    minutes = 0
                    seconds = 0
                }
                5 -> {
                    hours = parseNumber(offsetId, 1, false)
                    minutes = parseNumber(offsetId, 3, false)
                    seconds = 0
                }
                6 -> {
                    hours = parseNumber(offsetId, 1, false)
                    minutes = parseNumber(offsetId, 4, true)
                    seconds = 0
                }
                7 -> {
                    hours = parseNumber(offsetId, 1, false)
                    minutes = parseNumber(offsetId, 3, false)
                    seconds = parseNumber(offsetId, 5, false)
                }
                9 -> {
                    hours = parseNumber(offsetId, 1, false)
                    minutes = parseNumber(offsetId, 4, true)
                    seconds = parseNumber(offsetId, 7, true)
                }
                else -> throw DateTimeException("Invalid ID for ZoneOffset, invalid format: $offsetId")
            }
            val first: Char = offsetId[0]
            if (first != '+' && first != '-') {
                throw DateTimeException("Invalid ID for ZoneOffset, plus/minus not found when expected: $offsetId")
            }
            return if (first == '-') {
                ofHoursMinutesSeconds(-hours, -minutes, -seconds)
            } else {
                ofHoursMinutesSeconds(hours, minutes, seconds)
            }
        }

        // org.threeten.bp.ZoneOffset#validate
        private fun validate(hours: Int, minutes: Int, seconds: Int) {
            if (hours < -18 || hours > 18) {
                throw DateTimeException("Zone offset hours not in valid range: value " + hours +
                    " is not in the range -18 to 18")
            }
            if (hours > 0) {
                if (minutes < 0 || seconds < 0) {
                    throw DateTimeException("Zone offset minutes and seconds must be positive because hours is positive")
                }
            } else if (hours < 0) {
                if (minutes > 0 || seconds > 0) {
                    throw DateTimeException("Zone offset minutes and seconds must be negative because hours is negative")
                }
            } else if (minutes > 0 && seconds < 0 || minutes < 0 && seconds > 0) {
                throw DateTimeException("Zone offset minutes and seconds must have the same sign")
            }
            if (abs(minutes) > 59) {
                throw DateTimeException("Zone offset minutes not in valid range: abs(value) " +
                    abs(minutes) + " is not in the range 0 to 59")
            }
            if (abs(seconds) > 59) {
                throw DateTimeException("Zone offset seconds not in valid range: abs(value) " +
                    abs(seconds) + " is not in the range 0 to 59")
            }
            if (abs(hours) == 18 && (abs(minutes) > 0 || abs(seconds) > 0)) {
                throw DateTimeException("Zone offset not in valid range: -18:00 to +18:00")
            }
        }

        // org.threeten.bp.ZoneOffset#ofHoursMinutesSeconds
        internal fun ofHoursMinutesSeconds(hours: Int, minutes: Int, seconds: Int): ZoneOffset {
            validate(hours, minutes, seconds)
            return if (hours == 0 && minutes == 0 && seconds == 0) UTC
            else ZoneOffset(hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds)
        }

        // org.threeten.bp.ZoneOffset#parseNumber
        private fun parseNumber(offsetId: CharSequence, pos: Int, precededByColon: Boolean): Int {
            if (precededByColon && offsetId[pos - 1] != ':') {
                throw DateTimeException("Invalid ID for ZoneOffset, colon not found when expected: $offsetId")
            }
            val ch1 = offsetId[pos]
            val ch2 = offsetId[pos + 1]
            if (ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9') {
                throw DateTimeException("Invalid ID for ZoneOffset, non numeric characters found: $offsetId")
            }
            return (ch1.toInt() - 48) * 10 + (ch2.toInt() - 48)
        }
    }

    internal override fun LocalDateTime.atZone(preferred: ZoneOffset?): ZonedDateTime =
        ZonedDateTime(this@atZone, this@ZoneOffset, this@ZoneOffset)

    override val Instant.offset: ZoneOffset
        get() = this@ZoneOffset

    // org.threeten.bp.ZoneOffset#toString
    override fun toString(): String = id

    // org.threeten.bp.ZoneOffset#hashCode
    override fun hashCode(): Int = totalSeconds

    // org.threeten.bp.ZoneOffset#equals
    override fun equals(other: Any?): Boolean =
        this === other || other is ZoneOffset && totalSeconds == other.totalSeconds
}
