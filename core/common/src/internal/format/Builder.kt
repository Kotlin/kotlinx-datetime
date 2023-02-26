/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format

internal interface Builder<T> {
    fun build(): ConcatenatedFormatStructure<T>
    fun add(format: NonConcatenatedFormatStructure<T>)
    fun formatFromSubBuilder(name: String, block: Builder<*>.() -> Unit): FormatStructure<T>?
    fun formatFromDirective(letter: Char, length: Int): FormatStructure<T>?
    fun createSibling(): Builder<T>
}

internal abstract class AbstractBuilder<T>: Builder<T> {
    private var builder = ConcatenatedFormatStructureBuilder<T>()
    override fun build(): ConcatenatedFormatStructure<T> = builder.build()
    override fun add(format: NonConcatenatedFormatStructure<T>) { builder.add(format) }

    private class ConcatenatedFormatStructureBuilder<T>(
        private val list: MutableList<NonConcatenatedFormatStructure<T>> = mutableListOf()
    ) {
        fun add(element: NonConcatenatedFormatStructure<T>) = list.add(element)
        fun build(): ConcatenatedFormatStructure<T> = ConcatenatedFormatStructure(list)
    }
}

internal fun <T, E : T> Builder<E>.add(format: FormatStructure<T>) = when (format) {
    is NonConcatenatedFormatStructure -> add(format)
    is ConcatenatedFormatStructure -> format.formats.forEach { add(it) }
}

