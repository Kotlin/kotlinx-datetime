/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.datetime.internal

import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZoneDatabase
import kotlinx.datetime.TimeZone
import kotlin.concurrent.atomics.*
import kotlinx.cinterop.*
import platform.posix.*

internal fun TzdbOnFilesystem(defaultTzdbPath: Path? = null): TimeZoneDatabase =
    CachingTimezoneDatabase(RawTzdbOnFilesystem(defaultTzdbPath))

private class CachingTimezoneDatabase(private val inner: TimeZoneDatabase): TimeZoneDatabase {
    /** Note: some time zones reuse the exact same file data, and recognizing that could save some CPU time and RAM.
    To that end, we *could* add an additional inode->TimeZoneRules cache--if not for the Apple requirement
    to declare all `stat` usages in an application: <https://github.com/Kotlin/kotlinx-datetime/pull/385>.
    There is no reason to expect the requirement to ever be lifted, as inode numbers may be used for fingerprinting. */
    private val nameCache = mutableMapOf<String, Lazy<TimeZone?>>()

    /**
     * A mutex protecting every access to [nameCache].
     *
     * This mutex will leak!
     * The instances created with [TzdbOnFilesystem] outside tests are global singletons,
     * and we can not inject code to just before the program terminates.
     */
    private val globalMutex = nativeHeap.alloc<pthread_mutex_t>().also {
        val result = pthread_mutex_init(it.ptr, null)
        check(result == 0) { "Failed to initialize a mutex" }
    }

    private val cachedAvailableZoneIds by lazy { inner.availableZoneIds() }

    override fun get(id: String): TimeZone {
        getOrNull(id)?.let { return it }
        // `inner.getOrNull()` returned `null`. Call `get()` to determine and throw the actual issue.
        return inner.get(id)
    }

    override fun getOrNull(id: String): TimeZone? {
        // Put in a computation for the time zone, so that other threads can wait for it
        val computation = withGlobalLock {
            nameCache.getOrPut(id) {
                lazy {
                    inner.getOrNull(id)
                }
            }
        }
        // Await/compute/read (depending on the situation)
        val result = runCatching { computation.value }
        result.getOrNull()?.let { return it }
        // The computation failed. Avoid polluting our cache with broken timezone names
        withGlobalLock {
            val _ = nameCache.remove(id)
        }
        // Rethrow a tricky error if there is one
        return result.exceptionOrNull()?.let { throw it }
    }

    override fun availableZoneIds(): Set<String> = cachedAvailableZoneIds

    override fun toString() = "CachingTimezoneDatabase($inner)"

    private inline fun <T> withGlobalLock(action: () -> T): T {
        val error = pthread_mutex_lock(globalMutex.ptr)
        check(error == 0) { "Data race in TzdbOnFilesystem: locking before initializing" }
        try {
            return action()
        } finally {
            val error = pthread_mutex_unlock(globalMutex.ptr)
            check(error == 0) { "Data race in TzdbOnFilesystem: unlocking before initializing" }
        }
    }
}

private class RawTzdbOnFilesystem(defaultTzdbPath: Path? = null): TimeZoneDatabase {
    internal val tzdbPath = tzdbPaths(defaultTzdbPath).find { path ->
        tabPaths.any { path.containsFile(it) }
    } ?: throw IllegalStateException("Could not find the path to the timezone database")


    override fun get(id: String): TimeZone {
        if (id.length <= 1 || tzdbUnneededFiles.matches(id)) {
            throw IllegalTimeZoneException("Timezone ID '$id' is not valid")
        }
        val idAsPath = Path.fromString(id)
        if (idAsPath.isAbsolute) { throw IllegalTimeZoneException("Timezone ID '$idAsPath' must not begin with a '/'") }
        if (idAsPath.components.any { it == ".." }) {
            throw IllegalTimeZoneException("'$idAsPath' must not contain '..' as a component")
        }
        val file = Path(tzdbPath.isAbsolute, tzdbPath.components + idAsPath.components)
        val contents = file.toString().readBytes() ?: throw IllegalTimeZoneException("File '$file' not found")
        return RuleBasedTimeZone(readTzFileToRules(contents), id, this)
    }

    override fun getOrNull(id: String): TimeZone? {
        if (id.length <= 1 || tzdbUnneededFiles.matches(id)) { return null }
        val idAsPath = Path.fromString(id)
        if (idAsPath.isAbsolute) { return null }
        if (idAsPath.components.any { it == ".." }) { return null }
        val file = Path(tzdbPath.isAbsolute, tzdbPath.components + idAsPath.components)
        val contents = file.toString().readBytes() ?: return null
        return RuleBasedTimeZone(readTzFileToRules(contents), id, this)
    }

    override fun availableZoneIds(): Set<String> = buildSet {
        val _ = tzdbPath.tryTraverseDirectory(exclude = tzdbUnneededFiles) { add(it.toString()) }
    }

    override fun toString(): String = "TzdbOnFilesystem(tzdbPath=$tzdbPath)"
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

private fun Path.containsFile(file: String): Boolean = access("$this/$file", F_OK) == 0

private fun Path.tryTraverseDirectory(
    exclude: Regex,
    stripLeadingComponents: Int = this.components.size,
    maxDepth: Int = 100,
    actionOnFile: (Path) -> Unit
): Boolean {
    if (maxDepth <= 0) throw IllegalStateException("Max depth reached: $this")
    val handler = opendir(this.toString()) ?: return false
    try {
        while (true) {
            val entry = readdir(handler) ?: break
            val name = entry.pointed.d_name.toKString()
            if (name == "." || name == "..") continue
            if (exclude.matches(name)) continue
            val path = Path(isAbsolute, components + name)
            val isDirectory = path.tryTraverseDirectory(
                exclude, stripLeadingComponents, maxDepth = maxDepth - 1, actionOnFile
            )
            if (!isDirectory) {
                actionOnFile(Path(false, path.components.drop(stripLeadingComponents)))
            }
        }
    } finally {
        closedir(handler)
    }
    return true
}
