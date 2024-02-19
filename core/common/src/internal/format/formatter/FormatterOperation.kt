/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format.formatter

import kotlinx.datetime.internal.DecimalFraction
import kotlinx.datetime.internal.POWERS_OF_TEN
import kotlin.math.absoluteValue

internal class ConstantStringFormatterStructure<in T>(
    private val string: String,
): FormatterStructure<T> {
    override fun format(obj: T, builder: Appendable, minusNotRequired: Boolean) {
        builder.append(string)
    }
}

internal class UnsignedIntFormatterStructure<in T>(
    private val number: (T) -> Int,
    private val zeroPadding: Int,
): FormatterStructure<T> {

    init {
        require(zeroPadding >= 0) { "The minimum number of digits ($zeroPadding) is negative" }
        require(zeroPadding <= 9) { "The minimum number of digits ($zeroPadding) exceeds the length of an Int" }
    }

    override fun format(obj: T, builder: Appendable, minusNotRequired: Boolean) {
        val num = number(obj)
        val numberStr = num.toString()
        repeat(zeroPadding - numberStr.length) { builder.append('0') }
        builder.append(numberStr)
    }
}

internal class SignedIntFormatterStructure<in T>(
    private val number: (T) -> Int,
    private val zeroPadding: Int,
    private val outputPlusOnExceededWidth: Int?,
): FormatterStructure<T> {

    init {
        require(zeroPadding >= 0) { "The minimum number of digits ($zeroPadding) is negative" }
        require(zeroPadding <= 9) { "The minimum number of digits ($zeroPadding) exceeds the length of an Int" }
    }

    override fun format(obj: T, builder: Appendable, minusNotRequired: Boolean) {
        val innerBuilder = StringBuilder()
        val number = number(obj).let { if (minusNotRequired && it < 0) -it else it }
        if (outputPlusOnExceededWidth != null && number >= POWERS_OF_TEN[outputPlusOnExceededWidth]) {
            innerBuilder.append('+')
        }
        if (number.absoluteValue < POWERS_OF_TEN[zeroPadding - 1]) {
            // needs padding
            if (number >= 0) {
                innerBuilder.append((number + POWERS_OF_TEN[zeroPadding])).deleteAt(0)
            } else {
                innerBuilder.append((number - POWERS_OF_TEN[zeroPadding])).deleteAt(1)
            }
        } else {
            innerBuilder.append(number)
        }
        builder.append(innerBuilder)
    }
}

internal class DecimalFractionFormatterStructure<in T>(
    private val number: (T) -> DecimalFraction,
    private val minDigits: Int,
    private val maxDigits: Int,
    private val zerosToAdd: List<Int>,
): FormatterStructure<T> {

    init {
        require(minDigits in 1..9) {
            "The minimum number of digits ($minDigits) is not in range 1..9"
        }
        require(maxDigits in minDigits..9) {
            "The maximum number of digits ($maxDigits) is not in range $minDigits..9"
        }
    }

    override fun format(obj: T, builder: Appendable, minusNotRequired: Boolean) {
        val number = number(obj)
        // round the number to `maxDigits` significant figures
        val numberWithRequiredPrecision = number.fractionalPartWithNDigits(maxDigits)
        // we strip away trailing zeros while we can
        var zerosToStrip = 0
        while (maxDigits > minDigits + zerosToStrip && numberWithRequiredPrecision % POWERS_OF_TEN[zerosToStrip + 1] == 0) {
            ++zerosToStrip
        }
        // we add some zeros back if it means making the number that's being output prettier, like `.01` becoming `.010`
        val zerosToAddBack = zerosToAdd[maxDigits - zerosToStrip - 1]
        if (zerosToStrip >= zerosToAddBack) zerosToStrip -= zerosToAddBack
        // the final stage of outputting the number
        val digitsToOutput = maxDigits - zerosToStrip
        val numberToOutput = numberWithRequiredPrecision / POWERS_OF_TEN[zerosToStrip]
        builder.append((numberToOutput + POWERS_OF_TEN[digitsToOutput]).toString().substring(1))
    }
}

internal class StringFormatterStructure<in T>(
    private val string: (T) -> String,
): FormatterStructure<T> {
    override fun format(obj: T, builder: Appendable, minusNotRequired: Boolean) {
        builder.append(string(obj))
    }
}

internal class ReducedIntFormatterStructure<in T>(
    private val number: (T) -> Int,
    private val digits: Int,
    private val base: Int,
): FormatterStructure<T> {
    override fun format(obj: T, builder: Appendable, minusNotRequired: Boolean) {
        val number = number(obj)
        if (number - base in 0 until POWERS_OF_TEN[digits]) {
            // the number fits
            val numberStr = (number % POWERS_OF_TEN[digits]).toString()
            val zeroPaddingStr = '0'.toString().repeat(maxOf(0, digits - numberStr.length))
            builder.append(zeroPaddingStr, numberStr)
        } else {
            if (number >= 0)
                builder.append("+")
            builder.append(number.toString())
        }
    }
}
