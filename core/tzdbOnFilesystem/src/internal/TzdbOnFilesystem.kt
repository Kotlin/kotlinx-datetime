/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import kotlinx.cinterop.refTo
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZoneDatabase
import kotlinx.datetime.TimeZone
import platform.posix.SEEK_END
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.size_t

internal class TzdbOnFilesystem(defaultTzdbPath: Path? = null): TimeZoneDatabase {

    internal val tzdbPath = tzdbPaths(defaultTzdbPath).find { path ->
        tabPaths.any { path.containsFile(it) }
    } ?: throw IllegalStateException("Could not find the path to the timezone database")

    /**
     * The timezone database version, included in [toString] for debugging purposes.
     *
     * It's lazily computed, as it's only useful during debugging.
     * We ignore any failures that happen while computing it, as it's merely a nice-to-have thing.
     */
    @OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
    private val tzdataHeader = lazy {
        runCatching {
            // `tzdata.zi` contains a header of the form `# version SOMETHING`, which we want to show during debugging.
            // MacOS also includes the `+VERSION` file with just `SOMETHING` as its contents, but it's less portable
            val tzdata = Path(tzdbPath.isAbsolute, tzdbPath.components + "tzdata.zi")
            val handler = fopen(tzdata.toString(), "rb") ?: return@runCatching null
            try {
                var err = fseek(handler, 0, SEEK_END)
                if (err == -1) return@runCatching null
                val size = 32 // more than enough to fit "# version 2026f-rearguard\n"
                val buffer = ByteArray(size)
                val readAmount = fread(buffer.refTo(0), size.convert<size_t>(), 1u, handler)
                if (readAmount.convert<ULong>() == 1uL) {
                    buffer.decodeToString().lineSequence().first()
                } else {
                    null
                }
            } finally {
                fclose(handler)
            }
        }.getOrNull()
    }

    override fun get(id: String): TimeZone {
        if (id.length <= 1) { throw IllegalTimeZoneException("Timezone ID '$id' is not valid") }
        val idAsPath = Path.fromString(id)
        if (idAsPath.isAbsolute) { throw IllegalTimeZoneException("Timezone ID '$idAsPath' must not begin with a '/'") }
        if (idAsPath.components.any { it == ".." }) {
            throw IllegalTimeZoneException("'$idAsPath' must not contain '..' as a component")
        }
        val file = Path(tzdbPath.isAbsolute, tzdbPath.components + idAsPath.components)
        val contents = file.readBytes() ?: throw IllegalTimeZoneException("File '$file' not found")
        return RuleBasedTimeZone(readTzFileToRules(contents), id, this)
    }

    override fun getOrNull(id: String): TimeZone? {
        val idAsPath = Path.fromString(id)
        if (idAsPath.isAbsolute) { return null }
        if (idAsPath.components.any { it == ".." }) { return null }
        val file = Path(tzdbPath.isAbsolute, tzdbPath.components + idAsPath.components)
        val contents = file.readBytes() ?: return null
        return RuleBasedTimeZone(readTzFileToRules(contents), id, this)
    }

    override fun availableZoneIds(): Set<String> = buildSet {
        val _ = tzdbPath.tryTraverseDirectory(exclude = tzdbUnneededFiles) { add(it.toString()) }
    }

    override fun toString(): String = "TzdbOnFilesystem(tzdbPath=$tzdbPath,tzdataHeader=$tzdataHeader)"
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
    currentSystemTimeZonePath?.splitTimeZonePath()?.first?.let { yield(it) }
}

internal var systemTimezoneSearchRoot: String = "/"

internal val currentSystemTimeZonePath get() = chaseSymlinks("${systemTimezoneSearchRoot}etc/localtime")

/**
 * Given a path like `/usr/share/zoneinfo/Europe/Berlin`, produces `/usr/share/zoneinfo to Europe/Berlin`.
 * Returns null if the function can't recognize the boundary between the time zone and the tzdb.
 */
// taken from https://github.com/HowardHinnant/date/blob/ab37c362e35267d6dee02cb47760f9e9c669d3be/src/tz.cpp#L3951-L3952
internal fun Path.splitTimeZonePath(): Pair<Path, Path>? {
    val i = components.indexOf("zoneinfo")
    if (!isAbsolute || i == -1 || i == components.size - 1) return null
    return Pair(
        Path(true, components.subList(0, i + 1)),
        Path(false, components.subList(i + 1, components.size))
    )
}
