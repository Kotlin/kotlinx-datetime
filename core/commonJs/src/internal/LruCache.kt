/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal actual class LruCache<K, V> actual constructor(private val size: Int, private val create: (K) -> V) {
    private val cache = HashMap<K, V>()
    private val history = HashMap<K, Int>()
    private var counter = 0

    actual fun get(key: K): V {
        history[key] = ++counter
        return cache.getOrPut(key) {
            if (history.size > size) {
                val oldest = history.minByOrNull { it.value }!!.key
                history.remove(oldest)
                cache.remove(oldest)
            }
            create(key)
        }
    }
}
