/*
 * Copyright 2019-2020 JetBrains s.r.o.
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
public fun java.time.ZoneId.toKotlinTimeZone(): TimeZone = TimeZone(this)


/**
 * Converts this [kotlinx.datetime.FixedOffsetTimeZone][FixedOffsetTimeZone] value to a [java.time.ZoneOffset][java.time.ZoneOffset] value.
 */
public fun FixedOffsetTimeZone.toJavaZoneOffset(): java.time.ZoneOffset = this.utcOffset.zoneOffset

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

