/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

/**
 * Converts this [kotlinx.datetime.Instant][Instant] value to a [java.time.Instant][java.time.Instant] value.
 */
public fun Instant.toJavaInstant(): java.time.Instant = this.value

/**
 * Converts this [java.time.Instant][java.time.Instant] value to a [kotlinx.datetime.Instant][Instant] value.
 */
public fun java.time.Instant.toKotlinInstant(): Instant = Instant(this)


/**
 * Converts this [kotlinx.datetime.LocalDateTime][LocalDateTime] value to a [java.time.LocalDateTime][java.time.LocalDateTime] value.
 */
public fun LocalDateTime.toJavaLocalDateTime(): java.time.LocalDateTime = this.value

/**
 * Converts this [java.time.LocalDateTime][java.time.LocalDateTime] value to a [kotlinx.datetime.LocalDateTime][LocalDateTime] value.
 */
public fun java.time.LocalDateTime.toKotlinLocalDateTime(): LocalDateTime = LocalDateTime(this)

/**
 * Converts this [kotlinx.datetime.LocalDateTime][LocalTime] value to a [java.time.LocalTime][java.time.LocalTime] value.
 */
public fun LocalTime.toJavaLocalTime(): java.time.LocalTime = this.value

/**
 * Converts this [java.time.LocalTime][java.time.LocalTime] value to a [kotlinx.datetime.LocalTime][LocalTime] value.
 */
public fun java.time.LocalTime.toKotlinLocalTime(): LocalTime = LocalTime(this)


/**
 * Converts this [kotlinx.datetime.LocalDate][LocalDate] value to a [java.time.LocalDate][java.time.LocalDate] value.
 */
public fun LocalDate.toJavaLocalDate(): java.time.LocalDate = this.value

/**
 * Converts this [java.time.LocalDate][java.time.LocalDate] value to a [kotlinx.datetime.LocalDate][LocalDate] value.
 */
public fun java.time.LocalDate.toKotlinLocalDate(): LocalDate = LocalDate(this)


/**
 * Converts this [kotlinx.datetime.DatePeriod][DatePeriod] value to a [java.time.Period][java.time.Period] value.
 */
public fun DatePeriod.toJavaPeriod(): java.time.Period = java.time.Period.of(this.years, this.months, this.days)

/**
 * Converts this [java.time.Period][java.time.Period] value to a [kotlinx.datetime.DatePeriod][DatePeriod] value.
 */
public fun java.time.Period.toKotlinDatePeriod(): DatePeriod = DatePeriod(this.years, this.months, this.days)


/**
 * Converts this [kotlinx.datetime.TimeZone][TimeZone] value to a [java.time.ZoneId][java.time.ZoneId] value.
 */
public fun TimeZone.toJavaZoneId(): java.time.ZoneId = this.zoneId

/**
 * Converts this [java.time.ZoneId][java.time.ZoneId] value to a [kotlinx.datetime.TimeZone][TimeZone] value.
 */
public fun java.time.ZoneId.toKotlinTimeZone(): TimeZone = TimeZone.ofZone(this)


/**
 * Converts this [kotlinx.datetime.FixedOffsetTimeZone][FixedOffsetTimeZone] value to a [java.time.ZoneOffset][java.time.ZoneOffset] value.
 */
public fun FixedOffsetTimeZone.toJavaZoneOffset(): java.time.ZoneOffset = this.offset.zoneOffset

/**
 * Converts this [java.time.ZoneOffset][java.time.ZoneOffset] value to a [kotlinx.datetime.FixedOffsetTimeZone][FixedOffsetTimeZone] value.
 */
public fun java.time.ZoneOffset.toKotlinFixedOffsetTimeZone(): FixedOffsetTimeZone = FixedOffsetTimeZone(UtcOffset(this))

@Deprecated("Use toKotlinFixedOffsetTimeZone() instead.", ReplaceWith("this.toKotlinFixedOffsetTimeZone()"))
public fun java.time.ZoneOffset.toKotlinZoneOffset(): FixedOffsetTimeZone = toKotlinFixedOffsetTimeZone()

/**
 * Converts this [kotlinx.datetime.UtcOffset][UtcOffset] value to a [java.time.ZoneOffset][java.time.ZoneOffset] value.
 */
public fun UtcOffset.toJavaZoneOffset(): java.time.ZoneOffset = this.zoneOffset

/**
 * Converts this [java.time.ZoneOffset][java.time.ZoneOffset] value to a [kotlinx.datetime.UtcOffset][UtcOffset] value.
 */
public fun java.time.ZoneOffset.toKotlinUtcOffset(): UtcOffset = UtcOffset(this)

/**
 * Converts this [kotlinx.datetime.Month][Month] value to a [java.time.Month][java.time.Month] value.
 */
public fun Month.toJavaMonth(): java.time.Month = java.time.Month.of(number)

/**
 * Converts this [java.time.Month][java.time.Month] value to a [kotlinx.datetime.Month][Month] value.
 */
public fun java.time.Month.toKotlinMonth(): Month = Month.entries[this.value - 1]

/**
 * Converts this [kotlinx.datetime.DayOfWeek][DayOfWeek] value to a [java.time.DayOfWeek][java.time.DayOfWeek] value.
 */
public fun DayOfWeek.toJavaDayOfWeek(): java.time.DayOfWeek = java.time.DayOfWeek.of(isoDayNumber)

/**
 * Converts this [java.time.DayOfWeek][java.time.DayOfWeek] value to a [kotlinx.datetime.DayOfWeek][DayOfWeek] value.
 */
public fun java.time.DayOfWeek.toKotlinDayOfWeek(): DayOfWeek = DayOfWeek.entries[this.value - 1]
