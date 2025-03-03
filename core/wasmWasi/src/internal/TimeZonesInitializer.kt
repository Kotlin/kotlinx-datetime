/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone

@RequiresOptIn
internal annotation class InternalDateTimeApi

/*
This is internal API which is not intended to use on user-side.
 */
@InternalDateTimeApi
public interface TimeZonesProvider {
    public fun  zoneDataByName(name: String): ByteArray
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

@OptIn(InternalDateTimeApi::class)
internal actual fun timeZoneById(zoneId: String): TimeZone {
    val data = timeZonesProvider?.zoneDataByName(zoneId)
        ?: throw IllegalTimeZoneException("TimeZones are not supported")
    val rules = readTzFile(data).toTimeZoneRules()
    return RegionTimeZone(rules, zoneId)
}

@OptIn(InternalDateTimeApi::class)
internal actual fun getAvailableZoneIds(): Set<String> =
    timeZonesProvider?.getTimeZones() ?: setOf("UTC")

internal actual fun currentSystemDefaultZone(): Pair<String, TimeZone?> = "UTC" to null
