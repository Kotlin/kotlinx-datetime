/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal actual fun currentSystemDefaultZone(): RegionTimeZone {
    val zoneId = pathToSystemDefault()?.second?.toString()
        ?: throw IllegalStateException("Failed to get the system timezone")
    return RegionTimeZone(systemTzdb.rulesForId(zoneId), zoneId)
}

internal actual fun getTzdbPath(): Path {
    val defaultPath = Path.fromString("/usr/share/zoneinfo")
    return defaultPath.check()?.let { defaultPath }
            ?: pathToSystemDefault()?.first ?: throw IllegalStateException("Could not find the path to the timezone database")
}

private fun pathToSystemDefault(): Pair<Path, Path>? {
    val info = Path(true, listOf("etc", "localtime")).readLink() ?: return null
    val i = info.components.indexOf("zoneinfo")
    if (!info.isAbsolute || i == -1 || i == info.components.size - 1) return null
    return Pair(
            Path(true, info.components.subList(0, i + 1)),
            Path(false, info.components.subList(i + 1, info.components.size))
    )
}
