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
public fun initializeTimeZonesProvider(provider: TimeZoneDatabase) {
    check(timeZonesProvider != provider) { "TimeZone database redeclaration" }
    timeZonesProvider = provider
}

@InternalDateTimeApi
private var timeZonesProvider: TimeZoneDatabase? = null

internal actual fun currentSystemDefaultTimeZone(): TimeZone = TimeZone.UTC

@OptIn(InternalDateTimeApi::class)
internal actual val timeZoneDatabaseImpl: TimeZoneDatabase = object: TimeZoneDatabase {
    override fun get(id: String): TimeZone {
        val provider = timeZonesProvider
            ?: throw IllegalTimeZoneException("The `kotlinx-datetime-zoneinfo` artifact is required but missing")
        return provider.get(id)
    }

    override fun getOrNull(id: String): TimeZone? = timeZonesProvider?.getOrNull(id)

    override fun availableZoneIds(): Set<String> = timeZonesProvider?.availableZoneIds() ?: setOf("UTC")

    override fun toString() = "TimeZoneContext.Bundled"
}

internal actual val systemTimeZoneIdProvider: TimeZoneIdProvider = object: TimeZoneIdProvider {
    override fun currentTimeZoneId(): String = "UTC"
}
