/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.zoneinfo

internal actual fun zoneDataByNameOrNull(name: String): ByteArray? =
    kotlinx.datetime.timezones.tzData.zoneDataByNameOrNull(name)

internal actual val timeZones: Set<String> get() =
    kotlinx.datetime.timezones.tzData.timeZones
