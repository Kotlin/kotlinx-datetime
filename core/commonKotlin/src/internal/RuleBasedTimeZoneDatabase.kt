/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.TimeZone
import kotlinx.datetime.TimeZoneDatabase
import kotlinx.datetime.IllegalTimeZoneException

internal interface RuleBasedTimeZoneDatabase: TimeZoneDatabase {
    fun rulesForIdOrNull(id: String): TimeZoneRulesCommon?
    fun rulesForId(id: String): TimeZoneRulesCommon =
        rulesForIdOrNull(id) ?: throw IllegalTimeZoneException("Unknown time zone $id")

    override fun get(id: String): TimeZone = RuleBasedTimeZone(rulesForId(id), id)
    override fun getOrNull(id: String): TimeZone? = rulesForIdOrNull(id)?.let { RuleBasedTimeZone(it, id) }
}
