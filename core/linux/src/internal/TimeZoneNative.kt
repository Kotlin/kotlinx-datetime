/*
 * Copyright 2019-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal actual val systemTzdb: TimeZoneDatabase get() = tzdb.getOrThrow()

private val tzdb = runCatching { TzdbOnFilesystem() }

internal actual fun currentSystemDefaultZone(): Pair<String, TimeZoneRules?> {
    val zoneId = pathToSystemDefault()?.second?.toString()
        ?: throw IllegalStateException("Failed to get the system timezone")
    return zoneId to null
}
