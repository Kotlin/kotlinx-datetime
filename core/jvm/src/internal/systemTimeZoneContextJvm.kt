/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.TimeZoneIdProvider
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.TimeZoneDatabase
import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZoneId.getAvailableZoneIds
import java.time.ZoneId.systemDefault

internal actual fun currentSystemDefaultTimeZone(): TimeZone =
    TimeZone.ofZone(systemDefault())

internal actual val systemTimezoneDatabase: TimeZoneDatabase = object: TimeZoneDatabase {
    override fun get(id: String): TimeZone = try {
        TimeZone.ofZone(ZoneId.of(if (id == "z") "Z" else id))
    } catch (e: Exception) {
        if (e is DateTimeException) throw IllegalTimeZoneException(e)
        throw e
    }

    override fun getOrNull(id: String): TimeZone? = try {
        TimeZone.ofZone(ZoneId.of(if (id == "z") "Z" else id))
    } catch (e: Exception) {
        if (e is DateTimeException) null else throw e
    }

    override fun availableZoneIds(): Set<String> = getAvailableZoneIds()
}

internal actual val systemTimeZoneIdProvider: TimeZoneIdProvider = object: TimeZoneIdProvider {
    override fun currentTimeZoneId(): String = systemDefault().id
}
