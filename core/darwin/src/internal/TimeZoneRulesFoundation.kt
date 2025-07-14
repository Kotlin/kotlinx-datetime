/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toNSDate
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierISO8601
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneWithName
import kotlin.time.Instant

internal class TimeZoneRulesFoundation(private val zoneId: String) : TimeZoneRules {
    private val nsTimeZone: NSTimeZone = NSTimeZone.timeZoneWithName(zoneId)
        ?: throw IllegalArgumentException("Unknown timezone: $zoneId")

    override fun infoAtInstant(instant: Instant): UtcOffset =
        infoAtNsDate(instant.toNSDate())

    /**
     * IMPORTANT: mirrors the logic in [RecurringZoneRules.infoAtLocalDateTime].
     * Any update to offset calculations, transition handling, or edge cases
     * must be duplicated there to maintain consistent behavior across
     * all platforms.
     */
    @OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
    override fun infoAtDatetime(localDateTime: LocalDateTime): OffsetInfo {
        val calendar = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierISO8601)
            ?.apply { timeZone = nsTimeZone }

        val year = localDateTime.year
        val startOfTheYear = calendar?.dateFromComponents(LocalDateTime(year, 1, 1, 0, 0).toNSDateComponents())
        check(startOfTheYear != null) { "Failed to get the start of the year for $localDateTime, timezone: $zoneId" }

        var currentDate: NSDate = startOfTheYear
        var offset = infoAtNsDate(startOfTheYear)
        do {
            val transitionDateTime = nsTimeZone.nextDaylightSavingTimeTransitionAfterDate(currentDate)
            if (transitionDateTime == null) break

            val yearOfNextDate = calendar.component(NSCalendarUnitYear.convert(), fromDate = transitionDateTime)
            val transitionDateTimeInstant = transitionDateTime.toKotlinInstant()

            val offsetBefore = infoAtNsDate(currentDate)
            val ldtBefore = transitionDateTimeInstant.toLocalDateTime(offsetBefore)
            val offsetAfter = infoAtNsDate(transitionDateTime)
            val ldtAfter = transitionDateTimeInstant.toLocalDateTime(offsetAfter)

            return if (localDateTime < ldtBefore && localDateTime < ldtAfter) {
                OffsetInfo.Regular(offsetBefore)
            } else if (localDateTime >= ldtBefore && localDateTime >= ldtAfter) {
                offset = offsetAfter
                currentDate = transitionDateTime
                continue
            } else if (ldtAfter < ldtBefore) {
                OffsetInfo.Overlap(transitionDateTimeInstant, offsetBefore, offsetAfter)
            } else {
                OffsetInfo.Gap(transitionDateTimeInstant, offsetBefore, offsetAfter)
            }
        } while (yearOfNextDate <= year)

        return OffsetInfo.Regular(offset)
    }

    @OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
    private fun infoAtNsDate(nsDate: NSDate): UtcOffset {
        val offsetSeconds = nsTimeZone.secondsFromGMTForDate(nsDate)
        return UtcOffset(seconds = offsetSeconds.convert())
    }
}