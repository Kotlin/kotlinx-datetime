/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*
import kotlin.time.Instant

internal class RegionTimeZone(private val tzid: TimeZoneRules, override val id: String) : TimeZone() {
    override fun offsetInfoForImpl(dateTime: LocalDateTime): LocalDateTimeOffsetInfo = tzid.infoAtDatetime(dateTime)
    override fun offsetAtImpl(instant: Instant): UtcOffset = tzid.infoAtInstant(instant)
}
