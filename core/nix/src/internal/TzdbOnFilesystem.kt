/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal class TzdbOnFilesystem(val tzdbPath: Path): TimezoneDatabase {

    override fun rulesForId(id: String): TimeZoneRules =
        readTzFile(tzdbPath.resolve(Path.fromString(id)).readBytes()).toTimeZoneRules()

    override fun availableTimeZoneIds(): Set<String> = buildSet {
        tzdbPath.traverseDirectory(exclude = tzdbUnneededFiles) { add(it.toString()) }
    }

}

/** The files that sometimes lie in the `zoneinfo` directory but aren't actually time zones. */
private val tzdbUnneededFiles = setOf(
    // taken from https://github.com/tzinfo/tzinfo/blob/9953fc092424d55deaea2dcdf6279943f3495724/lib/tzinfo/data_sources/zoneinfo_data_source.rb#L88C29-L97C21
    "+VERSION",
    "leapseconds",
    "localtime",
    "posix",
    "posixrules",
    "right",
    "SECURITY",
    "src",
    "timeconfig",
    // taken from https://github.com/HowardHinnant/date/blob/ab37c362e35267d6dee02cb47760f9e9c669d3be/src/tz.cpp#L2863-L2874
    "Factory",
    "iso3166.tab",
    "zone.tab",
    "zone1970.tab",
    "tzdata.zi",
    "leap-seconds.list"
)

/** If the platform has a preference for a specific timezone database path, this field contains it. */
internal expect fun defaultTzdbPath(): String?

/** The directories checked for a valid timezone database. */
private val tzdbPaths = sequence {
    defaultTzdbPath()?.let { yield(Path.fromString(it)) }
    // taken from https://github.com/tzinfo/tzinfo/blob/9953fc092424d55deaea2dcdf6279943f3495724/lib/tzinfo/data_sources/zoneinfo_data_source.rb#L70
    yieldAll(listOf("/usr/share/zoneinfo", "/usr/share/lib/zoneinfo", "/etc/zoneinfo").map { Path.fromString(it) })
    pathToSystemDefault()?.first?.let { yield(it) }
}

// taken from https://github.com/HowardHinnant/date/blob/ab37c362e35267d6dee02cb47760f9e9c669d3be/src/tz.cpp#L3951-L3952
internal fun pathToSystemDefault(): Pair<Path, Path>? {
    val info = Path(true, listOf("etc", "localtime")).chaseSymlinks().first
    val i = info.components.indexOf("zoneinfo")
    if (!info.isAbsolute || i == -1 || i == info.components.size - 1) return null
    return Pair(
        Path(true, info.components.subList(0, i + 1)),
        Path(false, info.components.subList(i + 1, info.components.size))
    )
}

internal actual val systemTzdb: TimezoneDatabase = tzdbPaths.find {
    it.chaseSymlinks().second?.isDirectory == true
}?.let { TzdbOnFilesystem(it) } ?: throw IllegalStateException("Could not find the path to the timezone database")
