/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format.formatter

import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.POWERS_OF_TEN
import kotlin.math.*

internal interface FormatterOperation<in T> {
    fun format(obj: T, builder: StringBuilder, minusNotRequired: Boolean)
}

internal class ConstantStringFormatterOperation<in T>(
    private val string: String,
): FormatterOperation<T> {
    override fun format(obj: T, builder: StringBuilder, minusNotRequired: Boolean) {
        builder.append(string)
    }
}

internal class UnsignedIntFormatterOperation<in T>(
    private val number: (T) -> Int,
    private val zeroPadding: Int,
): FormatterOperation<T> {

    init {
        require(zeroPadding >= 0)
        require(zeroPadding <= 9)
    }

    override fun format(obj: T, builder: StringBuilder, minusNotRequired: Boolean) {
        val numberStr = number(obj).toString()
        val zeroPaddingStr = '0'.toString().repeat(maxOf(0, zeroPadding - numberStr.length))
        builder.append(zeroPaddingStr, numberStr)
    }
}

internal class SignedIntFormatterOperation<in T>(
    private val number: (T) -> Int,
    private val zeroPadding: Int,
    private val outputPlusOnExceedsPad: Boolean,
): FormatterOperation<T> {

    init {
        require(zeroPadding >= 0)
        require(zeroPadding <= 9)
    }

    override fun format(obj: T, builder: StringBuilder, minusNotRequired: Boolean) {
        val innerBuilder = StringBuilder()
        val number = number(obj).let { if (minusNotRequired && it < 0) -it else it }
        if (number.absoluteValue < POWERS_OF_TEN[zeroPadding - 1]) {
            // needs padding
            if (number >= 0) {
                innerBuilder.append((number + POWERS_OF_TEN[zeroPadding])).deleteAt(0)
            } else {
                innerBuilder.append((number - POWERS_OF_TEN[zeroPadding])).deleteAt(1)
            }
        } else {
            if (outputPlusOnExceedsPad && number >= POWERS_OF_TEN[zeroPadding]) innerBuilder.append('+')
            innerBuilder.append(number)
        }
        builder.append(innerBuilder)
    }
}

internal class DecimalFractionFormatterOperation<in T>(
    private val number: (T) -> DecimalFraction,
    private val minDigits: Int?,
    private val maxDigits: Int?,
): FormatterOperation<T> {

    init {
        require(minDigits == null || minDigits in 1..9)
        require(maxDigits == null || maxDigits in 1..9)
        require(minDigits == null || maxDigits == null || minDigits <= maxDigits)
    }

    override fun format(obj: T, builder: StringBuilder, minusNotRequired: Boolean) {
        val minDigits = minDigits ?: 1
        val number = number(obj)
        val nanoValue = number.fractionalPartWithNDigits(maxDigits ?: 9)
        when {
            nanoValue % 1000000 == 0 && minDigits <= 3 ->
                builder.append((nanoValue / 1000000 + 1000).toString().substring(1))
            nanoValue % 1000 == 0 && minDigits <= 6 ->
                builder.append((nanoValue / 1000 + 1000000).toString().substring(1))
            else -> builder.append((nanoValue + 1000000000).toString().substring(1))
        }
    }
}

internal class StringFormatterOperation<in T>(
    private val string: (T) -> String,
): FormatterOperation<T> {
    override fun format(obj: T, builder: StringBuilder, minusNotRequired: Boolean) {
        builder.append(string(obj))
    }
}
