/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetimex.darwin.converters
import kotlinx.cinterop.*
import kotlinx.datetime.*
import platform.Foundation.*

/**
 * Converts the Instant to an instance of [NSDate].
 * The conversion is lossy: Darwin uses millisecond precision to represent dates, and [Instant] allows for nanosecond
 * resolution.
 */
public fun Instant.toNSDate(): NSDate {
    // TODO: support nanosecond precision.
    // This is not urgent, as Darwin itself only uses millisecond precision.
    // However, we could at least round to the nearest millisecond so that it would be true that
    // Instant.parse(...).toNSDate() == [formatter dateFromString: ...],
    // as Darwin does perform rounding.
    val secs = toEpochMilliseconds() / 1000.0
    return NSDate.dateWithTimeIntervalSince1970(secs)
}

/**
 * Builds the corresponding [Instant].
 * Even though Darwin only uses millisecond precision, it is possible that [date] uses larger resolution, storing
 * microseconds or even nanoseconds. In this case, the sub-millisecond parts of [date] are ignored, given that they
 * are likely to be conversion artifacts.
 */
public fun NSDate.toKotlinInstant(): Instant {
    val secs = timeIntervalSince1970()
    val millis = secs * 1000
    return Instant.fromEpochMilliseconds(millis.toLong())
}

/**
 * Converts the time zone to [NSTimeZone].
 * If the time zone is represented as a fixed number of seconds from GMT (for example, if it is the result of a call to
 * [Instant.offset]) and the offset is not given in even minutes but also includes seconds, this method throws
 * [DateTimeException] to denote that lossy conversion would happen, as Darwin internally rounds the offsets to the
 * nearest minute.
 */
public fun TimeZone.toNSTimeZone(): NSTimeZone = if (this is ZoneOffset) {
    if (totalSeconds % 60 == 0) {
        NSTimeZone.timeZoneForSecondsFromGMT(totalSeconds.convert())
    } else {
        throw DateTimeException("Lossy conversion: Darwin uses minute precision for fixed-offset time zones")
    }
} else {
    NSTimeZone.timeZoneWithName(id) ?: NSTimeZone.timeZoneWithAbbreviation(id)!!
}

/**
 * Builds the corresponding [TimeZone].
 */
public fun NSTimeZone.toKotlinTimeZone(): TimeZone = TimeZone.of(name)

/**
 * Converts the given [LocalDate] to [NSDateComponents].
 * Of all the fields, only the bare minimum required for uniquely identifying the date are set.
 */
public fun LocalDate.toNSDateComponents(): NSDateComponents {
    val components = NSDateComponents()
    components.year = year.convert()
    components.month = monthNumber.convert()
    components.day = dayOfMonth.convert()
    return components
}

/**
 * Converts the given [LocalDate] to [NSDateComponents].
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
