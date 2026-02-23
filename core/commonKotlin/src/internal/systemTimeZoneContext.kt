/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*
import kotlin.time.Instant

internal class RuleBasedTimeZone(val calculations: RuleBasedTimeZoneCalculations): TimeZone() {
    override val id: String get() = calculations.id
    val original: Any? = null

    override fun atStartOfDay(date: LocalDate): Instant = calculations.atStartOfDay(date)

    override fun localDateTimeToInstant(dateTime: LocalDateTime, preferred: UtcOffset?): Instant =
        calculations.localDateTimeToInstant(dateTime, preferred)

    override fun offsetAtImpl(instant: Instant): UtcOffset = calculations.offsetAtImpl(instant)
}


internal actual fun RuleBasedTimeZoneCalculations.asTimeZone(): TimeZone = RuleBasedTimeZone(this)

internal actual fun FixedOffsetTimeZone.Companion.withSpecificName(offset: UtcOffset, id: String): FixedOffsetTimeZone =
    FixedOffsetTimeZone(offset, id)
