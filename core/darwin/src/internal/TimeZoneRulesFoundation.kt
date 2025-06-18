/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.cinterop.UnsafeNumber
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toNSDate
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneWithName

internal class TimeZoneRulesFoundation(zoneId: String) : TimeZoneRules {
    private val nsTimeZone: NSTimeZone = NSTimeZone.timeZoneWithName(zoneId)
        ?: throw IllegalArgumentException("Unknown timezone: $zoneId")

    @OptIn(UnsafeNumber::class)
    override fun infoAtInstant(instant: Instant): UtcOffset {
        val nsDate = instant.toNSDate()
        val offsetSeconds = nsTimeZone.secondsFromGMTForDate(nsDate)
        return UtcOffset.ofSeconds(offsetSeconds.toInt())
    }

    override fun infoAtDatetime(localDateTime: LocalDateTime): OffsetInfo {
        val calendar = NSCalendar.currentCalendar().apply { timeZone = nsTimeZone }
        val components = localDateTime.toNSDateComponents()
        val nsDate = calendar.dateFromComponents(components)
        if (nsDate == null) {
            throw IllegalArgumentException("Invalid LocalDateTime components: $localDateTime")
        }

        val instant = nsDate.toKotlinInstant()
        val utcOffset = infoAtInstant(instant)
        println("utcOffset = $utcOffset")

        return OffsetInfo.Regular(UtcOffset.ofSeconds(0))
    }
}