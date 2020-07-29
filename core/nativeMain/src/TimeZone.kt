/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlin.math.abs
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.native.concurrent.*

internal expect fun getCurrentSystemDefaultTimeZone(): TimeZone

internal typealias TZID = platform.posix.size_t
internal expect val TZID_INVALID: TZID
internal expect fun available_zone_ids(): kotlinx.cinterop.CPointer<kotlinx.cinterop.CPointerVar<kotlinx.cinterop.ByteVar>>?
internal expect fun offset_at_datetime(zone: kotlinx.datetime.TZID /* = kotlin.ULong */, epoch_sec: platform.posix.int64_t /* = kotlin.Long */, offset: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.IntVar /* = kotlinx.cinterop.IntVarOf<kotlin.Int> */>?): kotlin.Int
internal expect fun at_start_of_day(zone: kotlinx.datetime.TZID /* = kotlin.ULong */, epoch_sec: platform.posix.int64_t /* = kotlin.Long */): kotlin.Long
internal expect fun offset_at_instant(zone: kotlinx.datetime.TZID /* = kotlin.ULong */, epoch_sec: platform.posix.int64_t /* = kotlin.Long */): kotlin.Int
internal expect fun timezone_by_name(zone_name: kotlin.String?): kotlinx.datetime.TZID /* = kotlin.ULong */

public actual open class TimeZone internal constructor(private val tzid: TZID, actual val id: String) {

    actual companion object {

        actual fun currentSystemDefault(): TimeZone = getCurrentSystemDefaultTimeZone()

        actual val UTC: TimeZone = ZoneOffset.UTC

        // org.threeten.bp.ZoneId#of(java.lang.String)
        actual fun of(zoneId: String): TimeZone {
            // TODO: normalize aliases?
            if (zoneId == "Z") {
                return UTC
            }
            if (zoneId.length == 1) {
                throw IllegalTimeZoneException("Invalid zone ID: $zoneId")
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
            val tzid = timezone_by_name(zoneId)
            if (tzid == TZID_INVALID) {
                throw IllegalTimeZoneException("No timezone found with zone ID '$zoneId'")
            }
            return TimeZone(tzid, zoneId)
        }

        actual val availableZoneIds: Set<String>
            get() {
                val set = mutableSetOf("UTC")
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

    actual fun Instant.toLocalDateTime(): LocalDateTime = try {
        toZonedLocalDateTime(this@TimeZone).dateTime
    } catch (e: IllegalArgumentException) {
        throw DateTimeArithmeticException("Instant ${this@toLocalDateTime} is not representable as LocalDateTime", e)
    }

    actual open val Instant.offset: ZoneOffset
        get() {
            val offset = offset_at_instant(tzid, epochSeconds)
            if (offset == INT_MAX) {
                throw RuntimeException("Unable to acquire the offset at instant $this for zone ${this@TimeZone}")
            }
            return ZoneOffset.ofSeconds(offset)
        }

    actual fun LocalDateTime.toInstant(): Instant = atZone().toInstant()

    internal open fun atStartOfDay(date: LocalDate): Instant = memScoped {
        val ldt = LocalDateTime(date, LocalTime.MIN)
        val epochSeconds = ldt.toEpochSecond(ZoneOffset.UTC)
        val midnightInstantSeconds = at_start_of_day(tzid, epochSeconds)
        if (midnightInstantSeconds == Long.MAX_VALUE) {
            throw RuntimeException("Unable to acquire the time of start of day at $date for zone $this")
        }
        Instant(midnightInstantSeconds, 0)
    }

    internal open fun LocalDateTime.atZone(preferred: ZoneOffset? = null): ZonedDateTime = memScoped {
        val epochSeconds = toEpochSecond(ZoneOffset.UTC)
        val offset = alloc<IntVar>()
        offset.value = preferred?.totalSeconds ?: INT_MAX
        val transitionDuration = offset_at_datetime(tzid, epochSeconds, offset.ptr)
        if (offset.value == INT_MAX) {
            throw RuntimeException("Unable to acquire the offset at ${this@atZone} for zone ${this@TimeZone}")
        }
        val dateTime = try {
            this@atZone.plusSeconds(transitionDuration)
        } catch (e: IllegalArgumentException) {
            throw DateTimeArithmeticException("Overflow whet correcting the date-time to not be in the transition gap", e)
        } catch (e: ArithmeticException) {
            throw RuntimeException("Anomalously long timezone transition gap reported", e)
        }
        ZonedDateTime(dateTime, this@TimeZone, ZoneOffset.ofSeconds(offset.value))
    }

    // org.threeten.bp.ZoneId#equals
    override fun equals(other: Any?): Boolean =
        this === other || other is TimeZone && this.id == other.id

    // org.threeten.bp.ZoneId#hashCode
    override fun hashCode(): Int = id.hashCode()

    // org.threeten.bp.ZoneId#toString
    override fun toString(): String = id

}

@ThreadLocal
private var zoneOffsetCache: MutableMap<Int, ZoneOffset> = mutableMapOf()

public actual class ZoneOffset internal constructor(actual val totalSeconds: Int, id: String) : TimeZone(TZID_INVALID, id) {

    companion object {
        // org.threeten.bp.ZoneOffset#UTC
        val UTC = ZoneOffset(0, "Z")

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
                else -> throw IllegalTimeZoneException("Invalid ID for ZoneOffset, invalid format: $offsetId")
            }
            val first: Char = offsetId[0]
            if (first != '+' && first != '-') {
                throw IllegalTimeZoneException(
                    "Invalid ID for ZoneOffset, plus/minus not found when expected: $offsetId")
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
                throw IllegalTimeZoneException("Zone offset hours not in valid range: value " + hours +
                    " is not in the range -18 to 18")
            }
            if (hours > 0) {
                if (minutes < 0 || seconds < 0) {
                    throw IllegalTimeZoneException("Zone offset minutes and seconds must be positive because hours is positive")
                }
            } else if (hours < 0) {
                if (minutes > 0 || seconds > 0) {
                    throw IllegalTimeZoneException("Zone offset minutes and seconds must be negative because hours is negative")
                }
            } else if (minutes > 0 && seconds < 0 || minutes < 0 && seconds > 0) {
                throw IllegalTimeZoneException("Zone offset minutes and seconds must have the same sign")
            }
            if (abs(minutes) > 59) {
                throw IllegalTimeZoneException("Zone offset minutes not in valid range: abs(value) " +
                    abs(minutes) + " is not in the range 0 to 59")
            }
            if (abs(seconds) > 59) {
                throw IllegalTimeZoneException("Zone offset seconds not in valid range: abs(value) " +
                    abs(seconds) + " is not in the range 0 to 59")
            }
            if (abs(hours) == 18 && (abs(minutes) > 0 || abs(seconds) > 0)) {
                throw IllegalTimeZoneException("Zone offset not in valid range: -18:00 to +18:00")
            }
        }

        // org.threeten.bp.ZoneOffset#ofHoursMinutesSeconds
        internal fun ofHoursMinutesSeconds(hours: Int, minutes: Int, seconds: Int): ZoneOffset {
            validate(hours, minutes, seconds)
            return if (hours == 0 && minutes == 0 && seconds == 0) UTC
            else ofSeconds(hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds)
        }

        // org.threeten.bp.ZoneOffset#ofTotalSeconds
        internal fun ofSeconds(seconds: Int): ZoneOffset =
            if (seconds % (15 * SECONDS_PER_MINUTE) == 0) {
                zoneOffsetCache[seconds] ?:
                    ZoneOffset(seconds, zoneIdByOffset(seconds)).also { zoneOffsetCache[seconds] = it }
            } else {
                ZoneOffset(seconds, zoneIdByOffset(seconds))
            }

        // org.threeten.bp.ZoneOffset#parseNumber
        private fun parseNumber(offsetId: CharSequence, pos: Int, precededByColon: Boolean): Int {
            if (precededByColon && offsetId[pos - 1] != ':') {
                throw IllegalTimeZoneException("Invalid ID for ZoneOffset, colon not found when expected: $offsetId")
            }
            val ch1 = offsetId[pos]
            val ch2 = offsetId[pos + 1]
            if (ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9') {
                throw IllegalTimeZoneException("Invalid ID for ZoneOffset, non numeric characters found: $offsetId")
            }
            return (ch1.toInt() - 48) * 10 + (ch2.toInt() - 48)
        }
    }

    internal override fun atStartOfDay(date: LocalDate): Instant =
        LocalDateTime(date, LocalTime.MIN).atZone(null).toInstant()

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
