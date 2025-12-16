/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.TimeZoneIdProvider
import kotlinx.datetime.TimeZone
import kotlinx.datetime.TimeZoneDatabase
import kotlinx.datetime.asTimeZone

private val tzdbInRegistry = runCatching { TzdbInRegistry() }

internal actual fun currentSystemDefaultTimeZone(): TimeZone = with(currentSystemDefaultFromRegistry()) {
    when {
        isConstantOffset -> offset.asTimeZone("GMT")
        windowsName == WindowsUtcString -> TimeZone.UTC
        else -> {
            val ianaTzName = windowsToStandard[windowsName]
                ?: throw IllegalStateException("Unknown time zone name '$windowsName'")
            tzdbInRegistry.getOrThrow().get(ianaTzName)
        }
    }
}

internal actual val timeZoneDatabaseImpl: TimeZoneDatabase = object: TimeZoneDatabase {
    override fun get(id: String): TimeZone = tzdbInRegistry.getOrThrow().get(id)
    override fun getOrNull(id: String): TimeZone? = tzdbInRegistry.getOrThrow().getOrNull(id)
    override fun availableZoneIds(): Set<String> = tzdbInRegistry.getOrThrow().availableZoneIds()
}

internal actual val systemTimeZoneIdProvider: TimeZoneIdProvider = object: TimeZoneIdProvider {
    override fun currentTimeZoneId(): String = with(currentSystemDefaultFromRegistry()) {
        when {
            windowsName == WindowsUtcString -> "UTC"
            else -> windowsToStandard[windowsName]
                    ?: throw IllegalStateException("Unknown time zone name '$windowsName'")
        }
    }
}

private const val WindowsUtcString = "Coordinated Universal Time"
