/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal expect class LruCache<K, V>(size: Int, create: (K) -> V) {
    fun get(key: K): V
}