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
    val length: Int?,
    /** The human-readable name of the entity being parsed here. */
    val whatThisExpects: String
) {
    /**
     * Wholly consumes the given [input]. Should be called with a string consisting of [length] digits, or,
     * if [length] is `null`, with a string consisting of any number of digits. [consume] itself does not
     * necessarily check the length of the input string, instead expecting to be passed a valid one.
     *
     * @throws NumberFormatException if the given [input] is too large a number.
     */
    abstract fun Receiver.consume(input: String)
}

/**
 * A parser that accepts an [Int] value in range from `0` to [Int.MAX_VALUE].
 */
// TODO: should the parser reject excessive padding?
internal class UnsignedIntConsumer<in Receiver>(
    minLength: Int?,
    maxLength: Int?,
    private val setter: (Receiver, Int) -> (Unit),
    name: String,
    private val multiplyByMinus1: Boolean = false,
) : NumberConsumer<Receiver>(if (minLength == maxLength) minLength else null, name) {

    init {
        require(length == null || length in 1..9) { "Invalid length for field $whatThisExpects: $length" }
    }

    // TODO: ensure length
    override fun Receiver.consume(input: String) = when (val result = input.toIntOrNull()) {
        null -> throw NumberFormatException("Expected an Int value for $whatThisExpects but got $input")
        else -> setter(this, if (multiplyByMinus1) -result else result)
    }
}

/**
 * A parser that consumes exactly the string [expected].
 */
internal class ConstantNumberConsumer<in Receiver>(
    private val expected: String
) : NumberConsumer<Receiver>(expected.length, "the predefined string $expected") {
    override fun Receiver.consume(input: String) {
        require(input == expected) { "Expected '$expected' but got $input" }
    }
}

/**
 * A parser that accepts a [Long] value in range from `0` to [Long.MAX_VALUE].
 */
internal class UnsignedLongConsumer<in Receiver>(
    length: Int?,
    private val setter: (Receiver, Long) -> (Unit),
    name: String,
) : NumberConsumer<Receiver>(length, name) {

    init {
        require(length == null || length in 1..18) { "Invalid length for field $whatThisExpects: $length" }
    }

    override fun Receiver.consume(input: String) = when (val result = input.toLongOrNull()) {
        null -> throw NumberFormatException("Expected a Long value for $whatThisExpects but got $input")
        else -> setter(this, result)
    }
}

internal class FractionPartConsumer<in Receiver>(
    private val minLength: Int?,
    private val maxLength: Int?,
    private val setter: (Receiver, DecimalFraction) -> (Unit),
    name: String,
) : NumberConsumer<Receiver>(if (minLength == maxLength) minLength else null, name) {
    init {
        require(minLength == null || minLength in 1..9) { "Invalid length for field $whatThisExpects: $length" }
        // TODO: bounds on maxLength
    }

    override fun Receiver.consume(input: String) {
        if (minLength != null && input.length < minLength)
            throw NumberFormatException("Expected at least $minLength digits for $whatThisExpects but got $input")
        if (maxLength != null && input.length > maxLength)
            throw NumberFormatException("Expected at most $maxLength digits for $whatThisExpects but got $input")
        when (val numerator = input.toIntOrNull()) {
            null -> throw NumberFormatException("Expected at most a 9-digit value for $whatThisExpects but got $input")
            else -> setter(this, DecimalFraction(numerator, input.length))
        }
    }
}
