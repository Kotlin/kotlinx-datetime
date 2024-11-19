/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime.internal

import kotlinx.datetime.internal.JSJoda.ZoneRulesProvider
import kotlin.js.js
import kotlin.js.unsafeCast

private fun getZones(rulesProvider: JsAny): JsAny = js("rulesProvider.getTzdbData().zones")
private fun getLinks(rulesProvider: JsAny): JsAny = js("rulesProvider.getTzdbData().links")
private fun initializeTimeZones(): JsAny = js("require('@js-joda/timezone')")

internal actual fun readTzdb(): Pair<List<String>, List<String>>? = try {
    jsTry {
        initializeTimeZones()
        val zones = getZones(ZoneRulesProvider as JsAny)
        val links = getLinks(ZoneRulesProvider as JsAny)
        zones.unsafeCast<JsArray<JsString>>().toList() to links.unsafeCast<JsArray<JsString>>().toList()
    }
} catch (_: Throwable) {
    null
}

private fun JsArray<JsString>.toList(): List<String> = buildList {
    for (i in 0 until toList@length) {
        add(this@toList[i].toString())
    }
}

public actual typealias InteropInterface = JsAny

public actual typealias JsModule = kotlin.js.JsModule
