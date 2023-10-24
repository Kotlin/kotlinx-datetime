/*
 * Copyright 2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format.parser

internal interface ParserOperation<in Output> {
    fun Output.consume(input: CharSequence, startIndex: Int): ParseResult
}

/**
 * Consumes exactly the string [string].
 */
internal class PlainStringParserOperation<Output>(val string: String) : ParserOperation<Output> {
    init {
        require(string.isNotEmpty()) { "Empty string is not allowed" }
        require(!string[0].isDigit())
        require(!string[string.length - 1].isDigit())
    }

    override fun Output.consume(input: CharSequence, startIndex: Int): ParseResult {
        if (startIndex + string.length > input.length)
            return ParseResult.Error(startIndex) { "Unexpected end of input: yet to parse '$string'" }
        for (i in string.indices) {
            if (input[startIndex + i] != string[i]) return ParseResult.Error(startIndex) {
                "Expected $string but got ${input[startIndex + i]}"
            }
        }
        return ParseResult.Ok(startIndex + string.length)
    }

    // TODO: properly escape
    override fun toString(): String = "'$string'"
}

/**
 * Greedily consumes as many digits as possible and redistributes them among [consumers].
 *
 * At most one consumer is allowed to have a variable length.
 * If more digits are supplied than the sum of lengths of all consumers, this is the consumer that will receive the
 * extra.
 */
internal class NumberSpanParserOperation<Output>(
    val consumers: List<NumberConsumer<Output>>,
) : ParserOperation<Output> {

    private val minLength = consumers.sumOf { it.length ?: 1 }
    private val isFlexible = consumers.any { it.length == null }

    init {
        require(consumers.all { (it.length ?: Int.MAX_VALUE) > 0 })
        require(consumers.count { it.length == null } <= 1)
    }

    private val whatThisExpects: String
        get() {
            val consumerLengths = consumers.map {
                when (val length = it.length) {
                    null -> "at least one digit"
                    else -> "$length digits"
                } + " for ${it.whatThisExpects}"
            }
            return if (isFlexible) {
                "a number with at least $minLength digits: $consumerLengths"
            } else {
                "a number with exactly $minLength digits: $consumerLengths"
            }
        }

    override fun Output.consume(input: CharSequence, startIndex: Int): ParseResult {
        if (startIndex + minLength > input.length)
            return ParseResult.Error(startIndex) { "Unexpected end of input: yet to parse $whatThisExpects" }
        var digitsInRow = 0
        while (startIndex + digitsInRow < input.length && input[startIndex + digitsInRow].isDigit()) {
            ++digitsInRow
        }
        if (digitsInRow < minLength)
            return ParseResult.Error(startIndex) {
                "Only found $digitsInRow digits in a row, but need to parse $whatThisExpects"
            }
        val lengths = consumers.map { it.length ?: (digitsInRow - minLength + 1) }
        var index = startIndex
        for (i in lengths.indices) {
            val numberString = input.substring(index, index + lengths[i])
            try {
                with(consumers[i]) { consume(numberString) }
            } catch (e: Throwable) {
                return ParseResult.Error(index, e) {
                    "Can not interpret the string '$numberString' as ${consumers[i].whatThisExpects}"
                }
            }
            index += lengths[i]
        }
        return ParseResult.Ok(index)
    }

    override fun toString(): String = whatThisExpects
}

internal class SignParser<Output>(
    private val isNegativeSetter: (Output, Boolean) -> Unit,
    private val withPlusSign: Boolean,
    private val whatThisExpects: String,
) : ParserOperation<Output> {
    override fun Output.consume(input: CharSequence, startIndex: Int): ParseResult {
        if (startIndex >= input.length)
            return ParseResult.Ok(startIndex)
        val char = input[startIndex]
        if (char == '-') {
            isNegativeSetter(this, true)
            return ParseResult.Ok(startIndex + 1)
        }
        if (char == '+' && withPlusSign) {
            isNegativeSetter(this, false)
            return ParseResult.Ok(startIndex + 1)
        }
        return ParseResult.Error(startIndex) { "Expected $whatThisExpects but got $char" }
    }

    override fun toString(): String = whatThisExpects
}

/**
 * Matches the longest suitable string from `strings` and calls [consume] with the matched string.
 */
internal class StringSetParserOperation<Output>(
    strings: Collection<String>,
    private val setter: (Output, String) -> Unit,
    private val whatThisExpects: String,
) : ParserOperation<Output> {

    // TODO: tries don't have good performance characteristics for small sets, add a special case for small sets

    private class TrieNode(
        val children: MutableList<Pair<String, TrieNode>> = mutableListOf(),
        var isTerminal: Boolean = false
    )

    private val trie = TrieNode()

    init {
        for (string in strings) {
            var node = trie
            for (char in string) {
                val searchResult = node.children.binarySearchBy(char.toString()) { it.first }
                node = if (searchResult < 0) {
                    TrieNode().also { node.children.add(-searchResult - 1, char.toString() to it) }
                } else {
                    node.children[searchResult].second
                }
            }
            node.isTerminal = true
        }
        fun reduceTrie(trie: TrieNode) {
            for ((_, child) in trie.children) {
                reduceTrie(child)
            }
            val newChildren = mutableListOf<Pair<String, TrieNode>>()
            for ((key, child) in trie.children) {
                if (!child.isTerminal && child.children.size == 1) {
                    val (grandChildKey, grandChild) = child.children.single()
                    newChildren.add(key + grandChildKey to grandChild)
                } else {
                    newChildren.add(key to child)
                }
            }
            trie.children.clear()
            trie.children.addAll(newChildren.sortedBy { it.first })
        }
        reduceTrie(trie)
    }

    override fun Output.consume(input: CharSequence, startIndex: Int): ParseResult {
        var node = trie
        var index = startIndex
        var lastMatch: Int? = null
        loop@ while (index <= input.length) {
            if (node.isTerminal) lastMatch = index
            for ((key, child) in node.children) {
                if (input.startsWith(key, index)) {
                    node = child
                    index += key.length
                    continue@loop
                }
            }
            break // nothing found
        }
        return if (lastMatch != null) {
            setter(this, input.subSequence(startIndex, lastMatch).toString())
            ParseResult.Ok(lastMatch)
        } else {
            ParseResult.Error(startIndex) { "Expected $whatThisExpects but got ${input[startIndex]}" }
        }
    }
}

internal fun <Output> SignedIntParser(
    minDigits: Int?,
    maxDigits: Int?,
    spacePadding: Int?,
    setter: (Output, Int) -> Unit,
    name: String,
    plusOnExceedsWidth: Int?,
): ParserStructure<Output> {
    val parsers = mutableListOf(
        spaceAndZeroPaddedUnsignedInt(minDigits, maxDigits, spacePadding, setter, name, withMinus = true)
    )
    if (plusOnExceedsWidth != null) {
        parsers.add(
            spaceAndZeroPaddedUnsignedInt(minDigits, plusOnExceedsWidth, spacePadding, setter, name)
        )
        parsers.add(
            ParserStructure(
                listOf(
                    PlainStringParserOperation("+"),
                    NumberSpanParserOperation(
                        listOf(
                            UnsignedIntConsumer(
                                plusOnExceedsWidth + 1,
                                maxDigits,
                                setter,
                                name,
                                multiplyByMinus1 = false
                            )
                        )
                    )
                ),
                emptyList()
            )
        )
    } else {
        parsers.add(
            spaceAndZeroPaddedUnsignedInt(minDigits, maxDigits, spacePadding, setter, name)
        )
    }
    return ParserStructure(
        emptyList(),
        parsers,
    )
}

// With maxWidth = 4,
// padWidth = 7: "   " + (four digits | " " (three digits | " " (two digits | " ", one digit)))
// padWidth = 3: three to four digits | " " (two digits | " ", one digit)
internal fun <Target> spaceAndZeroPaddedUnsignedInt(
    minDigits: Int?,
    maxDigits: Int?,
    spacePadding: Int?,
    setter: (Target, Int) -> Unit,
    name: String,
    withMinus: Boolean = false,
): ParserStructure<Target> {
    val minNumberLength = (minDigits ?: 1) + if (withMinus) 1 else 0
    val maxNumberLength = maxDigits?.let { if (withMinus) it + 1 else it } ?: Int.MAX_VALUE
    val spacePadding = spacePadding ?: 0
    fun numberOfRequiredLengths(minNumberLength: Int, maxNumberLength: Int): ParserStructure<Target> {
        check(maxNumberLength >= 1 + if (withMinus) 1 else 0)
        return ParserStructure(
            buildList {
                if (withMinus) add(PlainStringParserOperation("-"))
                add(NumberSpanParserOperation(
                    listOf(
                        UnsignedIntConsumer(
                            minNumberLength - if (withMinus) 1 else 0,
                            maxNumberLength - if (withMinus) 1 else 0,
                            setter,
                            name,
                            multiplyByMinus1 = withMinus,
                        )
                    )
                ))
            },
            emptyList()
        )
    }
    val maxPaddedNumberLength = minOf(maxNumberLength, spacePadding)
    if (minNumberLength >= maxPaddedNumberLength) return numberOfRequiredLengths(minNumberLength, maxNumberLength)
    // invariant: the length of the string parsed by 'accumulated' is exactly 'accumulatedWidth'
    var accumulated: ParserStructure<Target> = numberOfRequiredLengths(minNumberLength, minNumberLength)
    for (accumulatedWidth in minNumberLength until maxPaddedNumberLength) {
        accumulated = ParserStructure(
            emptyList(),
            listOf(
                numberOfRequiredLengths(accumulatedWidth + 1, accumulatedWidth + 1),
                listOf(
                    ParserStructure(listOf(PlainStringParserOperation(" ")), emptyList()),
                    accumulated
                ).concat()
            )
        )
    }
    // accumulatedWidth == maxNumberLength || accumulatedWidth == spacePadding.
    // In the first case, we're done, in the second case, we need to add the remaining numeric lengths.
    return if (spacePadding > maxNumberLength) {
        val prepadding = PlainStringParserOperation<Target>(" ".repeat(spacePadding - maxNumberLength))
        listOf(
            ParserStructure(
                listOf(
                    prepadding,
                ),
                emptyList()
            ), accumulated
        ).concat()
    } else if (spacePadding == maxNumberLength) {
        accumulated
    } else {
        val r = ParserStructure(
            emptyList(),
            listOf(
                numberOfRequiredLengths(spacePadding + 1, maxNumberLength),
                accumulated
            )
        )
        r
    }
}

internal fun <Output> ReducedIntParser(
    digits: Int,
    base: Int,
    setter: (Output, Int) -> Unit,
    name: String,
): ParserStructure<Output> = ParserStructure(
    emptyList(),
    listOf(
        ParserStructure(
            listOf(
                NumberSpanParserOperation(
                    listOf(
                        ReducedIntConsumer(
                            digits,
                            setter,
                            name,
                            base = base
                        )
                    )
                )
            ),
            emptyList()
        ),
        ParserStructure(
            listOf(
                PlainStringParserOperation("+"),
                NumberSpanParserOperation(
                    listOf(
                        UnsignedIntConsumer(
                            null,
                            null,
                            setter,
                            name,
                            multiplyByMinus1 = false
                        )
                    )
                )
            ),
            emptyList()
        ),
        ParserStructure(
            listOf(
                PlainStringParserOperation("-"),
                NumberSpanParserOperation(
                    listOf(
                        UnsignedIntConsumer(
                            null,
                            null,
                            setter,
                            name,
                            multiplyByMinus1 = true
                        )
                    )
                )
            ),
            emptyList()
        ),
    )
)
