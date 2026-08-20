/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.timezones

import kotlinx.datetime.IllegalTimeZoneException

@Suppress("DEPRECATION")
@OptIn(ExperimentalStdlibApi::class)
@EagerInitialization
private val initializeTimeZones = run {
    kotlinx.datetime.internal.initializeTimeZonesProvider(
        object : kotlinx.datetime.internal.TimeZonesProvider {
            override fun zoneDataByName(name: String): ByteArray =
                kotlinx.datetime.timezones.tzData.zoneDataByNameOrNull(name)
                    ?: throw IllegalTimeZoneException("Zone ID '$name' not recognized")
            override fun getTimeZones(): Set<String> =
                kotlinx.datetime.timezones.tzData.timeZones
        }
    )
}
