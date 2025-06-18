/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.cinterop.UnsafeNumber
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toNSDate
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.addTimeInterval
import platform.Foundation.timeZoneWithName

internal class TimeZoneRulesFoundation(zoneId: String) : TimeZoneRules {
    private val nsTimeZone: NSTimeZone = NSTimeZone.timeZoneWithName(zoneId)
        ?: throw IllegalArgumentException("Unknown timezone: $zoneId")

    @OptIn(UnsafeNumber::class)
    override fun infoAtInstant(instant: Instant): UtcOffset =
        infoAtNsDate(instant.toNSDate())

    override fun infoAtDatetime(localDateTime: LocalDateTime): OffsetInfo {
        val calendar = NSCalendar.currentCalendar().apply { timeZone = nsTimeZone }
        val components = localDateTime.toNSDateComponents()

        val nsDate = requireNotNull(calendar.dateFromComponents(components)) {
            "Invalid LocalDateTime components: $localDateTime"
        }

        val currentOffset = infoAtNsDate(nsDate)

        if (!components.isValidDateInCalendar(calendar)) {
            val prevDay = nsDate.addTimeInterval(-SECS_PER_DAY) as NSDate
            return OffsetInfo.Gap(
                start = nsTimeZone.nextDaylightSavingTimeTransitionAfterDate(prevDay)!!.toKotlinInstant(),
                offsetBefore = infoAtNsDate(prevDay),
                offsetAfter = currentOffset
            )
        }

        val currentTransition = nsTimeZone.nextDaylightSavingTimeTransitionAfterDate(nsDate)
        val nextDay = nsDate.addTimeInterval(SECS_PER_DAY) as NSDate
        val nextDayTransition = nsTimeZone.nextDaylightSavingTimeTransitionAfterDate(nextDay)

        if (currentTransition != nextDayTransition) {
            return OffsetInfo.Overlap(
                start = currentTransition!!.toKotlinInstant(),
                offsetBefore = currentOffset,
                offsetAfter = infoAtNsDate(nextDay)
            )
        }

        return OffsetInfo.Regular(currentOffset)
    }

    @OptIn(UnsafeNumber::class)
    private fun infoAtNsDate(nsDate: NSDate): UtcOffset {
        val offsetSeconds = nsTimeZone.secondsFromGMTForDate(nsDate)
        return UtcOffset.ofSeconds(offsetSeconds.toInt())
    }
    companion object {
        const val SECS_PER_DAY = 24 * 60 * 60.0
    }
}