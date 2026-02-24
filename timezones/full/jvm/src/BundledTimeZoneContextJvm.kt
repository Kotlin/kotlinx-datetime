/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.zoneinfo

import java.io.InputStream

internal actual fun zoneDataByNameOrNull(name: String): ByteArray? {
    (tzResourceByName(name) ?: return null).use { resource ->
        return resource.readBytes()
    }
}

internal actual val timeZones: Set<String> by lazy {
    kotlinx.datetime.timezones.tzData.timeZones.takeIf {
        it.all { name -> tzResourceExists(name) }
    } ?: kotlinx.datetime.timezones.tzData.timeZones.filter {
        tzResourceExists(it)
    }.toSet()
}

private fun tzResourceByName(zoneId: String): InputStream? =
    BundledTimeZoneContext.javaClass.classLoader.getResourceAsStream("tzdb/$zoneId")

private fun tzResourceExists(zoneId: String): Boolean {
    val resource = tzResourceByName(zoneId)
    return if (resource != null) {
        resource.close()
        true
    } else {
        false
    }
}
