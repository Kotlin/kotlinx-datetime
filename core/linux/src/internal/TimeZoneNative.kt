/*
 * Copyright 2019-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.IllegalTimeZoneException

internal actual val systemTzdb: TimeZoneDatabase get() = tzdb.getOrThrow()

private val tzdb = runCatching { TzdbOnFilesystem() }

internal actual fun currentSystemDefaultZone(): Pair<String, TimeZoneRules?> {
    // according to https://www.man7.org/linux/man-pages/man5/localtime.5.html, when there is no symlink, UTC is used
    val zonePath = currentSystemTimeZonePath ?: return "Z" to null
    val zoneId = zonePath.splitTimeZonePath()?.second?.toString()
        ?: throw IllegalTimeZoneException("Could not determine the timezone ID that `$zonePath` corresponds to")
    return zoneId to null
}
