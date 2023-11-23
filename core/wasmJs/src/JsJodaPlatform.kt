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
        value.until(endTemporal.value, unit.value)
}

public actual open class JodaTimeChronoUnit(override val value: ChronoUnit) : JodaTimeTemporalUnit(value) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeChronoUnit && value.equals(other.value)
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
    actual override fun equals(other: Any?): Boolean = other is JodaTimeClock && value.equals(other.value)
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun instant(): JodaTimeInstant = JodaTimeInstant(value.instant())

    actual companion object {
        actual fun systemUTC(): JodaTimeClock = JodaTimeClock(Clock.systemUTC())
    }
}

public actual open class JodaTimeDuration(val value: Duration) : JodaTimeTemporalAmount() {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeDuration && value.equals(other.value)
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
    actual override fun equals(other: Any?): Boolean = other is JodaTimeInstant && value.equals(other.value)
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun atZone(zone: JodaTimeZoneId): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(value.atZone(zone.value))
    actual fun compareTo(otherInstant: JodaTimeInstant): Int = value.compareTo(otherInstant.value)
    actual fun epochSecond(): Double = value.epochSecond()
    actual fun nano(): Double = value.nano()

    actual companion object {
        actual var MIN: JodaTimeInstant = JodaTimeInstant(Instant.MIN)
        actual var MAX: JodaTimeInstant = JodaTimeInstant(Instant.MAX)
        actual fun ofEpochSecond(epochSecond: Double, nanoAdjustment: Int): JodaTimeInstant =
            jsTry { JodaTimeInstant(Instant.ofEpochSecond(epochSecond, nanoAdjustment)) }
    }
}

public actual open class JodaTimeLocalDate(override val value: LocalDate) : JodaTimeChronoLocalDate(value) {
    actual fun atStartOfDay(zone: JodaTimeZoneId): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(value.atStartOfDay(zone.value))
    actual fun compareTo(other: JodaTimeLocalDate): Int = value.compareTo(other.value)
    actual fun dayOfMonth(): Int = value.dayOfMonth()
    actual fun dayOfWeek(): JodaTimeDayOfWeek = JodaTimeDayOfWeek(value.dayOfWeek())
    actual fun dayOfYear(): Int = value.dayOfYear()
    actual override fun equals(other: Any?): Boolean = other is JodaTimeLocalDate && value.equals(other.value)
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun month(): JodaTimeMonth = JodaTimeMonth(value.month())
    actual fun monthValue(): Int = value.monthValue()
    actual fun plusDays(daysToAdd: Int): JodaTimeLocalDate =
        JodaTimeLocalDate(value.plusDays(daysToAdd))
    actual fun plusMonths(monthsToAdd: Int): JodaTimeLocalDate =
        JodaTimeLocalDate(value.plusMonths(monthsToAdd))
    actual fun toEpochDay(): Double = value.toEpochDay()
    actual fun year(): Int = value.year()

    actual companion object {
        actual var MIN: JodaTimeLocalDate = JodaTimeLocalDate(LocalDate.MIN)
        actual var MAX: JodaTimeLocalDate = JodaTimeLocalDate(LocalDate.MAX)
        actual fun of(year: Int, month: Int, dayOfMonth: Int): JodaTimeLocalDate =
            JodaTimeLocalDate(LocalDate.of(year, month, dayOfMonth))
        actual fun ofEpochDay(epochDay: Int):  JodaTimeLocalDate =
            JodaTimeLocalDate(LocalDate.ofEpochDay(epochDay))
        actual fun parse(text: String): JodaTimeLocalDate =
            JodaTimeLocalDate(LocalDate.parse(text))
    }
}

public actual open class JodaTimeLocalDateTime(override val value: LocalDateTime) : JodaTimeChronoLocalDateTime(value) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeLocalDateTime && value.equals(other.value)
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
            JodaTimeLocalDateTime(LocalDateTime.of(date.value, time.value))
        actual fun of(year: Int, month: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanoSecond: Int): JodaTimeLocalDateTime =
            JodaTimeLocalDateTime(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoSecond))
        actual fun ofInstant(instant: JodaTimeInstant, zoneId: JodaTimeZoneId): JodaTimeLocalDateTime =
            JodaTimeLocalDateTime(LocalDateTime.ofInstant(instant.value, zoneId.value))
        actual fun parse(text: String): JodaTimeLocalDateTime =
            JodaTimeLocalDateTime(LocalDateTime.parse(text))
    }
}

public actual open class JodaTimeLocalTime(override val value: LocalTime) : JodaTimeTemporal(value) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeLocalTime && value.equals(other.value)
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
            JodaTimeLocalTime(LocalTime.of(hour, minute, second, nanoOfSecond))
        actual fun ofNanoOfDay(nanoOfDay: Double): JodaTimeLocalTime =
            JodaTimeLocalTime(LocalTime.ofNanoOfDay(nanoOfDay))
        actual fun ofSecondOfDay(secondOfDay: Int, nanoOfSecond: Int): JodaTimeLocalTime =
            JodaTimeLocalTime(LocalTime.ofSecondOfDay(secondOfDay, nanoOfSecond))
        actual fun parse(text: String): JodaTimeLocalTime =
            JodaTimeLocalTime(LocalTime.parse(text))
    }
}

public actual open class JodaTimeOffsetDateTime(override val value: OffsetDateTime) : JodaTimeTemporal(value) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeOffsetDateTime && value.equals(other.value)
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun toInstant(): JodaTimeInstant = JodaTimeInstant(value.toInstant())

    actual companion object {
        actual fun ofInstant(instant: JodaTimeInstant, zone: JodaTimeZoneId): JodaTimeOffsetDateTime =
            JodaTimeOffsetDateTime(OffsetDateTime.ofInstant(instant.value, zone.value))
        actual fun parse(text: String): JodaTimeOffsetDateTime =
            JodaTimeOffsetDateTime(OffsetDateTime.parse(text))
    }
}

public actual open class JodaTimeZonedDateTime(override val value: ZonedDateTime) : JodaTimeChronoZonedDateTime(value) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeZonedDateTime && value.equals(other.value)
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun plusDays(days: Int): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(value.plusDays(days))
    actual fun plusDays(days: Double): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(value.plusDays(days))
    actual fun plusHours(hours: Int): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(value.plusHours(hours))
    actual fun plusMinutes(minutes: Int): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(value.plusMinutes(minutes))
    actual fun plusMonths(months: Int): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(value.plusMonths(months))
    actual fun plusMonths(months: Double): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(value.plusMonths(months))
    actual fun plusNanos(nanos: Double): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(value.plusNanos(nanos))
    actual fun plusSeconds(seconds: Int): JodaTimeZonedDateTime =
        JodaTimeZonedDateTime(value.plusSeconds(seconds))
}

internal actual fun JodaTimeZoneId.toZoneOffset(): JodaTimeZoneOffset? =
    (value as? ZoneOffset)?.let(::JodaTimeZoneOffset)

public actual open class JodaTimeZoneId(open val value: ZoneId)  {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeZoneId && value.equals(other.value)
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun id(): String = value.id()
    actual fun normalized(): JodaTimeZoneId = JodaTimeZoneId(value.normalized())
    actual fun rules(): JodaTimeZoneRules = JodaTimeZoneRules(value.rules())

    actual companion object {
        actual fun systemDefault(): JodaTimeZoneId = JodaTimeZoneId(ZoneId.systemDefault())
        actual fun of(zoneId: String): JodaTimeZoneId = JodaTimeZoneId(ZoneId.of(zoneId))
    }
}

public actual open class JodaTimeZoneOffset(override val value: ZoneOffset) : JodaTimeZoneId(value) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeZoneOffset && value.equals(other.value)
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun totalSeconds(): Int = value.totalSeconds()

    actual companion object {
        actual var UTC: JodaTimeZoneOffset = JodaTimeZoneOffset(ZoneOffset.UTC)
        actual fun of(offsetId: String): JodaTimeZoneOffset =
            JodaTimeZoneOffset(ZoneOffset.of(offsetId))
        actual fun ofHoursMinutesSeconds(hours: Int, minutes: Int, seconds: Int): JodaTimeZoneOffset =
            JodaTimeZoneOffset(ZoneOffset.ofHoursMinutesSeconds(hours, minutes, seconds))
        actual fun ofTotalSeconds(totalSeconds: Int): JodaTimeZoneOffset =
            JodaTimeZoneOffset(ZoneOffset.ofTotalSeconds(totalSeconds))
    }
}

public actual open class JodaTimeDayOfWeek(private val value: DayOfWeek) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeDayOfWeek && value.equals(other.value)
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun value(): Int = value.value()
}

public actual open class JodaTimeMonth(private val value: Month) {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeMonth && value.equals(other.value)
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun value(): Int = value.value()
}

public actual open class JodaTimeZoneRules(private val value: ZoneRules)  {
    actual override fun equals(other: Any?): Boolean = other is JodaTimeZoneRules && value.equals(other.value)
    actual override fun hashCode(): Int = value.hashCode()
    actual override fun toString(): String = value.toString()
    actual fun isFixedOffset(): Boolean = value.isFixedOffset()
    actual fun offsetOfInstant(instant: JodaTimeInstant): JodaTimeZoneOffset =
        JodaTimeZoneOffset(value.offsetOfInstant(instant.value))
}