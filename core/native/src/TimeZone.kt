/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlin.math.abs
import kotlin.native.concurrent.*
import kotlinx.serialization.Serializable

@Serializable(with = TimeZoneSerializer::class)
public actual open class TimeZone internal constructor(internal val value: TimeZoneImpl) {

    actual companion object {

        actual fun currentSystemDefault(): TimeZone = PlatformTimeZoneImpl.currentSystemDefault().let(::TimeZone)

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
                return TimeZone(ZoneOffsetImpl(0, zoneId))
            }
            if (zoneId.startsWith("UTC+") || zoneId.startsWith("GMT+") ||
                zoneId.startsWith("UTC-") || zoneId.startsWith("GMT-")) {
                val offset = ZoneOffset.of(zoneId.substring(3))
                return (if (offset.totalSeconds == 0) ZoneOffsetImpl(0, zoneId.substring(0, 3))
                else ZoneOffsetImpl(offset.totalSeconds, zoneId.substring(0, 3) + offset.id)).let(::TimeZone)
            }
            if (zoneId.startsWith("UT+") || zoneId.startsWith("UT-")) {
                val offset = ZoneOffset.of(zoneId.substring(2))
                return (if (offset.totalSeconds == 0) ZoneOffsetImpl(0, "UT")
                else ZoneOffsetImpl(offset.totalSeconds, "UT" + offset.id)).let(::TimeZone)
            }
            return TimeZone(PlatformTimeZoneImpl.of(zoneId))
        }

        actual val availableZoneIds: Set<String>
            get() = PlatformTimeZoneImpl.availableZoneIds
    }

    actual val id
        get() = value.id

    actual fun Instant.toLocalDateTime(): LocalDateTime = try {
        toZonedLocalDateTime(this@TimeZone).dateTime
    } catch (e: IllegalArgumentException) {
        throw DateTimeArithmeticException("Instant ${this@toLocalDateTime} is not representable as LocalDateTime", e)
    }

    actual fun LocalDateTime.toInstant(): Instant = atZone().toInstant()

    internal open fun atStartOfDay(date: LocalDate): Instant = value.atStartOfDay(date)

    internal open fun LocalDateTime.atZone(preferred: ZoneOffsetImpl? = null): ZonedDateTime =
        with(value) { atZone(preferred) }

    override fun equals(other: Any?): Boolean =
        this === other || other is TimeZone && this.value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()
}

@ThreadLocal
private var zoneOffsetCache: MutableMap<Int, ZoneOffset> = mutableMapOf()

@Serializable(with = ZoneOffsetSerializer::class)
public actual class ZoneOffset internal constructor(internal val offset: ZoneOffsetImpl) : TimeZone(offset) {

    actual val totalSeconds get() = offset.totalSeconds

    companion object {
        val UTC = ZoneOffset(ZoneOffsetImpl.UTC)

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
                    ZoneOffset(ZoneOffsetImpl(seconds, zoneIdByOffset(seconds))).also { zoneOffsetCache[seconds] = it }
            } else {
                ZoneOffset(ZoneOffsetImpl(seconds, zoneIdByOffset(seconds)))
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
}

public actual fun TimeZone.offsetAt(instant: Instant): ZoneOffset =
        value.offsetAt(instant).let(::ZoneOffset)

public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
        with(timeZone) { toLocalDateTime() }

public actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
        with(timeZone) { toInstant() }

public actual fun LocalDate.atStartOfDayIn(timeZone: TimeZone): Instant =
        timeZone.atStartOfDay(this)
