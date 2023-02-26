/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format.formatter

internal sealed interface FormatterStructure<in T> {
    fun format(obj: T, builder: StringBuilder, minusNotRequired: Boolean = false)
}

internal sealed interface NonConditionalFormatterStructure<in T>: FormatterStructure<T>

internal class BasicFormatter<in T>(
    private val operation: FormatterOperation<T>,
): NonConditionalFormatterStructure<T> {
    override fun format(obj: T, builder: StringBuilder, minusNotRequired: Boolean) =
        operation.format(obj, builder, minusNotRequired)
}

internal class ConditionalFormatter<in T>(
    private val formatters: List<Pair<T.() -> Boolean, FormatterStructure<T>>>
): FormatterStructure<T> {
    override fun format(obj: T, builder: StringBuilder, minusNotRequired: Boolean) {
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
    override fun format(obj: T, builder: StringBuilder, minusNotRequired: Boolean) {
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
): NonConditionalFormatterStructure<T> {
    override fun format(obj: T, builder: StringBuilder, minusNotRequired: Boolean) {
        for (formatter in formatters) {
            formatter.format(obj, builder, minusNotRequired)
        }
    }
}

