/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*

internal val systemTimezoneDatabase: TimeZoneDatabase =
    FixedOffsetTimeZoneDatabase(timeZoneDatabaseImpl)

internal expect val timeZoneDatabaseImpl: TimeZoneDatabase
internal expect fun currentSystemDefaultTimeZone(): TimeZone
internal expect val systemTimeZoneIdProvider: TimeZoneIdProvider

internal expect fun RuleBasedTimeZoneCalculations.asTimeZone(): TimeZone
internal expect fun FixedOffsetTimeZone.Companion.withSpecificName(offset: UtcOffset, id: String): FixedOffsetTimeZone

internal fun FixedOffsetTimeZone.Companion.withSpecificPrefix(offset: UtcOffset, prefix: String): FixedOffsetTimeZone =
    when (offset.totalSeconds) {
        0 -> FixedOffsetTimeZone.withSpecificName(offset, prefix)
        else -> FixedOffsetTimeZone.withSpecificName(offset, "$prefix$offset")
    }
