/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal class TzdbOnFilesystem(defaultTzdbPath: Path? = null): TimeZoneDatabase {

    internal val tzdbPath = tzdbPaths(defaultTzdbPath).find { path ->
        tabPaths.any { path.containsFile(it) }
    } ?: throw IllegalStateException("Could not find the path to the timezone database")

    override fun rulesForId(id: String): TimeZoneRules {
        val idAsPath = Path.fromString(id)
        require(!idAsPath.isAbsolute) { "Timezone ID '$idAsPath' must not begin with a '/'" }
        require(idAsPath.components.none { it == ".." }) { "'$idAsPath' must not contain '..' as a component" }
        val file = Path(tzdbPath.isAbsolute, tzdbPath.components + idAsPath.components)
        val contents = file.readBytes() ?: throw RuntimeException("File '$file' not found")
        return readTzFile(contents).toTimeZoneRules()
    }

    override fun availableTimeZoneIds(): Set<String> = buildSet {
        tzdbPath.tryTraverseDirectory(exclude = tzdbUnneededFiles) { add(it.toString()) }
    }

}

// taken from https://github.com/tzinfo/tzinfo/blob/9953fc092424d55deaea2dcdf6279943f3495724/lib/tzinfo/data_sources/zoneinfo_data_source.rb#L354
private val tabPaths = listOf("zone1970.tab", "zone.tab", "tab/zone_sun.tab")

/** The files that sometimes lie in the `zoneinfo` directory but aren't actually time zones. */
private val tzdbUnneededFiles: Regex = Regex(
    // taken from https://github.com/tzinfo/tzinfo/blob/9953fc092424d55deaea2dcdf6279943f3495724/lib/tzinfo/data_sources/zoneinfo_data_source.rb#L88C29-L97C21
    "\\+VERSION|leapseconds|localtime|posix|posixrules|right|SECURITY|src|timeconfig|" +
    // replicating https://github.com/tzinfo/tzinfo/blob/9953fc092424d55deaea2dcdf6279943f3495724/lib/tzinfo/data_sources/zoneinfo_data_source.rb#L442
    ".*\\..*|" +
    // taken from https://github.com/HowardHinnant/date/blob/ab37c362e35267d6dee02cb47760f9e9c669d3be/src/tz.cpp#L2863-L2874
    "Factory"
)

/** The directories checked for a valid timezone database. */
internal fun tzdbPaths(defaultTzdbPath: Path?) = sequence {
    defaultTzdbPath?.let { yield(it) }
    // taken from https://github.com/tzinfo/tzinfo/blob/9953fc092424d55deaea2dcdf6279943f3495724/lib/tzinfo/data_sources/zoneinfo_data_source.rb#L70
    yieldAll(listOf("/usr/share/zoneinfo", "/usr/share/lib/zoneinfo", "/etc/zoneinfo").map { Path.fromString(it) })
    pathToSystemDefault()?.first?.let { yield(it) }
}

// taken from https://github.com/HowardHinnant/date/blob/ab37c362e35267d6dee02cb47760f9e9c669d3be/src/tz.cpp#L3951-L3952
internal fun pathToSystemDefault(): Pair<Path, Path>? {
    val info = chaseSymlinks("/etc/localtime") ?: return null
    val i = info.components.indexOf("zoneinfo")
    if (!info.isAbsolute || i == -1 || i == info.components.size - 1) return null
    return Pair(
        Path(true, info.components.subList(0, i + 1)),
        Path(false, info.components.subList(i + 1, info.components.size))
    )
}
