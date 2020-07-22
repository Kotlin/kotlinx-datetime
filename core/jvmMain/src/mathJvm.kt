/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

internal actual fun safeMultiply(a: Long, b: Long): Long = Math.multiplyExact(a, b)
internal actual fun safeMultiply(a: Int, b: Int): Int = Math.multiplyExact(a, b)
internal actual fun safeAdd(a: Int, b: Int): Int = Math.addExact(a, b)
internal actual fun safeAdd(a: Long, b: Long): Long = Math.addExact(a, b)
