/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format.parser

internal class ConcatenatedListView<T>(val list1: List<T>, val list2: List<T>) : AbstractList<T>() {
    override val size: Int
        get() = list1.size + list2.size

    override fun get(index: Int): T = if (index < list1.size) list1[index] else list2[index - list1.size]

    override fun iterator(): Iterator<T> = ConcatenatedListViewIterator()

    private inner class ConcatenatedListViewIterator : Iterator<T> {
        private val iterators: List<Iterator<T>> = buildList {
            collectIterators(list1)
            collectIterators(list2)
        }
        private var index = 0

        private fun MutableList<Iterator<T>>.collectIterators(list: List<T>) {
            if (list is ConcatenatedListView<T>) {
                collectIterators(list.list1)
                collectIterators(list.list2)
            } else {
                add(list.iterator())
            }
        }

        override fun hasNext(): Boolean {
            while (index < iterators.size && !iterators[index].hasNext()) {
                index++
            }
            return index < iterators.size
        }

        override fun next(): T = iterators[index].next()
    }
}
