/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal class RuleBasedTimeZoneCalculations(
    private val tzid: TimeZoneRules, val id: String, val origin: Any?
) {
    fun offsetInfoForImpl(dateTime: LocalDateTime): LocalDateTimeOffsetInfo = tzid.infoAtDatetime(dateTime)

    fun offsetAtImpl(instant: Instant): UtcOffset = tzid.infoAtInstant(instant)

    override fun equals(other: Any?): Boolean =
        other is RuleBasedTimeZoneCalculations && id == other.id && origin == other.origin

    override fun hashCode(): Int = id.hashCode() * 31 + origin.hashCode()
}
