/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.cinterop.*
import kotlinx.datetime.*
import platform.Foundation.*
import kotlin.time.Instant
import kotlin.time.Duration.Companion.seconds

internal class DarwinTimeZone(private val nsTimeZone: NSTimeZone, override val id: String) : TimeZone() {

    override fun offsetAt(instant: Instant): UtcOffset =
        /* NSTimeZone operates with millisecond precision, so we want convert .123XXXXXX to .123000000,
        to avoid NSTimeZone rounding it up to S.124. */
        infoAtNsDate(instant.toNSDateRoundingDownToMillisecond())

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

    override fun transitionsAfter(instant: Instant) = sequence {
        var instant = instant
        val distantFuture = NSDate.distantFuture.toKotlinInstant()
        while (instant < distantFuture) {
            val nsDate = nsTimeZone.nextDaylightSavingTimeTransitionAfterDate(
                instant.toNSDateRoundingDownToMillisecond()
            ) ?: return@sequence
            val offsetAfter = infoAtNsDate(nsDate)
            val offsetBefore = infoAtNsDate(nsDate.dateByAddingTimeInterval(-1.0))
            val newInstant = nsDate.toKotlinInstant()
            if (offsetBefore != offsetAfter) {
                yield(LocalDateTimeOffsetInfo.Transition(newInstant, offsetBefore, offsetAfter))
            }
            instant = newInstant
        }
    }

    override fun transitionsBefore(instant: Instant) = sequence {
        /* There is no `previousDaylightSavingTimeTransition`, so we improvise. The algorithm in broad strokes:
        1. Subtract an arbitrary period of time from the current upper bound.
        2. Using `nextDaylightSavingTimeTransition`, iterate over transitions until we overshoot the upper bound.
        3. Emit the traversed transitions, moving back in time.
        Example (Europe/Berlin):
        0:
        | 2022-10-30 | 2023-03-26 | 2023-10-29 | 2024-03-31 | 2024-10-27 | (start, 2025-01)
                                                                              ^
                                                                              |
                                                                           cursor
        1:
        | 2022-10-30 | 2023-03-26 | 2023-10-29 | 2024-03-31 | 2024-10-27 | (start, 2025-01)
                     ^
                     |
                  cursor
        2:
        | 2022-10-30 | 2023-03-26 | 2023-10-29 | 2024-03-31 | 2024-10-27 | (start, 2025-01)
                            ^           ^            ^            ^
                            |           |            |            |
                            ---------------------------------------
                            |
                         enqueued
        3: Emitted 2024-10-27, 2024-03-31, 2023-10-29, 2023-03-26;
        | 2022-10-30 | 2023-03-26 | 2023-10-29 | 2024-03-31 | 2024-10-27 | 2025-01
                     ^
                     |
                  cursor
        */
        // Inclusive upper bound: can itself be a transition
        var nextUpperBound = instant
        val distantPast = NSDate.distantPast.toKotlinInstant()
        // Earlier to later transitions
        val queue = mutableListOf<NSDate>()
        while (queue.isNotEmpty() || nextUpperBound > distantPast) {
            when (val lastTransition = queue.removeLastOrNull()) {
                null -> {
                    val newNextUpperBound = nextUpperBound - 100_000_000.seconds // - around 3 years
                    var toCheck = newNextUpperBound
                    while (true) {
                        val nextTransition = nsTimeZone.nextDaylightSavingTimeTransitionAfterDate(
                            toCheck.toNSDateRoundingDownToMillisecond()
                        ) ?: break
                        val nextTransitionInstant = nextTransition.toKotlinInstant()
                        if (nextTransitionInstant > nextUpperBound) break
                        queue.add(nextTransition)
                        toCheck = nextTransitionInstant
                    }
                    nextUpperBound = newNextUpperBound
                }
                else -> {
                    val offsetAfter = infoAtNsDate(lastTransition)
                    val offsetBefore = infoAtNsDate(lastTransition.dateByAddingTimeInterval(-1.0))
                    if (offsetBefore != offsetAfter) {
                        yield(LocalDateTimeOffsetInfo.Transition(
                            lastTransition.toKotlinInstant(), offsetBefore, offsetAfter
                        ))
                    }
                }
            }
        }
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
