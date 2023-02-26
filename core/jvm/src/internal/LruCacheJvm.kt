/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import java.util.concurrent.*
import java.util.concurrent.atomic.*

internal actual class LruCache<K, V> actual constructor(private val size: Int, private val create: (K) -> V) {
    private val history = ConcurrentHashMap<K, Int>()
    private val cache = ConcurrentHashMap<K, V>()
    private val counter = AtomicInteger()

    actual fun get(key: K): V {
        history[key] = counter.incrementAndGet()
        return cache.getOrPut(key) {
            synchronized(history) {
                if (history.size > size) {
                    val oldest = history.minByOrNull { it.value }!!.key
                    history.remove(oldest)
                    cache.remove(oldest)
                }
                create(key)
            }
        }
    }
}
