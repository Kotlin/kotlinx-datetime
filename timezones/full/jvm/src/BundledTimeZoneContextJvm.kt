/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.zoneinfo

import java.io.InputStream

internal actual fun zoneDataByNameOrNull(name: String): ByteArray? {
    // Forbid reading unrelated resources to avoid traversing arbitrary resources +
    // improve performance for clearly faulty timezone names
    if (name !in kotlinx.datetime.timezones.tzData.timeZones)
        return null
    (BundledTimeZoneContext.javaClass.classLoader.tzResourceByName(name) ?: return null).use { resource ->
        return resource.readBytes()
    }
}

/**
 * May be different from [kotlinx.datetime.timezones.tzData.timeZones]
 * if the user removed some of the resources from the classpath when packaging the application.
 * We are not documenting this behavior yet and only implement it for graceful handling of the case,
 * because it is not very ergonomic.
 * If the use case of trimming down timezone databases is popular,
 * we should support it more directly,
 * including the way to strip away historical data *or* add timezones that are not listed in
 * [kotlinx.datetime.timezones.tzData.timeZones].
 */
internal actual val timeZones: Set<String> by lazy {
    // First, a happy non-allocating path, for cases when no resources are missing;
    // as a fallback, start again and allocate the updated set.
    val cls = BundledTimeZoneContext.javaClass.classLoader
    kotlinx.datetime.timezones.tzData.timeZones.takeIf {
        it.all { name -> cls.tzResourceExists(name) }
    } ?: kotlinx.datetime.timezones.tzData.timeZones.filter {
        cls.tzResourceExists(it)
    }.toSet()
}

private fun ClassLoader.tzResourceByName(zoneId: String): InputStream? =
    getResourceAsStream("tzdb/$zoneId")

private fun ClassLoader.tzResourceExists(zoneId: String): Boolean =
    getResource("tzdb/$zoneId") != null
