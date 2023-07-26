/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format.formatter

import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.POWERS_OF_TEN
import kotlin.math.*

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
        require(zeroPadding >= 0)
        require(zeroPadding <= 9)
    }

    override fun format(obj: T, builder: Appendable, minusNotRequired: Boolean) {
        val numberStr = number(obj).toString()
        val zeroPaddingStr = '0'.toString().repeat(maxOf(0, zeroPadding - numberStr.length))
        builder.append(zeroPaddingStr, numberStr)
    }
}

internal class SignedIntFormatterStructure<in T>(
    private val number: (T) -> Int,
    private val zeroPadding: Int,
    private val outputPlusOnExceededWidth: Int?,
): FormatterStructure<T> {

    init {
        require(zeroPadding >= 0)
        require(zeroPadding <= 9)
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
    private val minDigits: Int?,
    private val maxDigits: Int?,
): FormatterStructure<T> {

    init {
        require(minDigits == null || minDigits in 1..9)
        require(maxDigits == null || maxDigits in 1..9)
        require(minDigits == null || maxDigits == null || minDigits <= maxDigits)
    }

    override fun format(obj: T, builder: Appendable, minusNotRequired: Boolean) {
        val number = number(obj)
        val maxLength = maxDigits ?: 9
        if (minDigits != null) {
            val nDigitNumber = number.fractionalPartWithNDigits(maxLength)
            val fullString = (nDigitNumber + POWERS_OF_TEN[maxLength]).toString().substring(1)
            var zeroSpanStart = fullString.length
            while (zeroSpanStart > minDigits && fullString[zeroSpanStart - 1] == '0') { zeroSpanStart-- }
            val truncatedString = fullString.substring(0, zeroSpanStart)
            builder.append(truncatedString)
        } else {
            val nanos = number.fractionalPartWithNDigits(9)
            val digitsToOutput = minOf(maxLength, when {
                nanos % 1000000 == 0 -> 3
                nanos % 1000 == 0 -> 6
                else -> 9
            })
            val numberToOutput = number.fractionalPartWithNDigits(digitsToOutput).let {
                if (it >= POWERS_OF_TEN[digitsToOutput]) POWERS_OF_TEN[digitsToOutput] - 1 else it
            }
            builder.append((numberToOutput + POWERS_OF_TEN[digitsToOutput]).toString().substring(1))
        }
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
