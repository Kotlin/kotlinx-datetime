/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("LocalDateJvmKt")
package kotlinx.datetime

import kotlinx.datetime.LocalDate.Formats
import kotlinx.datetime.format.*
import kotlinx.datetime.internal.safeAdd
import kotlinx.datetime.internal.safeMultiply
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.LocalDate as jtLocalDate

// Preserving binary compatibility with 0.8.x, before LocalDate.kt was reorganized
@PublishedApi
@Suppress("INVISIBLE_REFERENCE") @kotlin.internal.LowPriorityInOverloadResolution
@JvmName("parseOrNull")
internal fun LocalDate.Companion.parseOrNullJvm(
    input: CharSequence, format: DateTimeFormat<LocalDate> = LocalDate.Formats.ISO
): LocalDate? = parseOrNull(input, format)

/**
 * @suppress
 */
@Deprecated(
    "Use the constructor that accepts a 'day'",
    ReplaceWith("LocalDate(year = year, month = month.toKotlinMonth(), day = dayOfMonth)"),
    level = DeprecationLevel.ERROR
)
@Suppress("INVISIBLE_REFERENCE") @kotlin.internal.LowPriorityInOverloadResolution
public fun LocalDate(year: Int, month: java.time.Month, dayOfMonth: Int): LocalDate =
    LocalDate(year = year, month = month.toKotlinMonth(), day = dayOfMonth)

@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit)"), level = DeprecationLevel.ERROR)
public actual fun LocalDate.plus(unit: DateTimeUnit.DateBased): LocalDate =
        plus(1L, unit)

@PublishedApi
@JvmName("plus")
internal fun LocalDate.plusJvm(value: Int, unit: DateTimeUnit.DateBased): LocalDate = plus(value.toLong(), unit)

@PublishedApi
@JvmName("minus")
internal fun LocalDate.minusJvm(value: Int, unit: DateTimeUnit.DateBased): LocalDate = plus(-value.toLong(), unit)

// Preserving binary compatibility with 0.8.x, before LocalDate.kt was reorganized
@PublishedApi
@JvmName("plus")
internal fun LocalDate.plusJvm(value: Long, unit: DateTimeUnit.DateBased): LocalDate = plus(value, unit)

// Preserving binary compatibility with 0.8.x, before LocalDate.kt was reorganized
@PublishedApi
@JvmName("plus")
internal fun LocalDate.plusJvm(period: DatePeriod): LocalDate = plus(period)

// Preserving binary compatibility with 0.8.x, before LocalDate.kt was reorganized
@PublishedApi
@JvmName("periodUntil")
internal fun LocalDate.periodUntilJvm(other: LocalDate): DatePeriod = periodUntil(other)

// Preserving binary compatibility with 0.8.x, before LocalDate.kt was reorganized
@PublishedApi
@JvmName("until")
internal fun LocalDate.untilJvm2(other: LocalDate, unit: DateTimeUnit.DateBased): Long = until(other, unit)

@PublishedApi
@JvmName("until")
internal fun LocalDate.untilJvm1(other: LocalDate, unit: DateTimeUnit.DateBased): Int =
    until(other, unit).clampToInt()

// Preserving binary compatibility with 0.8.x, before LocalDate.kt was reorganized
@PublishedApi
@JvmName("daysUntil")
internal fun LocalDate.daysUntilJvm(other: LocalDate): Int = daysUntil(other)

// Preserving binary compatibility with 0.8.x, before LocalDate.kt was reorganized
@PublishedApi
@JvmName("monthsUntil")
internal fun LocalDate.monthsUntilJvm(other: LocalDate): Int = monthsUntil(other)

// Preserving binary compatibility with 0.8.x, before LocalDate.kt was reorganized
@PublishedApi
@JvmName("yearsUntil")
internal fun LocalDate.yearsUntilJvm(other: LocalDate): Int = yearsUntil(other)
