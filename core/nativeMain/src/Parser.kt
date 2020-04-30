/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

typealias Parser<T> = (String, Int) -> Pair<Int, T>

class ParseException(message: String, position: Int) : Exception("Parse error at char $position: $message")

internal fun <T, S> Parser<T>.map(transform: (T) -> S): Parser<S> = { str, pos ->
    val (pos1, t) = this(str, pos)
    Pair(pos1, transform(t))
}

internal fun <T, S> Parser<T>.chain(other: Parser<S>): Parser<Pair<T, S>> = { str, pos ->
    val (pos1, t) = this(str, pos)
    val (pos2, s) = other(str, pos1)
    Pair(pos2, Pair(t, s))
}

internal fun <T, S> Parser<T>.chainIgnoring(other: Parser<S>): Parser<T> =
    chain(other).map { (t, _) -> t }

internal fun <T, S> Parser<T>.chainSkipping(other: Parser<S>): Parser<S> =
    chain(other).map { (_, s) -> s }

@SharedImmutable
internal val eofParser: Parser<Unit> = { str, pos ->
    if (str.length > pos) {
        throw ParseException("extraneous input", pos)
    }
    Pair(pos, Unit)
}

internal fun <T> Parser<T>.parse(str: String): T =
    chainIgnoring(eofParser)(str, 0).second

internal fun digitSpanParser(minLength: Int?, maxLength: Int?): Parser<IntRange> = { str, pos ->
    var spanLength = 0
    // index of the position after the potential sign
    val pos1 = if (str.length > pos && (str[pos] == '-' || str[pos] == '+')) {
        pos + 1
    } else {
        pos
    }
    for (i in pos1 until str.length) {
        if (str[i].isDigit()) {
            spanLength += 1
        } else {
            break
        }
    }
    if (minLength != null && spanLength < minLength) {
        if (spanLength == 0) {
            throw ParseException("number expected", pos1)
        } else {
            throw ParseException("expected at least $minLength digits", pos1 + spanLength - 1)
        }
    }
    if (maxLength != null && spanLength > maxLength) {
        throw ParseException("expected at most $maxLength digits", pos1 + maxLength)
    }
    Pair(pos1 + spanLength, IntRange(pos, pos1 + spanLength - 1))
}

internal fun intParser(minDigits: Int?, maxDigits: Int?): Parser<Int> = { str, pos ->
    val (pos1, intRange) = digitSpanParser(minDigits, maxDigits)(str, pos)
    val result = if (intRange.isEmpty()) {
        0
    } else {
        str.substring(intRange).toInt()
    }
    Pair(pos1, result)
}

internal fun fractionParser(minDigits: Int, maxDigits: Int, denominatorDigits: Int): Parser<Int> = { str, pos ->
    require(denominatorDigits <= maxDigits)
    val (pos1, intRange) = digitSpanParser(minDigits, maxDigits)(str, pos)
    if (intRange.isEmpty()) {
        Pair(pos1, 0)
    } else {
        val nominator = str.substring(intRange).toInt()
        val digitsParsed = intRange.last - intRange.first
        var result = nominator
        for (i in digitsParsed until denominatorDigits - 1) {
            result *= 10
        }
        Pair(pos1, result)
    }
}

internal fun <T> Parser<T>.or(other: Parser<T>): Parser<T> = { str, pos ->
    try {
        this(str, pos)
    } catch (e: ParseException) {
        other(str, pos)
    }
}

internal fun <T> optional(parser: Parser<T>): Parser<T?> = parser.or { _, pos -> Pair(pos, null) }

internal fun concreteCharParser(requiredChar: Char): Parser<Char> = { str, pos ->
    if (str.length <= pos) {
        throw ParseException("unexpected end of string", pos)
    }
    if (str[pos] != requiredChar) {
        throw ParseException("expected char '$requiredChar', got '${str[pos]}", pos)
    }
    Pair(pos + 1, requiredChar)
}
