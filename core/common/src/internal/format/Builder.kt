/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format

internal abstract class BuilderSpec<in T>(
    val subBuilders: Map<String, BuilderSpec<T>>,
    val formats: Map<Char, (Int) -> FormatStructure<T>>,
)

internal class AppendableFormatStructure<T>(
    private val spec: BuilderSpec<T>
) {
    private var builder = ConcatenatedFormatStructureBuilder<T>()
    fun build(): ConcatenatedFormatStructure<T> = builder.build()
    fun add(format: FormatStructure<T>) {
        when (format) {
            is NonConcatenatedFormatStructure -> builder.add(format)
            is ConcatenatedFormatStructure -> format.formats.forEach { builder.add(it) }
        }
    }

    fun formatFromSubBuilder(
        name: String,
        block: AppendableFormatStructure<*>.() -> Unit
    ): FormatStructure<T>? =
        spec.subBuilders[name]?.let { subSpec ->
            AppendableFormatStructure(subSpec).apply(block).build()
        }

    fun formatFromDirective(letter: Char, length: Int): FormatStructure<T>? =
        spec.formats[letter]?.invoke(length)

    fun createSibling(): AppendableFormatStructure<T> = AppendableFormatStructure(spec)

    private class ConcatenatedFormatStructureBuilder<T>(
        private val list: MutableList<NonConcatenatedFormatStructure<T>> = mutableListOf()
    ) {
        fun add(element: NonConcatenatedFormatStructure<T>) = list.add(element)
        fun build(): ConcatenatedFormatStructure<T> = ConcatenatedFormatStructure(list)
    }
}
