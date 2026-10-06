/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDateTimeOffsetInfo
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlin.time.Instant

internal class RuleBasedTimeZone(
    val rules: TimeZoneRules, override val id: String, val origin: Any?
): TimeZone() {
    override fun offsetAt(instant: Instant): UtcOffset = rules.infoAtInstant(instant)

    override fun offsetInfoFor(dateTime: LocalDateTime): LocalDateTimeOffsetInfo =
        rules.infoAtDatetime(dateTime)

    override fun equals(other: Any?): Boolean =
        other is RuleBasedTimeZone && id == other.id && origin == other.origin

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id
}
