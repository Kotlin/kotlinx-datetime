/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.cinterop.UnsafeNumber
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
import kotlin.time.Instant

internal class TimeZoneRulesFoundation(zoneId: String) : TimeZoneRules {
    private val nsTimeZone: NSTimeZone = NSTimeZone.timeZoneWithName(zoneId)
        ?: throw IllegalArgumentException("Unknown timezone: $zoneId")

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
            val oneDayEarlier = nsDate.addTimeInterval(-SECS_PER_DAY) as NSDate
            return OffsetInfo.Gap(
                start = nsTimeZone.nextDaylightSavingTimeTransitionAfterDate(oneDayEarlier)!!.toKotlinInstant(),
                offsetBefore = infoAtNsDate(oneDayEarlier),
                offsetAfter = currentOffset
            )
        }

        val oneDayLater = nsDate.addTimeInterval(SECS_PER_DAY) as NSDate
        val oneDayLatterOffset = infoAtNsDate(oneDayLater)
        val delta = currentOffset.totalSeconds - oneDayLatterOffset.totalSeconds
        if (delta > 0) {
            val deltaSecondsLater = nsDate.addTimeInterval(delta.toDouble()) as NSDate
            val deltaSecondsLaterOffset = infoAtNsDate(deltaSecondsLater)
            if (currentOffset.totalSeconds > deltaSecondsLaterOffset.totalSeconds) {
                return OffsetInfo.Overlap(
                    start = nsTimeZone.nextDaylightSavingTimeTransitionAfterDate(nsDate)!!.toKotlinInstant(),
                    offsetBefore = currentOffset,
                    offsetAfter = deltaSecondsLaterOffset
                )
            }
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