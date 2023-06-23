/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

import kotlinx.datetime.internal.*
import kotlinx.cinterop.*
import platform.posix.free

internal actual class RegionTimeZone(private val tzid: TZID, actual override val id: String): TimeZone() {
    actual companion object {
        actual fun of(zoneId: String): RegionTimeZone {
            val tzid = timezone_by_name(zoneId)
            if (tzid == TZID_INVALID) {
                throw IllegalTimeZoneException("No timezone found with zone ID '$zoneId'")
            }
            return RegionTimeZone(tzid, zoneId)
        }

        actual fun currentSystemDefault(): RegionTimeZone = memScoped {
            val tzid = alloc<TZIDVar>()
            val string = get_system_timezone(tzid.ptr)
                ?: throw RuntimeException("Failed to get the system timezone.")
            val kotlinString = string.toKString()
            free(string)
            RegionTimeZone(tzid.value, kotlinString)
        }

        actual val availableZoneIds: Set<String>
            get() {
                val set = mutableSetOf("UTC")
                val zones = available_zone_ids()
                    ?: throw RuntimeException("Failed to get the list of available timezones")
                var ptr = zones
                while (true) {
                    val cur = ptr.pointed.value ?: break
                    val zoneName = cur.toKString()
                    set.add(zoneName)
                    free(cur)
                    ptr = (ptr + 1)!!
                }
                free(zones)
                return set
            }
    }

    actual override fun atStartOfDay(date: LocalDate): Instant = memScoped {
        val ldt = LocalDateTime(date, LocalTime.MIN)
        val epochSeconds = ldt.toEpochSecond(UtcOffset.ZERO)
        val midnightInstantSeconds = at_start_of_day(tzid, epochSeconds)
        if (midnightInstantSeconds == Long.MAX_VALUE) {
            throw RuntimeException("Unable to acquire the time of start of day at $date for zone $this")
        }
        Instant(midnightInstantSeconds, 0)
    }

    actual override fun atZone(dateTime: LocalDateTime, preferred: UtcOffset?): ZonedDateTime = memScoped {
        val epochSeconds = dateTime.toEpochSecond(UtcOffset.ZERO)
        val offset = alloc<IntVar>()
        offset.value = preferred?.totalSeconds ?: Int.MAX_VALUE
        val transitionDuration = offset_at_datetime(tzid, epochSeconds, offset.ptr)
        if (offset.value == Int.MAX_VALUE) {
            throw RuntimeException("Unable to acquire the offset at $dateTime for zone ${this@RegionTimeZone}")
        }
        val correctedDateTime = try {
            dateTime.plusSeconds(transitionDuration)
        } catch (e: IllegalArgumentException) {
            throw DateTimeArithmeticException("Overflow whet correcting the date-time to not be in the transition gap", e)
        } catch (e: ArithmeticException) {
            throw RuntimeException("Anomalously long timezone transition gap reported", e)
        }
        ZonedDateTime(correctedDateTime, this@RegionTimeZone, UtcOffset.ofSeconds(offset.value))
    }

    actual override fun offsetAtImpl(instant: Instant): UtcOffset {
        val offset = offset_at_instant(tzid, instant.epochSeconds)
        if (offset == Int.MAX_VALUE) {
            throw RuntimeException("Unable to acquire the offset at instant $instant for zone $this")
        }
        return UtcOffset.ofSeconds(offset)
    }

}

internal actual fun currentTime(): Instant = memScoped {
    val seconds = alloc<LongVar>()
    val nanoseconds = alloc<IntVar>()
    val result = current_time(seconds.ptr, nanoseconds.ptr)
    try {
        require(result)
        require(nanoseconds.value >= 0 && nanoseconds.value < NANOS_PER_ONE)
        Instant(seconds.value, nanoseconds.value)
    } catch (e: IllegalArgumentException) {
        throw IllegalStateException("The readings from the system clock are not representable as an Instant")
    }
}
