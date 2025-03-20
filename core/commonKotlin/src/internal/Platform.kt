/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.TimeZone

internal expect fun timeZoneById(zoneId: String): TimeZone

internal expect fun getAvailableZoneIds(): Set<String>

internal expect fun currentSystemDefaultZone(): Pair<String, TimeZone?>
