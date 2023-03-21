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
            return ParseResult.Error("Unexpected end of input: yet to parse '$string'", startIndex)
        for (i in string.indices) {
            if (input[startIndex + i] != string[i]) return ParseResult.Error(
                "Expected $string but got ${input[startIndex + i]}",
                startIndex
            )
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
            return ParseResult.Error("Unexpected end of input: yet to parse $whatThisExpects", startIndex)
        var digitsInRow = 0
        while (startIndex + digitsInRow < input.length && input[startIndex + digitsInRow].isDigit()) {
            ++digitsInRow
        }
        if (digitsInRow < minLength)
            return ParseResult.Error(
                "Only found $digitsInRow digits in a row, but need to parse $whatThisExpects",
                startIndex
            )
        val lengths = consumers.map { it.length ?: (digitsInRow - minLength + 1) }
        var index = startIndex
        for (i in lengths.indices) {
            val numberString = input.substring(index, index + lengths[i])
            try {
                with(consumers[i]) { consume(numberString) }
            } catch (e: Throwable) {
                return ParseResult.Error(
                    "Can not interpret the string '$numberString' as ${consumers[i].whatThisExpects}",
                    index,
                    e
                )
            }
            index += lengths[i]
        }
        return ParseResult.Ok(index)
    }

    override fun toString(): String = whatThisExpects
}

/**
 * Matches the longest suitable string from `strings` and calls [consume] with the matched string.
 */
internal class StringSetParserOperation<Output>(
    strings: Set<String>,
    private val setter: (Output, String) -> Unit,
    private val whatThisExpects: String
) :
    ParserOperation<Output> {

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
                    TrieNode().also { node.children.add(-searchResult + 1, char.toString() to it) }
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
        loop@ while (index < input.length) {
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
            ParseResult.Error("Expected $whatThisExpects but got ${input[startIndex]}", startIndex)
        }
    }
}

internal fun <Output> SignedIntParser(
    minDigits: Int?,
    maxDigits: Int?,
    setter: (Output, Int) -> Unit,
    name: String,
    plusOnExceedsPad: Boolean = false,
    signsInverted: Boolean = false,
): ParserStructure<Output> {
    val parsers = mutableListOf<List<ParserOperation<Output>>>(
    )
    if (!signsInverted) {
        parsers.add(
            listOf(
                PlainStringParserOperation("-"),
                NumberSpanParserOperation(
                    listOf(
                        UnsignedIntConsumer(
                            minDigits,
                            maxDigits,
                            setter,
                            name,
                            multiplyByMinus1 = !signsInverted
                        )
                    )
                )
            ),
        )
    }
    if (plusOnExceedsPad) {
        parsers.add(
            listOf(
                NumberSpanParserOperation(
                    listOf(
                        UnsignedIntConsumer(
                            minDigits,
                            minDigits,
                            setter,
                            name,
                            multiplyByMinus1 = signsInverted
                        )
                    )
                )
            )
        )
        parsers.add(
            listOf(
                PlainStringParserOperation("+"),
                NumberSpanParserOperation(
                    listOf(
                        UnsignedIntConsumer(
                            minDigits?.let { it + 1 },
                            maxDigits,
                            setter,
                            name,
                            multiplyByMinus1 = signsInverted
                        )
                    )
                )
            )
        )
    } else {
        parsers.add(
            listOf(
                NumberSpanParserOperation(
                    listOf(
                        UnsignedIntConsumer(
                            minDigits,
                            maxDigits,
                            setter,
                            name,
                            multiplyByMinus1 = signsInverted
                        )
                    )
                )
            )
        )
    }
    return ParserStructure(
        emptyList(),
        parsers.map { ParserStructure(it, emptyList()) },
    )
}
