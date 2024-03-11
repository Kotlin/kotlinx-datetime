/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

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

@OptIn(InternalDateTimeApi::class)
private fun zoneDataByName(name: String): ByteArray =
    timeZonesProvider?.zoneDataByName(name) ?: error("TimeZones are not supported")

@OptIn(InternalDateTimeApi::class)
private fun getTimeZones(): Set<String> =
    timeZonesProvider?.getTimeZones() ?: emptySet()

internal class TzdbOnData: TimeZoneDatabase {
    override fun rulesForId(id: String): TimeZoneRules =
        readTzFile(zoneDataByName(id)).toTimeZoneRules()

    override fun availableTimeZoneIds(): Set<String> = getTimeZones()
}