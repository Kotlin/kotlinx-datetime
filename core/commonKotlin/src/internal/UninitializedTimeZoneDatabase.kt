/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.TimeZone
import kotlinx.datetime.TimeZoneDatabase

internal fun tryInitializeTimezoneDatabase(construct: () -> TimeZoneDatabase): TimeZoneDatabase = try {
    construct()
} catch (e: Throwable) {
    UninitializedTimeZoneDatabase(e)
}

internal class UninitializedTimeZoneDatabase(val error: Throwable): TimeZoneDatabase {
    override fun get(id: String): TimeZone = throw error
    // Note: if a timezone database failed to initialize, it's an error, not a problem with a timezone, so we throw.
    override fun getOrNull(id: String): TimeZone = throw error
    override fun availableZoneIds(): Set<String> = throw error
}
