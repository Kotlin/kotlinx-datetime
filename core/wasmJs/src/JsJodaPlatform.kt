/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE")

package kotlinx.datetime.internal

import kotlinx.datetime.internal.JSJoda.*
import kotlinx.datetime.jsTry

public actual open class JodaTimeTemporalUnit(open val value: TemporalUnit)
public actual open class JodaTimeTemporalAmount
public actual open class JodaTimeChronoLocalDate(override val value: ChronoLocalDate) : JodaTimeTemporal(value)
public actual open class JodaTimeTemporalAccessor(open val value: TemporalAccessor)
public actual open class JodaTimeChronoLocalDateTime(override val value: ChronoLocalDateTime) : JodaTimeTemporal(value) {
    actual open fun toInstant(offset: JodaTimeZoneOffset): JodaTimeInstant = JodaTimeInstant(value.toInstant(offset.value))
}
public actual open class JodaTimeChronoZonedDateTime(override val value: ChronoZonedDateTime) : JodaTimeTemporal(value) {
    actual fun toInstant(): JodaTimeInstant = JodaTimeInstant(value.toInstant())
}
public actual open class JodaTimeTemporal(override val value: Temporal) : JodaTimeTemporalAccessor(value) {
    actual open fun until(endTemporal: JodaTimeTemporal, unit: JodaTimeTemporalUnit): Double =
        /**
         * * Can throw for [JodaTimeZonedDateTime] values if the offsets are different.
         * * Can throw for values with local time components if the difference between them is a large number of days
         *   and the requested time unit is time-based.
         *
         * The caller must ensure this doesn't happen.
         */
        value.until(endTemporal.value, unit.value)
}

public actual open class JodaTimeChronoUnit(override val value: ChronoUnit) : JodaTimeTemporalUnit(value) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeChronoUnit && value === other.value
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()

    actual companion object {
        actual var NANOS: JodaTimeChronoUnit = JodaTimeChronoUnit(ChronoUnit.NANOS)
        actual var DAYS: JodaTimeChronoUnit = JodaTimeChronoUnit(ChronoUnit.DAYS)
        actual var MONTHS: JodaTimeChronoUnit = JodaTimeChronoUnit(ChronoUnit.MONTHS)
        actual var YEARS: JodaTimeChronoUnit = JodaTimeChronoUnit(ChronoUnit.YEARS)
    }
}

public actual open class JodaTimeClock(val value: Clock)  {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeClock && (value === other.value || value.equals(other.value))
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun instant(): JodaTimeInstant = JodaTimeInstant(value.instant())

    actual companion object {
        actual fun systemUTC(): JodaTimeClock = JodaTimeClock(Clock.systemUTC())
    }
}

public actual open class JodaTimeDuration(val value: Duration) : JodaTimeTemporalAmount() {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeDuration && (value === other.value || value.equals(other.value))
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun nano(): Double = value.nano()
    actual fun seconds(): Double = value.seconds()

    actual companion object {
        actual fun between(startInclusive: JodaTimeTemporal, endExclusive: JodaTimeTemporal): JodaTimeDuration =
            JodaTimeDuration(Duration.between(startInclusive.value, endExclusive.value))
    }
}

public actual open class JodaTimeInstant(override val value: Instant) : JodaTimeTemporal(value) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeInstant && (value === other.value || value.equals(other.value))
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun atZone(zone: JodaTimeZoneId): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(jsTry { value.atZone(zone.value) })
    actual fun compareTo(otherInstant: JodaTimeInstant): Int = value.compareTo(otherInstant.value)
    actual fun epochSecond(): Double = value.epochSecond()
    actual fun nano(): Double = value.nano()

    actual companion object {
        actual var MIN: JodaTimeInstant = JodaTimeInstant(Instant.MIN)
        actual var MAX: JodaTimeInstant = JodaTimeInstant(Instant.MAX)
        actual fun ofEpochSecond(epochSecond: Double, nanoAdjustment: Int): JodaTimeInstant =
            JodaTimeInstant(jsTry { Instant.ofEpochSecond(epochSecond, nanoAdjustment) })
    }
}

public actual open class JodaTimeLocalDate(override val value: LocalDate) : JodaTimeChronoLocalDate(value) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeLocalDate && (value === other.value || value.equals(other.value))
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun atStartOfDay(zone: JodaTimeZoneId): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(value.atStartOfDay(zone.value))
    actual fun compareTo(other: JodaTimeLocalDate): Int = value.compareTo(other.value)
    actual fun dayOfMonth(): Int = value.dayOfMonth()
    actual fun dayOfWeek(): JodaTimeDayOfWeek = JodaTimeDayOfWeek(value.dayOfWeek())
    actual fun dayOfYear(): Int = value.dayOfYear()
    actual fun month(): JodaTimeMonth = JodaTimeMonth(value.month())
    actual fun monthValue(): Int = value.monthValue()
    actual fun plusDays(daysToAdd: Int): JodaTimeLocalDate =
        JodaTimeLocalDate(jsTry { value.plusDays(daysToAdd) })
    actual fun plusMonths(monthsToAdd: Int): JodaTimeLocalDate =
        JodaTimeLocalDate(jsTry { value.plusMonths(monthsToAdd) })
    actual fun toEpochDay(): Double = value.toEpochDay()
    actual fun year(): Int = value.year()

    actual companion object {
        actual var MIN: JodaTimeLocalDate = JodaTimeLocalDate(LocalDate.MIN)
        actual var MAX: JodaTimeLocalDate = JodaTimeLocalDate(LocalDate.MAX)
        actual fun of(year: Int, month: Int, dayOfMonth: Int): JodaTimeLocalDate =
            JodaTimeLocalDate(jsTry { LocalDate.of(year, month, dayOfMonth) })
        actual fun ofEpochDay(epochDay: Int):  JodaTimeLocalDate =
            JodaTimeLocalDate(jsTry { LocalDate.ofEpochDay(epochDay) })
        actual fun parse(text: String): JodaTimeLocalDate =
            JodaTimeLocalDate(jsTry { LocalDate.parse(text) })
    }
}

public actual open class JodaTimeLocalDateTime(override val value: LocalDateTime) : JodaTimeChronoLocalDateTime(value) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeLocalDateTime && (value === other.value || value.equals(other.value))
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()

    actual fun atZone(zone: JodaTimeZoneId): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(value.atZone(zone.value))
    actual fun compareTo(other: JodaTimeLocalDateTime): Int = value.compareTo(other.value)
    actual fun dayOfMonth(): Int = value.dayOfMonth()
    actual fun dayOfWeek(): JodaTimeDayOfWeek = JodaTimeDayOfWeek(value.dayOfWeek())
    actual fun dayOfYear(): Int = value.dayOfYear()
    actual fun hour(): Int = value.hour()
    actual fun minute(): Int = value.minute()
    actual fun month(): JodaTimeMonth = JodaTimeMonth(value.month())
    actual fun monthValue(): Int = value.monthValue()
    actual fun nano(): Double = value.nano()
    actual fun second(): Int = value.second()
    actual fun toLocalDate(): JodaTimeLocalDate = JodaTimeLocalDate(value.toLocalDate())
    actual fun toLocalTime(): JodaTimeLocalTime = JodaTimeLocalTime(value.toLocalTime())
    actual fun year(): Int = value.year()

    actual companion object {
        actual var MIN: JodaTimeLocalDateTime = JodaTimeLocalDateTime(LocalDateTime.MIN)
        actual var MAX: JodaTimeLocalDateTime = JodaTimeLocalDateTime(LocalDateTime.MAX)
        actual fun of(date: JodaTimeLocalDate, time: JodaTimeLocalTime): JodaTimeLocalDateTime =
            JodaTimeLocalDateTime(jsTry { LocalDateTime.of(date.value, time.value) })
        actual fun of(year: Int, month: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanoSecond: Int): JodaTimeLocalDateTime =
            JodaTimeLocalDateTime(jsTry { LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoSecond) })
        actual fun ofInstant(instant: JodaTimeInstant, zoneId: JodaTimeZoneId): JodaTimeLocalDateTime =
            JodaTimeLocalDateTime(jsTry { LocalDateTime.ofInstant(instant.value, zoneId.value) })
        actual fun parse(text: String): JodaTimeLocalDateTime =
            JodaTimeLocalDateTime(jsTry { LocalDateTime.parse(text) })
    }
}

public actual open class JodaTimeLocalTime(override val value: LocalTime) : JodaTimeTemporal(value) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeLocalTime && (value === other.value || value.equals(other.value))
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun compareTo(other: JodaTimeLocalTime): Int = value.compareTo(other.value)
    actual fun hour(): Int = value.hour()
    actual fun minute(): Int = value.minute()
    actual fun nano(): Double = value.nano()
    actual fun second(): Int = value.second()
    actual fun toNanoOfDay(): Double = value.toNanoOfDay()
    actual fun toSecondOfDay(): Int = value.toSecondOfDay()

    actual companion object {
        actual var MIN: JodaTimeLocalTime = JodaTimeLocalTime(LocalTime.MIN)
        actual var MAX: JodaTimeLocalTime = JodaTimeLocalTime(LocalTime.MAX)
        actual fun of(hour: Int, minute: Int, second: Int, nanoOfSecond: Int): JodaTimeLocalTime =
            JodaTimeLocalTime(jsTry { LocalTime.of(hour, minute, second, nanoOfSecond) })
        actual fun ofNanoOfDay(nanoOfDay: Double): JodaTimeLocalTime =
            JodaTimeLocalTime(jsTry { LocalTime.ofNanoOfDay(nanoOfDay) })
        actual fun ofSecondOfDay(secondOfDay: Int, nanoOfSecond: Int): JodaTimeLocalTime =
            JodaTimeLocalTime(jsTry { LocalTime.ofSecondOfDay(secondOfDay, nanoOfSecond) })
        actual fun parse(text: String): JodaTimeLocalTime =
            JodaTimeLocalTime(jsTry { LocalTime.parse(text) })
    }
}

public actual open class JodaTimeOffsetDateTime(override val value: OffsetDateTime) : JodaTimeTemporal(value) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeOffsetDateTime && (value === other.value || value.equals(other.value))
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun toInstant(): JodaTimeInstant = JodaTimeInstant(value.toInstant())

    actual companion object {
        actual fun ofInstant(instant: JodaTimeInstant, zone: JodaTimeZoneId): JodaTimeOffsetDateTime =
            JodaTimeOffsetDateTime(OffsetDateTime.ofInstant(instant.value, zone.value))
        actual fun parse(text: String): JodaTimeOffsetDateTime =
            JodaTimeOffsetDateTime(jsTry { OffsetDateTime.parse(text) })
    }
}

public actual open class JodaTimeZonedDateTime(override val value: ZonedDateTime) : JodaTimeChronoZonedDateTime(value) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeZonedDateTime && (value === other.value || value.equals(other.value))
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun plusDays(days: Int): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(jsTry { value.plusDays(days) })
    actual fun plusDays(days: Double): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(jsTry { value.plusDays(days) })
    actual fun plusHours(hours: Int): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(jsTry { value.plusHours(hours) })
    actual fun plusMinutes(minutes: Int): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(jsTry { value.plusMinutes(minutes) })
    actual fun plusMonths(months: Int): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(jsTry { value.plusMonths(months) })
    actual fun plusMonths(months: Double): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(jsTry { value.plusMonths(months) })
    actual fun plusNanos(nanos: Double): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(jsTry { value.plusNanos(nanos) })
    actual fun plusSeconds(seconds: Int): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(jsTry { value.plusSeconds(seconds) })
}

internal actual fun JodaTimeZoneId.toZoneOffset(): JodaTimeZoneOffset? =
    (value as? ZoneOffset)?.let(::JodaTimeZoneOffset)

public actual open class JodaTimeZoneId(open val value: ZoneId)  {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeZoneId && (value === other.value || value.equals(other.value))
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun id(): String = value.id()
    actual fun normalized(): JodaTimeZoneId = JodaTimeZoneId(value.normalized())
    actual fun rules(): JodaTimeZoneRules = JodaTimeZoneRules(value.rules())

    actual companion object {
        actual fun systemDefault(): JodaTimeZoneId = JodaTimeZoneId(ZoneId.systemDefault())
        actual fun of(zoneId: String): JodaTimeZoneId = JodaTimeZoneId(jsTry { ZoneId.of(zoneId) })
    }
}

public actual open class JodaTimeZoneOffset(override val value: ZoneOffset) : JodaTimeZoneId(value) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeZoneOffset && (value === other.value || value.equals(other.value))
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun totalSeconds(): Int = value.totalSeconds()

    actual companion object {
        actual var UTC: JodaTimeZoneOffset = JodaTimeZoneOffset(ZoneOffset.UTC)
        actual fun of(offsetId: String): JodaTimeZoneOffset =
            JodaTimeZoneOffset(jsTry { ZoneOffset.of(offsetId) })
        actual fun ofHoursMinutesSeconds(hours: Int, minutes: Int, seconds: Int): JodaTimeZoneOffset =
            JodaTimeZoneOffset(jsTry { ZoneOffset.ofHoursMinutesSeconds(hours, minutes, seconds) })
        actual fun ofTotalSeconds(totalSeconds: Int): JodaTimeZoneOffset =
            JodaTimeZoneOffset(jsTry { ZoneOffset.ofTotalSeconds(totalSeconds) })
    }
}

public actual open class JodaTimeDayOfWeek(private val value: DayOfWeek) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeDayOfWeek && (value === other.value || value.equals(other.value))
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun value(): Int = value.value()
}

public actual open class JodaTimeMonth(private val value: Month) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeMonth && (value === other.value || value.equals(other.value))
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun value(): Int = value.value()
}

public actual open class JodaTimeZoneRules(private val value: ZoneRules)  {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeZoneRules && value === other.value
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun isFixedOffset(): Boolean = value.isFixedOffset()
    actual fun offsetOfInstant(instant: JodaTimeInstant): JodaTimeZoneOffset =
        JodaTimeZoneOffset(jsTry { value.offsetOfInstant(instant.value) })
}
