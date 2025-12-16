/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.TimeZoneIdProvider
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.TimeZoneDatabase

@RequiresOptIn
internal annotation class InternalDateTimeApi

/*
This is internal API which is not intended to use on user-side.
 */
@InternalDateTimeApi
public interface TimeZonesProvider {
    public fun zoneDataByName(name: String): ByteArray
    public fun getTimeZones(): Set<String>
}

/*
This is internal API which is not intended to use on user-side.
 */
@InternalDateTimeApi
public fun initializeTimeZonesProvider(provider: TimeZonesProvider) {
    check(timeZonesProvider != provider) { "TimeZone database redeclaration" }
    timeZonesProvider = provider
}

@InternalDateTimeApi
private var timeZonesProvider: TimeZonesProvider? = null

internal actual fun currentSystemDefaultTimeZone(): TimeZone = TimeZone.UTC

@OptIn(InternalDateTimeApi::class)
internal actual val timeZoneDatabaseImpl: TimeZoneDatabase = object: RuleBasedTimeZoneDatabase {
    override fun rulesForId(id: String): TimeZoneRulesCommon {
        val data = timeZonesProvider?.zoneDataByName(id)
            ?: throw IllegalTimeZoneException("The `kotlinx-datetime-zoneinfo` artifact is required but missing")
        return readTzFile(data).toTimeZoneRules()
    }

    override fun rulesForIdOrNull(id: String): TimeZoneRulesCommon? {
        val provider = timeZonesProvider ?: return null
        val data = try {
            provider.zoneDataByName(id)
        } catch (_: Throwable) {
            return null
        }
        return readTzFile(data).toTimeZoneRules()
    }

    override fun availableZoneIds(): Set<String> = timeZonesProvider?.getTimeZones() ?: setOf("UTC")
}

internal actual val systemTimeZoneIdProvider: TimeZoneIdProvider = object: TimeZoneIdProvider {
    override fun currentTimeZoneId(): String = "UTC"
}
