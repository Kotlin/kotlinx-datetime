/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.IllegalTimeZoneException

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

private val utcData get() = byteArrayOf(
    84,   90,  105,  102,   50,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    1,    0,    0,    0,    1,    0,    0,    0,    0,
    0,    0,    0,   84,   90,  105,  102,   50,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    1,    0,    0,    0,    4,    0,
    0,    0,    0,    0,    0,   85,   84,   67,    0,   10,   85,   84,   67,   48,   10,
)

private val utcNames get() = setOf("Universal", "Etc/Universal", "Etc/Zulu", "Etc/UCT", "Etc/UTC", "Zulu", "UCT", "UTC")

@OptIn(InternalDateTimeApi::class)
internal class TzdbOnData: TimeZoneDatabase {
    override fun rulesForId(id: String): TimeZoneRules {
        val provider = timeZonesProvider
        val data: ByteArray
        if (provider != null) {
            data = provider.zoneDataByName(id)
        } else {
            if (id !in utcNames) throw IllegalTimeZoneException("TimeZones are not supported")
            data = utcData
        }
        return readTzFile(data).toTimeZoneRules()
    }

    override fun availableTimeZoneIds(): Set<String> =
        timeZonesProvider?.getTimeZones() ?: utcNames
}