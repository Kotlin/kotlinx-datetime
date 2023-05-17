/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal actual class LruCache<K: Any, V: Any> actual constructor(private val size: Int, private val create: (K) -> V) {
    private val marker = Any()
    @Suppress("UNCHECKED_CAST")
    actual fun get(key: K): V {
        val cache: SingleThreadedLru<K, V> =
            Caches.mappings.getOrPut(marker) { SingleThreadedLru(size, create) } as SingleThreadedLru<K, V>
        return cache.get(key)
    }
}

private class SingleThreadedLru<K, V>(val size: Int, val create: (K) -> V) {
    private val cache = HashMap<K, V>()
    private val history = HashMap<K, Int>()
    private var counter = 0

    fun get(key: K): V {
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

@ThreadLocal
private object Caches {
    val mappings = mutableMapOf<Any, SingleThreadedLru<*, *>>()
}
