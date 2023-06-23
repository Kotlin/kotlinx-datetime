/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal class TzdbOnFilesystem(defaultTzdbPath: Path) {

    internal fun rulesForId(id: String): TimeZoneRules =
        readTzFile(tzdbPath.resolve(Path.fromString(id)).readBytes()).toTimeZoneRules()

    internal fun availableTimeZoneIds(): Set<String> = buildSet {
        tzdbPath.traverseDirectory(exclude = tzdbUnneededFiles) { add(it.toString()) }
    }

    internal fun currentSystemDefault(): Pair<Path, Path>? {
        val info = Path(true, listOf("etc", "localtime")).readLink() ?: return null
        val i = info.components.indexOf("zoneinfo")
        if (!info.isAbsolute || i == -1 || i == info.components.size - 1) return null
        return Pair(
            Path(true, info.components.subList(0, i + 1)),
            Path(false, info.components.subList(i + 1, info.components.size))
        )
    }

    private val tzdbPath = defaultTzdbPath.check()?.let { defaultTzdbPath }
        ?: currentSystemDefault()?.first ?: throw IllegalStateException("Could not find the path to the timezone database")

}

private val tzdbUnneededFiles = setOf(
    "posix",
    "posixrules",
    "Factory",
    "iso3166.tab",
    "right",
    "+VERSION",
    "zone.tab",
    "zone1970.tab",
    "tzdata.zi",
    "leapseconds",
    "leap-seconds.list"
)
