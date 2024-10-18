/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

internal actual fun Throwable.hasJsExceptionName(name: String): Boolean =
    this.asDynamic().name == name

internal actual inline fun <reified T : Any> jsTry(crossinline body: () -> T): T = body()