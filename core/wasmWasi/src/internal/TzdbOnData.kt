/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.internal.tzData.getTimeZones
import kotlinx.datetime.internal.tzData.zoneDataByName

internal class TzdbOnData: TimezoneDatabase {
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun rulesForId(id: String): TimeZoneRules =
        readTzFile(zoneDataByName(id).toByteArray()).toTimeZoneRules()

    override fun availableTimeZoneIds(): Set<String> = getTimeZones()
}

internal actual fun currentSystemDefaultZone(): RegionTimeZone =
    RegionTimeZone(systemTzdb.rulesForId("UTC"), "UTC")

internal actual val systemTzdb: TimezoneDatabase = TzdbOnData()