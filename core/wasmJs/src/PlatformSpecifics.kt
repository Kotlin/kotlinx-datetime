/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

import kotlinx.datetime.internal.JSJoda.ZoneId

internal actual fun getAvailableZoneIdsSet(): Set<String> {
    val result = mutableSetOf<String>()
    val ids = ZoneId.getAvailableZoneIds().unsafeCast<JsArray<JsString>>()
    for (i in 0 until ids.length) {
        result.add(ids[i].toString())
    }
    return result
}

public actual typealias InteropInterface = JsAny

@Target(AnnotationTarget.FILE)
public actual annotation class JsNonModule