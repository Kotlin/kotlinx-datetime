/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.TimeZoneIdProvider
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.JvmTimeZone
import kotlinx.datetime.TimeZone
import kotlinx.datetime.TimeZoneDatabase
import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZoneId.getAvailableZoneIds

internal actual fun currentSystemDefaultTimeZone(): TimeZone =
    systemTimezoneDatabase.get(systemTimeZoneIdProvider.currentTimeZoneId())

internal actual val timeZoneDatabaseImpl: TimeZoneDatabase = object: TimeZoneDatabase {
    override fun get(id: String): TimeZone = try {
        JvmTimeZone(ZoneId.of(id))
    } catch (e: Exception) {
        if (e is DateTimeException) throw IllegalTimeZoneException(e)
        throw e
    }

    override fun getOrNull(id: String): TimeZone? = try {
        JvmTimeZone(ZoneId.of(id))
    } catch (e: Exception) {
        if (e is DateTimeException) null else throw e
    }

    override fun availableZoneIds(): Set<String> = getAvailableZoneIds()
}

internal actual val systemTimeZoneIdProvider: TimeZoneIdProvider = object: TimeZoneIdProvider {
    override fun currentTimeZoneId(): String = java.util.TimeZone.getDefault().id
}
