/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*
import kotlin.time.Instant

internal class RuleBasedTimeZone(private val calculations: RuleBasedTimeZoneCalculations): TimeZone() {
    override val id: String get() = calculations.id

    override fun offsetAt(instant: Instant): UtcOffset = calculations.offsetAt(instant)

    override fun offsetInfoFor(dateTime: LocalDateTime): LocalDateTimeOffsetInfo =
        calculations.offsetInfoFor(dateTime)

    override fun equals(other: Any?): Boolean =
        other is RuleBasedTimeZone && calculations == other.calculations

    override fun hashCode(): Int = calculations.hashCode()
}


internal actual fun RuleBasedTimeZoneCalculations.asTimeZone(): TimeZone = RuleBasedTimeZone(this)

internal actual fun FixedOffsetTimeZone.Companion.withSpecificName(offset: UtcOffset, id: String): FixedOffsetTimeZone =
    FixedOffsetTimeZone(offset, id)
