/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*
import kotlin.time.Instant

internal class RuleBasedTimeZone(
    private val tzid: TimeZoneRules, override val id: String, val origin: Any?, overloadResolver: Unit
): TimeZone() {
    override fun offsetAt(instant: Instant): UtcOffset = tzid.infoAtInstant(instant)

    override fun offsetInfoFor(dateTime: LocalDateTime): LocalDateTimeOffsetInfo =
        tzid.infoAtDatetime(dateTime)

    override fun equals(other: Any?): Boolean =
        other is RuleBasedTimeZone && id == other.id && origin == other.origin

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id
}

internal actual fun RuleBasedTimeZone(tzid: TimeZoneRules, id: String, origin: Any?): TimeZone =
    RuleBasedTimeZone(tzid, id, origin, Unit)
