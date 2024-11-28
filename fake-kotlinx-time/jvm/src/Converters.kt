/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.time

/**
 * Converts this [kotlin.time.Instant][Instant] value to a [java.time.Instant][java.time.Instant] value.
 */
public fun Instant.toJavaInstant(): java.time.Instant =
    java.time.Instant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())

/**
 * Converts this [java.time.Instant][java.time.Instant] value to a [kotlin.time.Instant][Instant] value.
 */
public fun java.time.Instant.toKotlinInstant(): Instant = Instant.fromEpochSeconds(epochSecond, nano)
