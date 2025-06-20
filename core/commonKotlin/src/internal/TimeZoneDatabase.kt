/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal interface TimeZoneDatabase {
    fun rulesForId(id: String): TimeZoneRules
    fun availableTimeZoneIds(): Set<String>
}
