/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.js.*

/**
 * Converts the [Instant] to an instance of JS [Date].
 *
 * The conversion is lossy: JS uses millisecond precision to represent dates, and [Instant] allows for nanosecond
 * resolution.
 */
public fun Instant.toJSDate(): Date = Date(milliseconds = toEpochMilliseconds().toDouble())

/**
 * Converts the JS [Date] to the corresponding [Instant].
 */
public fun Date.toKotlinInstant(): Instant = Instant.fromEpochMilliseconds(getTime().toLong())
