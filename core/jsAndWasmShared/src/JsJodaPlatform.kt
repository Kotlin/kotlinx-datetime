/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE")

package kotlinx.datetime.internal

public expect open class JodaTimeTemporalUnit
public expect open class JodaTimeTemporalAmount
public expect open class JodaTimeChronoLocalDate : JodaTimeTemporal
public expect open class JodaTimeTemporalAccessor
public expect open class JodaTimeChronoLocalDateTime : JodaTimeTemporal {
    open fun toInstant(offset: JodaTimeZoneOffset): JodaTimeInstant
}
public expect open class JodaTimeChronoZonedDateTime : JodaTimeTemporal {
    fun toInstant(): JodaTimeInstant
}
public expect open class JodaTimeTemporal : JodaTimeTemporalAccessor {
    open fun until(endTemporal: JodaTimeTemporal, unit: JodaTimeTemporalUnit): Double
}


public expect open class JodaTimeChronoUnit : JodaTimeTemporalUnit {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String

    companion object {
        var NANOS: JodaTimeChronoUnit
        var DAYS: JodaTimeChronoUnit
        var MONTHS: JodaTimeChronoUnit
        var YEARS: JodaTimeChronoUnit
    }
}

public expect open class JodaTimeClock {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
    fun instant(): JodaTimeInstant

    companion object {
        fun systemUTC(): JodaTimeClock
    }
}

public expect open class JodaTimeDuration : JodaTimeTemporalAmount {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
    fun nano(): Double
    fun seconds(): Double

    companion object {
        fun between(startInclusive: JodaTimeTemporal, endExclusive: JodaTimeTemporal): JodaTimeDuration
    }
}

public expect open class JodaTimeInstant : JodaTimeTemporal {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
    fun atZone(zone: JodaTimeZoneId): JodaTimeZonedDateTime
    fun compareTo(otherInstant: JodaTimeInstant): Int
    fun epochSecond(): Double
    fun nano(): Double

    companion object {
        var MIN: JodaTimeInstant
        var MAX: JodaTimeInstant
        fun ofEpochSecond(epochSecond: Double, nanoAdjustment: Int): JodaTimeInstant
    }
}

public expect open class JodaTimeLocalDate : JodaTimeChronoLocalDate {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
    fun atStartOfDay(zone: JodaTimeZoneId): JodaTimeZonedDateTime
    fun compareTo(other: JodaTimeLocalDate): Int
    fun dayOfMonth(): Int
    fun dayOfWeek(): JodaTimeDayOfWeek
    fun dayOfYear(): Int
    fun month(): JodaTimeMonth
    fun monthValue(): Int
    fun plusDays(daysToAdd: Int): JodaTimeLocalDate
    fun plusMonths(monthsToAdd: Int): JodaTimeLocalDate
    fun toEpochDay(): Double
    fun year(): Int

    companion object {
        var MIN: JodaTimeLocalDate
        var MAX: JodaTimeLocalDate
        fun of(year: Int, month: Int, dayOfMonth: Int):  JodaTimeLocalDate
        fun ofEpochDay(epochDay: Int):  JodaTimeLocalDate
        fun parse(text: String): JodaTimeLocalDate
    }
}

public expect open class JodaTimeLocalDateTime : JodaTimeChronoLocalDateTime {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
    fun atZone(zone: JodaTimeZoneId): JodaTimeZonedDateTime
    fun compareTo(other: JodaTimeLocalDateTime): Int
    fun dayOfMonth(): Int
    fun dayOfWeek(): JodaTimeDayOfWeek
    fun dayOfYear(): Int
    fun hour(): Int
    fun minute(): Int
    fun month(): JodaTimeMonth
    fun monthValue(): Int
    fun nano(): Double
    fun second(): Int
    fun toLocalDate(): JodaTimeLocalDate
    fun toLocalTime(): JodaTimeLocalTime
    fun year(): Int

    companion object {
        var MIN: JodaTimeLocalDateTime
        var MAX: JodaTimeLocalDateTime
        fun of(date: JodaTimeLocalDate, time: JodaTimeLocalTime): JodaTimeLocalDateTime
        fun of(year: Int, month: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanoSecond: Int): JodaTimeLocalDateTime
        fun ofInstant(instant: JodaTimeInstant, zoneId: JodaTimeZoneId): JodaTimeLocalDateTime
        fun parse(text: String): JodaTimeLocalDateTime
    }
}

public expect open class JodaTimeLocalTime : JodaTimeTemporal {
    fun compareTo(other: JodaTimeLocalTime): Int
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
    fun hour(): Int
    fun minute(): Int
    fun nano(): Double
    fun second(): Int
    fun toNanoOfDay(): Double
    fun toSecondOfDay(): Int

    companion object {
        var MIN: JodaTimeLocalTime
        var MAX: JodaTimeLocalTime
        fun of(hour: Int, minute: Int, second: Int, nanoOfSecond: Int): JodaTimeLocalTime
        fun ofNanoOfDay(nanoOfDay: Double): JodaTimeLocalTime
        fun ofSecondOfDay(secondOfDay: Int, nanoOfSecond: Int): JodaTimeLocalTime
        fun parse(text: String): JodaTimeLocalTime
    }
}

public expect open class JodaTimeOffsetDateTime : JodaTimeTemporal {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
    fun toInstant(): JodaTimeInstant

    companion object {
        fun ofInstant(instant: JodaTimeInstant, zone: JodaTimeZoneId): JodaTimeOffsetDateTime
        fun parse(text: String): JodaTimeOffsetDateTime
    }
}

public expect open class JodaTimeZonedDateTime : JodaTimeChronoZonedDateTime {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
    fun plusDays(days: Int): JodaTimeZonedDateTime
    fun plusDays(days: Double): JodaTimeZonedDateTime
    fun plusHours(hours: Int): JodaTimeZonedDateTime
    fun plusMinutes(minutes: Int): JodaTimeZonedDateTime
    fun plusMonths(months: Int): JodaTimeZonedDateTime
    fun plusMonths(months: Double): JodaTimeZonedDateTime
    fun plusNanos(nanos: Double): JodaTimeZonedDateTime
    fun plusSeconds(seconds: Int): JodaTimeZonedDateTime
}

internal expect fun JodaTimeZoneId.toZoneOffset(): JodaTimeZoneOffset?

public expect open class JodaTimeZoneId {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
    fun id(): String
    fun normalized(): JodaTimeZoneId
    fun rules(): JodaTimeZoneRules

    companion object {
        fun systemDefault(): JodaTimeZoneId
        fun of(zoneId: String): JodaTimeZoneId
    }
}

public expect open class JodaTimeZoneOffset : JodaTimeZoneId {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
    fun totalSeconds(): Int

    companion object {
        var UTC: JodaTimeZoneOffset
        fun of(offsetId: String): JodaTimeZoneOffset
        fun ofHoursMinutesSeconds(hours: Int, minutes: Int, seconds: Int): JodaTimeZoneOffset
        fun ofTotalSeconds(totalSeconds: Int): JodaTimeZoneOffset
    }
}

public expect open class JodaTimeDayOfWeek {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
    fun value(): Int
}

public expect open class JodaTimeMonth {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
    fun value(): Int
}

public expect open class JodaTimeZoneRules {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
    fun isFixedOffset(): Boolean
    fun offsetOfInstant(instant: JodaTimeInstant): JodaTimeZoneOffset
}