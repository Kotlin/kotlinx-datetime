/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format

internal class AppendableFormatStructure<T> {
    private val list: MutableList<NonConcatenatedFormatStructure<T>> = mutableListOf()
    fun build(): ConcatenatedFormatStructure<T> = ConcatenatedFormatStructure(list)
    fun add(format: FormatStructure<T>) {
        when (format) {
            is NonConcatenatedFormatStructure -> list.add(format)
            is ConcatenatedFormatStructure -> format.formats.forEach { list.add(it) }
        }
    }
}
