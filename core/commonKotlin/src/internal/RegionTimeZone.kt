/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*
import kotlin.time.Instant

internal class RegionTimeZone(private val tzid: TimeZoneRules, override val id: String) : TimeZone() {

    override fun atStartOfDay(date: LocalDate): Instant {
        val ldt = LocalDateTime(date, LocalTime.MIN)
        return when (val info = tzid.infoAtDatetime(ldt)) {
            is OffsetInfo.Regular -> ldt.toInstant(info.offset)
            is OffsetInfo.Gap -> info.start
            is OffsetInfo.Overlap -> ldt.toInstant(info.offsetBefore)
        }
    }

    override fun localDateTimeToInstant(dateTime: LocalDateTime, preferred: UtcOffset?): Instant =
        when (val info = tzid.infoAtDatetime(dateTime)) {
            is OffsetInfo.Regular -> dateTime.toInstant(info.offset)
            is OffsetInfo.Gap -> {
                try {
                    dateTime.plusSeconds(info.transitionDurationSeconds).toInstant(info.offsetAfter)
                } catch (e: IllegalArgumentException) {
                    throw DateTimeArithmeticException(
                        "Overflow whet correcting the date-time to not be in the transition gap",
                        e
                    )
                }
            }

            is OffsetInfo.Overlap -> dateTime.toInstant(
                if (info.offsetAfter == preferred) info.offsetAfter else info.offsetBefore
            )
        }

    override fun offsetAtImpl(instant: Instant): UtcOffset = tzid.infoAtInstant(instant)
}
