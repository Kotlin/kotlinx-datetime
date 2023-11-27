/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package kotlinx.datetime

import kotlinx.cinterop.memScoped
import kotlinx.datetime.internal.OffsetInfo
import kotlinx.datetime.internal.TimeZoneRules
import kotlinx.datetime.internal.TzdbOnFilesystem

internal expect val tzdbOnFilesystem: TzdbOnFilesystem

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
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
