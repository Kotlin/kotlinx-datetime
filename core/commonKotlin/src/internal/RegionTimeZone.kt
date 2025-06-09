/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*

internal class RegionTimeZone(private val tzid: TimeZoneRules, override val id: String) : TimeZone() {

    override fun atStartOfDay(date: LocalDate): Instant {
        val ldt = LocalDateTime(date, LocalTime.MIN)
        return when (val info = tzid.infoAtDatetime(ldt)) {
            is OffsetInfo.Regular -> ldt.toInstant(info.offset)
            is OffsetInfo.Gap -> info.start
            is OffsetInfo.Overlap -> ldt.toInstant(info.offsetBefore)
        }
    }

    override fun atZone(dateTime: LocalDateTime, preferred: UtcOffset?): ZonedDateTime =
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

    override fun offsetAtImpl(instant: Instant): UtcOffset = tzid.infoAtInstant(instant)
}
