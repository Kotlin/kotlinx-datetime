/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

internal fun Long.clampToInt(): Int =
        when {
            this > Int.MAX_VALUE -> Int.MAX_VALUE
            this < Int.MIN_VALUE -> Int.MIN_VALUE
            else -> toInt()
        }


internal expect fun safeMultiply(a: Long, b: Long): Long
internal expect fun safeMultiply(a: Int, b: Int): Int
internal expect fun safeAdd(a: Long, b: Long): Long
internal expect fun safeAdd(a: Int, b: Int): Int
