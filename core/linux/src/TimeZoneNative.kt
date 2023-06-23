/*
 * Copyright 2019-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.cinterop.*
import kotlinx.datetime.internal.*
import platform.posix.*

internal actual class RegionTimeZone(private val tzid: TimeZoneRules, actual override val id: String) : TimeZone() {
    actual companion object {
        actual fun of(zoneId: String): RegionTimeZone = try {
            RegionTimeZone(tzdbOnFilesystem.rulesForId(zoneId), zoneId)
        } catch (e: Exception) {
            throw IllegalTimeZoneException("Invalid zone ID: $zoneId", e)
        }

        actual fun currentSystemDefault(): RegionTimeZone {
            val zoneId = tzdbOnFilesystem.currentSystemDefault()?.second
                ?: throw IllegalStateException("Failed to get the system timezone")
            return of(zoneId.toString())
        }

        actual val availableZoneIds: Set<String>
            get() = tzdbOnFilesystem.availableTimeZoneIds()
    }

    actual override fun atStartOfDay(date: LocalDate): Instant = memScoped {
        val ldt = LocalDateTime(date, LocalTime.MIN)
        when (val info = tzid.infoAtDatetime(ldt)) {
            is Regular -> ldt.toInstant(info.offset)
            is Gap -> info.start
            is Overlap -> ldt.toInstant(info.offsetBefore)
        }
    }

    actual override fun atZone(dateTime: LocalDateTime, preferred: UtcOffset?): ZonedDateTime =
        when (val info = tzid.infoAtDatetime(dateTime)) {
            is Regular -> ZonedDateTime(dateTime, this, info.offset)
            is Gap -> {
                try {
                    ZonedDateTime(dateTime.plusSeconds(info.transitionDurationSeconds), this, info.offsetAfter)
                } catch (e: IllegalArgumentException) {
                    throw DateTimeArithmeticException(
                        "Overflow whet correcting the date-time to not be in the transition gap",
                        e
                    )
                }
            }

            is Overlap -> ZonedDateTime(dateTime, this,
                if (info.offsetAfter == preferred) info.offsetAfter else info.offsetBefore)
        }

    actual override fun offsetAtImpl(instant: Instant): UtcOffset = tzid.infoAtInstant(instant)
}

internal actual fun currentTime(): Instant = memScoped {
    val tm = alloc<timespec>()
    val error = clock_gettime(CLOCK_REALTIME, tm.ptr)
    if (error != 0) {
        val errorStr: String = strerror(errno)?.toKString() ?: "Unknown error"
        throw IllegalStateException("Could not obtain the current clock readings from the system: $errorStr")
    }
    val seconds: Long = tm.tv_sec
    val nanoseconds: Int = tm.tv_nsec.toInt()
    try {
        require(nanoseconds in 0 until NANOS_PER_ONE)
        return Instant(seconds, nanoseconds)
    } catch (e: IllegalArgumentException) {
        throw IllegalStateException("The readings from the system clock are not representable as an Instant")
    }
}

@ThreadLocal
private val tzdbOnFilesystem = TzdbOnFilesystem(Path.fromString("/usr/share/zoneinfo"))
