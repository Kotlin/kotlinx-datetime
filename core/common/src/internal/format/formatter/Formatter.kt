/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format.formatter

internal interface FormatterStructure<in T> {
    fun format(obj: T, builder: Appendable, minusNotRequired: Boolean = false)
}

internal class SpacePaddedFormatter<in T>(
    private val formatter: FormatterStructure<T>,
    private val padding: Int,
): FormatterStructure<T> {
    override fun format(obj: T, builder: Appendable, minusNotRequired: Boolean) {
        val string = StringBuilder().let {
            formatter.format(obj, it, minusNotRequired)
            it.toString()
        }
        repeat(padding - string.length) { builder.append(' ') }
        builder.append(string)
    }
}

internal class ConditionalFormatter<in T>(
    private val formatters: List<Pair<T.() -> Boolean, FormatterStructure<T>>>
): FormatterStructure<T> {
    override fun format(obj: T, builder: Appendable, minusNotRequired: Boolean) {
        for ((condition, formatter) in formatters) {
            if (obj.condition()) {
                formatter.format(obj, builder, minusNotRequired)
                return
            }
        }
    }
}

internal class SignedFormatter<in T>(
    private val formatter: FormatterStructure<T>,
    private val allSubFormatsNegative: T.() -> Boolean,
    private val alwaysOutputSign: Boolean,
): FormatterStructure<T> {
    override fun format(obj: T, builder: Appendable, minusNotRequired: Boolean) {
        val sign = if (!minusNotRequired && obj.allSubFormatsNegative()) {
            '-'
        } else if (alwaysOutputSign) {
            '+'
        } else {
            null
        }
        sign?.let { builder.append(it) }
        formatter.format(obj, builder, minusNotRequired = minusNotRequired || sign == '-')
    }
}

internal class ConcatenatedFormatter<in T>(
    private val formatters: List<FormatterStructure<T>>,
): FormatterStructure<T> {
    override fun format(obj: T, builder: Appendable, minusNotRequired: Boolean) {
        for (formatter in formatters) {
            formatter.format(obj, builder, minusNotRequired)
        }
    }
}

