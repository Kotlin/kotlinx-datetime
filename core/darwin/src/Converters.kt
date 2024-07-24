/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.UnsafeNumber::class)

package kotlinx.datetime

import kotlinx.cinterop.*
import platform.Foundation.*

/**
 * Converts the [Instant] to an instance of [NSDate].
 *
 * The conversion is lossy: Darwin uses millisecond precision to represent dates, and [Instant] allows for nanosecond
 * resolution.
 */
public fun Instant.toNSDate(): NSDate {
    val secs = epochSeconds * 1.0 + nanosecondsOfSecond / 1.0e9
    if (secs < NSDate.distantPast.timeIntervalSince1970 || secs > NSDate.distantFuture.timeIntervalSince1970) {
        throw IllegalArgumentException("Boundaries of NSDate exceeded")
    }
    return NSDate.dateWithTimeIntervalSince1970(secs)
}

/**
 * Converts the [NSDate] to the corresponding [Instant].
 *
 * Even though Darwin only uses millisecond precision, it is possible that [date] uses larger resolution, storing
 * microseconds or even nanoseconds. In this case, the sub-millisecond parts of [date] are rounded to the nearest
 * millisecond, given that they are likely to be conversion artifacts.
 */
public fun NSDate.toKotlinInstant(): Instant {
    val secs = timeIntervalSince1970()
    val millis = secs * 1000 + if (secs > 0) 0.5 else -0.5
    return Instant.fromEpochMilliseconds(millis.toLong())
}

/**
 * Converts the [TimeZone] to [NSTimeZone].
 *
 * If the time zone is represented as a fixed number of seconds from UTC+0 (for example, if it is the result of a call
 * to [TimeZone.offset]) and the offset is not given in even minutes but also includes seconds, this method throws
 * [IllegalArgumentException] to denote that lossy conversion would happen, as Darwin internally rounds the offsets
 * to the nearest minute.
 *
 * If the time zone is unknown to the Foundation framework, [IllegalArgumentException] will be thrown.
 */
public fun TimeZone.toNSTimeZone(): NSTimeZone = if (this is FixedOffsetTimeZone) {
    require(offset.totalSeconds % 60 == 0) {
        "NSTimeZone cannot represent fixed-offset time zones with offsets not expressed in whole minutes: $this"
    }
    NSTimeZone.timeZoneForSecondsFromGMT(offset.totalSeconds.convert())
} else {
    NSTimeZone.timeZoneWithName(id)
        ?: NSTimeZone.timeZoneWithAbbreviation(id)
        ?: throw IllegalArgumentException("The Foundation framework does not support the timezone '$id'")
}

/**
 * Converts the [NSTimeZone] to the corresponding [TimeZone].
 */
public fun NSTimeZone.toKotlinTimeZone(): TimeZone = TimeZone.of(name)

/**
 * Converts the given [LocalDate] to [NSDateComponents].
 *
 * Of all the fields, only the bare minimum required for uniquely identifying the date are set.
 */
public fun LocalDate.toNSDateComponents(): NSDateComponents {
    val components = NSDateComponents()
    components.year = year.convert()
    components.month = month.number.convert()
    components.day = day.convert()
    return components
}

/**
 * Converts the given [LocalDate] to [NSDateComponents].
 *
 * Of all the fields, only the bare minimum required for uniquely identifying the date and time are set.
 */
public fun LocalDateTime.toNSDateComponents(): NSDateComponents {
    val components = date.toNSDateComponents()
    components.hour = hour.convert()
    components.minute = minute.convert()
    components.second = second.convert()
    components.nanosecond = nanosecond.convert()
    return components
}
