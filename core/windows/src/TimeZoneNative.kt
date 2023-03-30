/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package kotlinx.datetime

import kotlinx.cinterop.*
import kotlinx.datetime.internal.*
import platform.posix.*
import platform.windows.*

internal actual class RegionTimeZone(private val tzid: TimeZoneRules, actual override val id: String) : TimeZone() {
    actual companion object {
        actual fun of(zoneId: String): RegionTimeZone = try {
            RegionTimeZone(tzdbInRegistry.rulesForId(zoneId), zoneId)
        } catch (e: Exception) {
            throw IllegalTimeZoneException("Invalid zone ID: $zoneId", e)
        }

        actual fun currentSystemDefault(): RegionTimeZone {
            val (name, zoneRules) = tzdbInRegistry.currentSystemDefault()
            return RegionTimeZone(zoneRules, name)
        }

        actual val availableZoneIds: Set<String>
            get() = tzdbInRegistry.availableTimeZoneIds()
    }

    actual override fun atStartOfDay(date: LocalDate): Instant = memScoped {
        val ldt = LocalDateTime(date, LocalTime.MIN)
        when (val info = tzid.infoAtDatetime(ldt)) {
            is OffsetInfo.Regular -> ldt.toInstant(info.offset)
            is OffsetInfo.Gap -> info.start
            is OffsetInfo.Overlap -> ldt.toInstant(info.offsetBefore)
        }
    }

    actual override fun atZone(dateTime: LocalDateTime, preferred: UtcOffset?): ZonedDateTime =
        when (val info = tzid.infoAtDatetime(dateTime)) {
            is OffsetInfo.Regular -> ZonedDateTime(dateTime, this, info.offset)
            is OffsetInfo.Gap -> {
                try {
                    ZonedDateTime(dateTime.plusSeconds(info.transitionDurationSeconds), this, info.offsetAfter)
                } catch (e: IllegalArgumentException) {
                    throw DateTimeArithmeticException(
                        "Overflow whet correcting the date-time to not be in the transition gap",
                        e
                    )
                }
            }

            is OffsetInfo.Overlap -> ZonedDateTime(dateTime, this,
                if (info.offsetAfter == preferred) info.offsetAfter else info.offsetBefore)
        }

    actual override fun offsetAtImpl(instant: Instant): UtcOffset = tzid.infoAtInstant(instant)
}

private val tzdbInRegistry = TzdbInRegistry()

internal actual fun currentTime(): Instant = memScoped {
    val tm = alloc<timespec>()
    val error = clock_gettime(CLOCK_REALTIME, tm.ptr)
    check(error == 0) { "Error when reading the system clock: ${strerror(errno)}" }
    try {
        require(tm.tv_nsec in 0 until NANOS_PER_ONE)
        Instant(tm.tv_sec, tm.tv_nsec)
    } catch (e: IllegalArgumentException) {
        throw IllegalStateException("The readings from the system clock (${tm.tv_sec} seconds, ${tm.tv_nsec} nanoseconds) are not representable as an Instant")
    }
}
