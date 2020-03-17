/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.cinterop.*
import platform.posix.*

class DateTimeException(str: String? = null) : Exception(str)

public actual open class TimeZone(actual val id: String) {

    actual companion object {
        actual val SYSTEM: TimeZone
            get() = memScoped {
                val string = get_system_timezone()
                val kotlinString = string!!.toKString()
                free(string)
                TimeZone(kotlinString)
            }
        actual val UTC: TimeZone = ZoneOffset(0)

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
                val zones = available_zone_ids()!!
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
        get() = ZoneOffset(offset_at_instant(id, epochSeconds))

    actual fun LocalDateTime.toInstant(): Instant =
        Instant(toEpochSecond(presumedOffset()), nanosecond)

    internal open fun LocalDateTime.presumedOffset(preferred: ZoneOffset? = null): ZoneOffset =
        ZoneOffset(offset_at_datetime(id, toEpochSecond(ZoneOffset(0)), preferred?.totalSeconds ?: INT_MAX))

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is TimeZone && this.id == other.id)

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id

}

public actual class ZoneOffset(actual val totalSeconds: Int, id: String? = null) : TimeZone(id
    ?: zoneIdByOffset(totalSeconds)) {

    companion object {
        fun of(offsetId: String): ZoneOffset {
            if (offsetId == "Z") {
                return ZoneOffset(0)
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

        private fun ofHoursMinutesSeconds(hours: Int, minutes: Int, seconds: Int): ZoneOffset =
            ZoneOffset(hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds)

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

    internal override fun LocalDateTime.presumedOffset(preferred: ZoneOffset?): ZoneOffset = this@ZoneOffset

    override val Instant.offset: ZoneOffset
        get() = this@ZoneOffset
}

// TODO: transition duration
