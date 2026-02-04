/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmMultifileClass
@file:JvmName("InstantJvmKt")

package kotlinx.datetime

import kotlin.time.Instant

/**
 * All of these functions were available in the `InstantJvm.kt` file as `actual` parts of `expect`/`actual`.
 * To preserve binary compatibility with 0.7.x, they are left here for one major version (to be removed in 0.9.0).
 */

@JvmName("minus") @PublishedApi internal fun minusJvm(instant: Instant, value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    instant.minus(value, unit, timeZone)

@JvmName("periodUntil") @PublishedApi internal fun periodUntilJvm(
    start: Instant, end: Instant, timeZone: TimeZone
): DateTimePeriod = start.periodUntil(end, timeZone)

@JvmName("plus") @PublishedApi internal fun plusJvm(
    instant: Instant, value: Int, unit: DateTimeUnit, timeZone: TimeZone
): Instant = instant.plus(value, unit, timeZone)

@JvmName("plus") @PublishedApi internal fun plusJvm(
    instant: Instant, value: Long, unit: DateTimeUnit.TimeBased
): Instant = instant.plus(value, unit)

@JvmName("plus") @PublishedApi internal fun plusJvm(
    instant: Instant, value: Long, unit: DateTimeUnit, timeZone: TimeZone
): Instant = instant.plus(value, unit, timeZone)

@JvmName("plus") @PublishedApi internal fun plusJvm(
    instant: Instant, period: DateTimePeriod, timeZone: TimeZone
): Instant = instant.plus(period, timeZone)

@JvmName("plus") @PublishedApi internal fun plusJvm(
    instant: Instant, unit: DateTimeUnit, timeZone: TimeZone
): Instant = instant.plus(1, unit, timeZone)

@JvmName("until") @PublishedApi internal fun untilJvm(
    start: Instant, end: Instant, unit: DateTimeUnit, timeZone: TimeZone
): Long = start.until(end, unit, timeZone)
