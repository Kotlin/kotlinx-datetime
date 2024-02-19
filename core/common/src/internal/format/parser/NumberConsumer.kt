/*
 * Copyright 2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format.parser

import kotlinx.datetime.internal.POWERS_OF_TEN
import kotlinx.datetime.internal.DecimalFraction

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
     * Wholly consumes the given [input]. Should be called with a string consisting of [length] digits, or,
     * if [length] is `null`, with a string consisting of any number of digits. [consume] itself does not
     * necessarily check the length of the input string, instead expecting to be passed a valid one.
     *
     * Returns `null` on success and a `NumberConsumptionError` on failure.
     */
    abstract fun consume(storage: Receiver, input: String): NumberConsumptionError?
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
// TODO: should the parser reject excessive padding?
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

    override fun consume(storage: Receiver, input: String): NumberConsumptionError? = when {
        maxLength != null && input.length > maxLength -> NumberConsumptionError.TooManyDigits(maxLength)
        minLength != null && input.length < minLength -> NumberConsumptionError.TooFewDigits(minLength)
        else -> when (val result = input.toIntOrNull()) {
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

    override fun consume(storage: Receiver, input: String): NumberConsumptionError? = when (val result = input.toIntOrNull()) {
        null -> NumberConsumptionError.ExpectedInt
        else -> setter.setWithoutReassigning(storage, if (result >= baseMod) {
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
    override fun consume(storage: Receiver, input: String): NumberConsumptionError? = if (input == expected) {
        null
    } else {
        NumberConsumptionError.WrongConstant(expected)
    }
}

internal class FractionPartConsumer<in Receiver>(
    private val minLength: Int?,
    private val maxLength: Int?,
    private val setter: AssignableField<Receiver, DecimalFraction>,
    name: String,
) : NumberConsumer<Receiver>(if (minLength == maxLength) minLength else null, name) {
    init {
        require(minLength == null || minLength in 1..9) { "Invalid length for field $whatThisExpects: $length" }
        // TODO: bounds on maxLength
    }

    override fun consume(storage: Receiver, input: String): NumberConsumptionError? = when {
        minLength != null && input.length < minLength -> NumberConsumptionError.TooFewDigits(minLength)
        maxLength != null && input.length > maxLength -> NumberConsumptionError.TooManyDigits(maxLength)
        else -> when (val numerator = input.toIntOrNull()) {
            null -> NumberConsumptionError.TooManyDigits(9)
            else -> setter.setWithoutReassigning(storage, DecimalFraction(numerator, input.length))
        }
    }
}

private fun <Object, Type> AssignableField<Object, Type>.setWithoutReassigning(
    receiver: Object,
    value: Type,
): NumberConsumptionError? {
    val conflictingValue = trySetWithoutReassigning(receiver, value) ?: return null
    return NumberConsumptionError.Conflicting(conflictingValue)
}
