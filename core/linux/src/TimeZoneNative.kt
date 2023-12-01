/*
 * Copyright 2019-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.*

internal actual val systemTzdb: TimezoneDatabase = TzdbOnFilesystem(Path.fromString("/usr/share/zoneinfo"))

internal actual fun currentSystemDefaultZone(): RegionTimeZone {
    val zoneId = pathToSystemDefault()?.second?.toString()
        ?: throw IllegalStateException("Failed to get the system timezone")
    return RegionTimeZone(systemTzdb.rulesForId(zoneId), zoneId)
}
