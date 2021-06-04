/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.datetime.serializers.TimeZoneSerializer
import kotlinx.datetime.serializers.FixedOffsetTimeZoneSerializer
import kotlin.math.abs
import kotlin.native.concurrent.*
import kotlinx.serialization.Serializable

@Serializable(with = TimeZoneSerializer::class)
public actual open class TimeZone internal constructor(internal val value: TimeZoneImpl) {

    public actual companion object {

        public actual fun currentSystemDefault(): TimeZone = PlatformTimeZoneImpl.currentSystemDefault().let(::TimeZone)

        public actual val UTC: FixedOffsetTimeZone = UtcOffset.ZERO.asTimeZone()

        // org.threeten.bp.ZoneId#of(java.lang.String)
        public actual fun of(zoneId: String): TimeZone {
            // TODO: normalize aliases?
            if (zoneId == "Z") {
                return UTC
            }
            if (zoneId.length == 1) {
                throw IllegalTimeZoneException("Invalid zone ID: $zoneId")
            }
            if (zoneId.startsWith("+") || zoneId.startsWith("-")) {
                return UtcOffset.parse(zoneId).asTimeZone()
            }
            if (zoneId == "UTC" || zoneId == "GMT" || zoneId == "UT") {
                return FixedOffsetTimeZone(UtcOffset(0), zoneId)
            }
            if (zoneId.startsWith("UTC+") || zoneId.startsWith("GMT+") ||
                zoneId.startsWith("UTC-") || zoneId.startsWith("GMT-")) {
                val prefix = zoneId.take(3)
                val offset = UtcOffset.parse(zoneId.substring(3))
                return when (offset.totalSeconds) {
                    0 -> FixedOffsetTimeZone(offset, prefix)
                    else -> FixedOffsetTimeZone(offset, "$prefix$offset")
                }
            }
            if (zoneId.startsWith("UT+") || zoneId.startsWith("UT-")) {
                val offset = UtcOffset.parse(zoneId.substring(2))
                return when (offset.totalSeconds) {
                    0 -> FixedOffsetTimeZone(offset, "UT")
                    else -> FixedOffsetTimeZone(offset, "UT$offset")
                }
            }
            return TimeZone(PlatformTimeZoneImpl.of(zoneId))
        }

        public actual val availableZoneIds: Set<String>
            get() = PlatformTimeZoneImpl.availableZoneIds
    }

    public actual val id: String
        get() = value.id

    public actual fun Instant.toLocalDateTime(): LocalDateTime = instantToLocalDateTime(this)
    public actual fun LocalDateTime.toInstant(): Instant = localDateTimeToInstant(this)

    internal open fun atStartOfDay(date: LocalDate): Instant = value.atStartOfDay(date)

    internal open fun instantToLocalDateTime(instant: Instant): LocalDateTime = try {
        instant.toLocalDateTimeImpl(offsetAt(instant))
    } catch (e: IllegalArgumentException) {
        throw DateTimeArithmeticException("Instant $instant is not representable as LocalDateTime.", e)
    }

    internal open fun localDateTimeToInstant(dateTime: LocalDateTime): Instant =
        atZone(dateTime).toInstant()

    internal open fun atZone(dateTime: LocalDateTime, preferred: UtcOffset? = null): ZonedDateTime =
        value.atZone(dateTime, preferred)

    override fun equals(other: Any?): Boolean =
        this === other || other is TimeZone && this.value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()
}

@ThreadLocal
private var utcOffsetCache: MutableMap<Int, UtcOffset> = mutableMapOf()

@Serializable(with = FixedOffsetTimeZoneSerializer::class)
public actual class FixedOffsetTimeZone internal constructor(public actual val utcOffset: UtcOffset, id: String) : TimeZone(ZoneOffsetImpl(utcOffset, id)) {

    public actual constructor(utcOffset: UtcOffset) : this(utcOffset, utcOffset.toString())

    @Deprecated("Use utcOffset.totalSeconds", ReplaceWith("utcOffset.totalSeconds"))
    public actual val totalSeconds: Int get() = utcOffset.totalSeconds

    override fun instantToLocalDateTime(instant: Instant): LocalDateTime = instant.toLocalDateTime(utcOffset)
    override fun localDateTimeToInstant(dateTime: LocalDateTime): Instant = dateTime.toInstant(utcOffset)
}


public actual class UtcOffset internal constructor(public actual val totalSeconds: Int) {
    private val id: String = zoneIdByOffset(totalSeconds)

    override fun hashCode(): Int = totalSeconds
    override fun equals(other: Any?): Boolean = other is UtcOffset && this.totalSeconds == other.totalSeconds
    override fun toString(): String = id

    public actual companion object {

        internal val ZERO: UtcOffset = UtcOffset(0)

        public actual fun parse(offsetString: String): UtcOffset {
            if (offsetString == "Z") {
                return ZERO
            }

            // parse - +h, +hh, +hhmm, +hh:mm, +hhmmss, +hh:mm:ss
            val hours: Int
            val minutes: Int
            val seconds: Int
            when (offsetString.length) {
                2 -> return parse(offsetString[0].toString() + "0" + offsetString[1])
                3 -> {
                    hours = parseNumber(offsetString, 1, false)
                    minutes = 0
                    seconds = 0
                }
                5 -> {
                    hours = parseNumber(offsetString, 1, false)
                    minutes = parseNumber(offsetString, 3, false)
                    seconds = 0
                }
                6 -> {
                    hours = parseNumber(offsetString, 1, false)
                    minutes = parseNumber(offsetString, 4, true)
                    seconds = 0
                }
                7 -> {
                    hours = parseNumber(offsetString, 1, false)
                    minutes = parseNumber(offsetString, 3, false)
                    seconds = parseNumber(offsetString, 5, false)
                }
                9 -> {
                    hours = parseNumber(offsetString, 1, false)
                    minutes = parseNumber(offsetString, 4, true)
                    seconds = parseNumber(offsetString, 7, true)
                }
                else -> throw IllegalTimeZoneException("Invalid ID for UtcOffset, invalid format: $offsetString")
            }
            val first: Char = offsetString[0]
            if (first != '+' && first != '-') {
                throw IllegalTimeZoneException(
                    "Invalid ID for UtcOffset, plus/minus not found when expected: $offsetString")
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
        internal fun ofHoursMinutesSeconds(hours: Int, minutes: Int, seconds: Int): UtcOffset {
            validate(hours, minutes, seconds)
            return if (hours == 0 && minutes == 0 && seconds == 0) ZERO
            else ofSeconds(hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds)
        }

        // org.threeten.bp.ZoneOffset#ofTotalSeconds
        internal fun ofSeconds(seconds: Int): UtcOffset =
            if (seconds % (15 * SECONDS_PER_MINUTE) == 0) {
                utcOffsetCache[seconds] ?: UtcOffset(seconds).also { utcOffsetCache[seconds] = it }
            } else {
                UtcOffset(seconds)
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
            return (ch1 - '0') * 10 + (ch2 - '0')
        }
    }
}

public actual fun TimeZone.offsetAt(instant: Instant): UtcOffset =
        value.offsetAt(instant)

public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
    timeZone.instantToLocalDateTime(this)

public actual fun Instant.toLocalDateTime(utcOffset: UtcOffset): LocalDateTime = try {
    toLocalDateTimeImpl(utcOffset)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Instant ${this@toLocalDateTime} is not representable as LocalDateTime", e)
}

internal fun Instant.toLocalDateTimeImpl(offset: UtcOffset): LocalDateTime {
    val localSecond: Long = epochSeconds + offset.totalSeconds // overflow caught later
    val localEpochDay = floorDiv(localSecond, SECONDS_PER_DAY.toLong()).toInt()
    val secsOfDay = floorMod(localSecond, SECONDS_PER_DAY.toLong()).toInt()
    val date: LocalDate = LocalDate.ofEpochDay(localEpochDay) // may throw
    val time: LocalTime = LocalTime.ofSecondOfDay(secsOfDay, nanosecondsOfSecond)
    return LocalDateTime(date, time)
}

public actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
    timeZone.localDateTimeToInstant(this)

public actual fun LocalDateTime.toInstant(utcOffset: UtcOffset): Instant =
    Instant(this.toEpochSecond(utcOffset), this.nanosecond)

public actual fun LocalDate.atStartOfDayIn(timeZone: TimeZone): Instant =
    timeZone.atStartOfDay(this)
