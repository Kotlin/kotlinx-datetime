/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime.internal

import kotlinx.datetime.internal.JSJoda.ZoneId

internal actual fun getAvailableZoneIdsSet(): Set<String> = buildSet {
    val ids = ZoneId.getAvailableZoneIds().unsafeCast<JsArray<JsString>>()
    for (i in 0 until ids.length) {
        add(ids[i].toString())
    }
}

public actual typealias InteropInterface = JsAny

public actual typealias JsModule = kotlin.js.JsModule