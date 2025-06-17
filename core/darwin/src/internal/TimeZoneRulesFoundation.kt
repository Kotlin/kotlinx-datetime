/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset

internal class TimeZoneRulesFoundation(private val zoneId: String) : TimeZoneRules {
    override fun infoAtInstant(instant: Instant): UtcOffset {
        TODO("TimeZoneRulesFoundation.infoAtInstant: not yet implemented")
    }

    override fun infoAtDatetime(localDateTime: LocalDateTime): OffsetInfo {
        TODO("TimeZoneRulesFoundation.infoAtDatetime: not yet implemented")
    }
}