/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

import kotlinx.datetime.internal.JSJoda.ZoneId

internal actual fun getAvailableZoneIdsSet(): Set<String> =
    ZoneId.getAvailableZoneIds().unsafeCast<Array<String>>().toSet()

public actual external interface InteropInterface

public actual typealias JsNonModule = kotlin.js.JsNonModule