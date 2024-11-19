/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime.internal

import kotlinx.datetime.internal.JSJoda.ZoneRulesProvider

internal actual fun readTzdb(): Pair<List<String>, List<String>>? = try {
    js("require('@js-joda/timezone')")
    val tzdbData = ZoneRulesProvider.asDynamic().getTzdbData()
    tzdbData.zones.unsafeCast<Array<String>>().toList() to tzdbData.links.unsafeCast<Array<String>>().toList()
} catch (_: Throwable) {
    null
}

public actual external interface InteropInterface

public actual typealias JsNonModule = kotlin.js.JsNonModule

public actual typealias JsModule = kotlin.js.JsModule
