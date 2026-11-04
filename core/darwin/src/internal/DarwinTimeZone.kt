/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.cinterop.*
import kotlinx.datetime.*
import platform.Foundation.*
import kotlin.time.Instant
import kotlin.math.roundToInt

internal class DarwinTimeZone(private val nsTimeZone: NSTimeZone, override val id: String) : TimeZone() {

    override fun offsetAt(instant: Instant): UtcOffset =
        /* NSTimeZone operates with millisecond precision, so we want convert .123XXXXXX to .123000000,
        to avoid NSTimeZone rounding it up to S.124. */
        infoAtNsDate(instant.toNSDateRoundingDownToMillisecond())

    @OptIn(ExperimentalForeignApi::class)
    override fun standardOffsetAt(instant: Instant): UtcOffset {
        val nsDate = instant.toNSDate()
        val totalOffsetSeconds = nsTimeZone.secondsFromGMTForDate(nsDate) 
        val dstOffsetContributionSeconds = nsTimeZone.daylightSavingTimeOffsetForDate(nsDate)
        val standardOffsetSeconds = totalOffsetSeconds - dstOffsetContributionSeconds.roundToInt()
        return UtcOffset(seconds = standardOffsetSeconds.convert())
    }

    /**
     * IMPORTANT: mirrors the logic in [RecurringZoneRules.infoAtLocalDateTime].
     * Any update to offset calculations, transition handling, or edge cases
     * must be duplicated there to maintain consistent behavior across
     * all platforms.
     */
    @OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
    override fun offsetInfoFor(dateTime: LocalDateTime): LocalDateTimeOffsetInfo {
        val calendar = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierISO8601)
            ?.apply { timeZone = nsTimeZone }

        val year = dateTime.year
        val startOfTheYear = calendar?.dateFromComponents(LocalDateTime(year, 1, 1, 0, 0).toNSDateComponents())
        check(startOfTheYear != null) { "Failed to get the start of the year for $dateTime, timezone: $nsTimeZone" }

        var currentDate: NSDate = startOfTheYear
        var offset = infoAtNsDate(startOfTheYear)
        do {
            val transitionDateTime = nsTimeZone.nextDaylightSavingTimeTransitionAfterDate(currentDate) ?: break

            val yearOfNextDate = calendar.component(NSCalendarUnitYear.convert(), fromDate = transitionDateTime)
            val transitionDateTimeInstant = transitionDateTime.toKotlinInstant()

            val offsetBefore = infoAtNsDate(currentDate)
            val ldtBefore = transitionDateTimeInstant.toLocalDateTime(offsetBefore)
            val offsetAfter = infoAtNsDate(transitionDateTime)
            val ldtAfter = transitionDateTimeInstant.toLocalDateTime(offsetAfter)

            return if (dateTime < ldtBefore && dateTime < ldtAfter) {
                LocalDateTimeOffsetInfo.Regular(offsetBefore)
            } else if (dateTime >= ldtBefore && dateTime >= ldtAfter) {
                offset = offsetAfter
                currentDate = transitionDateTime
                continue
            } else if (ldtAfter < ldtBefore) {
                LocalDateTimeOffsetInfo.Overlap(transitionDateTimeInstant, offsetBefore, offsetAfter)
            } else {
                LocalDateTimeOffsetInfo.Gap(transitionDateTimeInstant, offsetBefore, offsetAfter)
            }
        } while (yearOfNextDate <= year)

        return LocalDateTimeOffsetInfo.Regular(offset)
    }

    @OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
    private fun infoAtNsDate(nsDate: NSDate): UtcOffset {
        val offsetSeconds = nsTimeZone.secondsFromGMTForDate(nsDate)
        return UtcOffset(seconds = offsetSeconds.convert())
    }

    public override fun toString(): String = id

    public override fun equals(other: Any?): Boolean =
        other === this || other is DarwinTimeZone && id == other.id

    public override fun hashCode(): Int =
        id.hashCode()
}

private fun Instant.toNSDateRoundingDownToMillisecond(): NSDate =
    NSDate.dateWithTimeIntervalSince1970(epochSeconds * 1.0 + (nanosecondsOfSecond / 1000) / 1.0e6)
