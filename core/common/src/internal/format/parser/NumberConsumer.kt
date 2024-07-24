/*
 * Copyright 2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format.parser

import kotlinx.datetime.internal.*

/**
 * A parser that expects to receive a string consisting of [length] digits, or, if [length] is `null`,
 * a string consisting of any number of digits.
 */
internal sealed class NumberConsumer<in Receiver>(
    /** The number of digits to consume. `null` means that the length is variable. */
    open val length: Int?,
    /** The human-readable name of the entity being parsed here. */
    val whatThisExpects: String
) {
    /**
     * Wholly consumes the substring of [input] between indices [start] (inclusive) and [end] (exclusive).
     *
     * If [length] is non-null, [end] must be equal to [start] + [length].
     * In any case, the substring between [start] and [end] must consist of ASCII digits only.
     * [consume] itself does not necessarily check the length of the input string,
     * instead expecting to be given a valid one.
     *
     * Returns `null` on success and a `NumberConsumptionError` on failure.
     */
    abstract fun consume(storage: Receiver, input: CharSequence, start: Int, end: Int): NumberConsumptionError?
}

internal interface NumberConsumptionError {
    fun errorMessage(): String
    object ExpectedInt: NumberConsumptionError {
        override fun errorMessage() = "expected an Int value"
    }
    class TooManyDigits(val maxDigits: Int): NumberConsumptionError {
        override fun errorMessage() = "expected at most $maxDigits digits"
    }
    class TooFewDigits(val minDigits: Int): NumberConsumptionError {
        override fun errorMessage() = "expected at least $minDigits digits"
    }
    class WrongConstant(val expected: String): NumberConsumptionError {
        override fun errorMessage() = "expected '$expected'"
    }
    class Conflicting(val conflicting: Any): NumberConsumptionError {
        override fun errorMessage() = "attempted to overwrite the existing value '$conflicting'"
    }
}

/**
 * A parser that accepts an [Int] value in range from `0` to [Int.MAX_VALUE].
 */
internal class UnsignedIntConsumer<in Receiver>(
    private val minLength: Int?,
    private val maxLength: Int?,
    private val setter: AssignableField<Receiver, Int>,
    name: String,
    private val multiplyByMinus1: Boolean = false,
) : NumberConsumer<Receiver>(if (minLength == maxLength) minLength else null, name) {

    init {
        require(length == null || length in 1..9) { "Invalid length for field $whatThisExpects: $length" }
    }

    override fun consume(storage: Receiver, input: CharSequence, start: Int, end: Int): NumberConsumptionError? = when {
        maxLength != null && end - start > maxLength -> NumberConsumptionError.TooManyDigits(maxLength)
        minLength != null && end - start < minLength -> NumberConsumptionError.TooFewDigits(minLength)
        else -> when (val result = input.parseAsciiIntOrNull(start = start, end = end)) {
            null -> NumberConsumptionError.ExpectedInt
            else -> setter.setWithoutReassigning(storage, if (multiplyByMinus1) -result else result)
        }
    }
}

internal class ReducedIntConsumer<in Receiver>(
    override val length: Int,
    private val setter: AssignableField<Receiver, Int>,
    name: String,
    val base: Int,
): NumberConsumer<Receiver>(length, name) {

    private val modulo = POWERS_OF_TEN[length]
    private val baseMod = base % modulo
    private val baseFloor = base - baseMod

    init {
        require(length in 1..9) { "Invalid length for field $whatThisExpects: $length" }
    }

    override fun consume(storage: Receiver, input: CharSequence, start: Int, end: Int): NumberConsumptionError? {
        val result = input.parseAsciiInt(start = start, end = end)
        return setter.setWithoutReassigning(storage, if (result >= baseMod) {
            baseFloor + result
        } else {
            baseFloor + modulo + result
        })
    }
}

/**
 * A parser that consumes exactly the string [expected].
 */
internal class ConstantNumberConsumer<in Receiver>(
    private val expected: String
) : NumberConsumer<Receiver>(expected.length, "the predefined string $expected") {
    override fun consume(storage: Receiver, input: CharSequence, start: Int, end: Int): NumberConsumptionError? =
        if (input.substring(startIndex = start, endIndex = end) == expected) {
            null
        } else {
            NumberConsumptionError.WrongConstant(expected)
        }
}

internal class FractionPartConsumer<in Receiver>(
    private val minLength: Int,
    private val maxLength: Int,
    private val setter: AssignableField<Receiver, DecimalFraction>,
    name: String,
) : NumberConsumer<Receiver>(if (minLength == maxLength) minLength else null, name) {
    init {
        require(minLength in 1..9) {
            "Invalid minimum length $minLength for field $whatThisExpects: expected 1..9"
        }
        require(maxLength in minLength..9) {
            "Invalid maximum length $maxLength for field $whatThisExpects: expected $minLength..9"
        }
    }

    override fun consume(storage: Receiver, input: CharSequence, start: Int, end: Int): NumberConsumptionError? = when {
        end - start < minLength -> NumberConsumptionError.TooFewDigits(minLength)
        end - start > maxLength -> NumberConsumptionError.TooManyDigits(maxLength)
        else -> setter.setWithoutReassigning(
            storage, DecimalFraction(input.parseAsciiInt(start = start, end = end), end - start)
        )
    }
}

private fun <Object, Type> AssignableField<Object, Type>.setWithoutReassigning(
    receiver: Object,
    value: Type,
): NumberConsumptionError? {
    val conflictingValue = trySetWithoutReassigning(receiver, value) ?: return null
    return NumberConsumptionError.Conflicting(conflictingValue)
}

/**
 * Parses a substring of the receiver string as a positive ASCII integer.
 *
 * All characters between [start] (inclusive) and [end] (exclusive) must be ASCII digits,
 * and the size of the substring must be at most 9, but the function does not check it.
 */
private fun CharSequence.parseAsciiInt(start: Int, end: Int): Int {
    var result = 0
    for (i in start until end) {
        val digit = this[i]
        result = result * 10 + digit.asciiDigitToInt()
    }
    return result
}

/**
 * Parses a substring of the receiver string as a positive ASCII integer.
 *
 * All characters between [start] (inclusive) and [end] (exclusive) must be ASCII digits,
 * but the function does not check it.
 *
 * Returns `null` if the result does not fit into a positive [Int].
 */
private fun CharSequence.parseAsciiIntOrNull(start: Int, end: Int): Int? {
    var result = 0
    for (i in start until end) {
        val digit = this[i]
        result = result * 10 + digit.asciiDigitToInt()
        if (result < 0) return null
    }
    return result
}
