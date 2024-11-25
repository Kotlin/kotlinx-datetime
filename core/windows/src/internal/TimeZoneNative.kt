/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.TimeZone

internal actual fun timeZoneById(zoneId: String): TimeZone =
    RegionTimeZone(tzdbInRegistry.getOrThrow().rulesForId(zoneId), zoneId)

internal actual fun getAvailableZoneIds(): Set<String> =
    tzdbInRegistry.getOrThrow().availableTimeZoneIds()

internal actual fun currentSystemDefaultZone(): Pair<String, TimeZone?> =
    tzdbInRegistry.getOrThrow().currentSystemDefault()

private val tzdbInRegistry = runCatching { TzdbInRegistry() }
