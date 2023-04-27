/*
 * Copyright 2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format.parser

/**
 * Describes the commands that the parser must execute, in two portions:
 * * [operations], which are executed in order, and
 * * [followedBy], which are executed *in parallel* after [operations].
 *
 * An example of a [ParserStructure]:
 * ```
 * // number - dash - number - dash - number
 * //        |
 * //        \
 * //         letter 'W' - number
 * ParserStructure(
 *   listOf(numberParser),
 *   listOf(
 *     ParserStructure(
 *       listOf(stringParser("-"), numberParser, stringParser("-"), numberParser),
 *       emptyList()
 *     ),
 *     ParserStructure(
 *       listOf(stringParser("W"), numberParser),
 *       emptyList()
 *     ),
 *   )
 * )
 * ```
 */
internal class ParserStructure<in Output>(
    val operations: List<ParserOperation<Output>>,
    val followedBy: List<ParserStructure<Output>>,
) {
    override fun toString(): String =
        "${operations.joinToString(", ")}(${followedBy.joinToString(";")})"
}

// TODO: O(size of the resulting parser ^ 2), but can be O(size of the resulting parser)
internal fun <T> List<ParserStructure<T>>.concat(): ParserStructure<T> {
    fun <T> ParserStructure<T>.append(other: ParserStructure<T>): ParserStructure<T> = if (followedBy.isEmpty()) {
        ParserStructure(operations + other.operations, other.followedBy)
    } else {
        ParserStructure(operations, followedBy.map { it.append(other) })
    }
    fun <T> ParserStructure<T>.simplify(): ParserStructure<T> {
        val newOperations = mutableListOf<ParserOperation<T>>()
        var currentNumberSpan: MutableList<NumberConsumer<T>>? = null
        // joining together the number consumers in this parser before the first alternative
        for (op in operations) {
            if (op is NumberSpanParserOperation) {
                if (currentNumberSpan != null) {
                    currentNumberSpan.addAll(op.consumers)
                } else {
                    currentNumberSpan = op.consumers.toMutableList()
                }
            } else {
                if (currentNumberSpan != null) {
                    newOperations.add(NumberSpanParserOperation(currentNumberSpan))
                    currentNumberSpan = null
                }
                newOperations.add(op)
            }
        }
        val mergedTails = followedBy.flatMap {
            val simplified = it.simplify()
            // parser `ParserStructure(emptyList(), p)` is equivalent to `p`,
            // unless `p` is empty. For example, ((a|b)|(c|d)) is equivalent to (a|b|c|d).
            // As a special case, `ParserStructure(emptyList(), emptyList())` represents a parser that recognizes an empty
            // string. For example, (|a|b) is not equivalent to (a|b).
            if (simplified.operations.isEmpty())
                simplified.followedBy.ifEmpty { listOf(simplified) }
            else
                listOf(simplified)
        }
        return if (currentNumberSpan == null) {
            // the last operation was not a number span, or it was a number span that we are allowed to interrupt
            ParserStructure(newOperations, mergedTails)
        } else if (mergedTails.none {
                it.operations.firstOrNull()?.let { it is NumberSpanParserOperation } == true
            }) {
            // the last operation was a number span, but there are no alternatives that start with a number span.
            newOperations.add(NumberSpanParserOperation(currentNumberSpan))
            ParserStructure(newOperations, mergedTails)
        } else {
            val newTails = mergedTails.map {
                when (val firstOperation = it.operations.firstOrNull()) {
                    is NumberSpanParserOperation -> {
                        ParserStructure(
                            listOf(NumberSpanParserOperation(currentNumberSpan + firstOperation.consumers)) + it.operations.drop(
                                1
                            ),
                            it.followedBy
                        )
                    }

                    null -> ParserStructure(
                        listOf(NumberSpanParserOperation(currentNumberSpan)),
                        it.followedBy
                    )

                    else -> ParserStructure(
                        listOf(NumberSpanParserOperation(currentNumberSpan)) + it.operations,
                        it.followedBy
                    )
                }
            }
            ParserStructure(newOperations, newTails)
        }
    }
    val naiveParser = foldRight(ParserStructure<T>(emptyList(), emptyList())) { parser, acc -> parser.append(acc) }
    return naiveParser.simplify()
}

internal class Parser<Output>(
    private val defaultState: () -> Output,
    private val copyState: (Output) -> Output,
    private val commands: ParserStructure<Output>
) {
    /**
     * [startIndex] is the index of the first character that is not yet consumed.
     *
     * [allowDanglingInput] determines whether the match is only successful if the whole string after [startIndex]
     * is consumed.
     *
     * [onSuccess] is invoked as soon as some parsing attempt succeeds.
     * [onError] is invoked when some parsing attempt fails.
     */
    // Would be a great place to use the `Flow` from `kotlinx.coroutines` here instead of `onSuccess` and
    // `onError`, but alas.
    private inline fun parse(
        input: CharSequence,
        startIndex: Int,
        allowDanglingInput: Boolean,
        onError: (ParseError) -> Unit,
        onSuccess: (Int, Output) -> Unit
    ) {
        var states = mutableListOf(ParserState(defaultState(), StructureIndex(0, commands), startIndex))
        while (states.isNotEmpty()) {
            states = states.flatMap { state ->
                val index = state.commandPosition
                if (index.operationIndex < index.parserStructure.operations.size) {
                    val newIndex = StructureIndex(index.operationIndex + 1, index.parserStructure)
                    val command = state.commandPosition.parserStructure.operations[state.commandPosition.operationIndex]
                    val result = with(command) { state.output.consume(input, state.inputPosition) }
                    if (result.isOk()) {
                        listOf(ParserState(state.output, newIndex, result.tryGetIndex()!!))
                    } else {
                        onError(result.tryGetError()!!)
                        emptyList()
                    }
                } else {
                    index.parserStructure.followedBy.map { nextStructure ->
                        ParserState(copyState(state.output), StructureIndex(0, nextStructure), state.inputPosition)
                    }.also {
                        if (it.isEmpty()) {
                            if (allowDanglingInput || state.inputPosition == input.length) {
                                onSuccess(state.inputPosition, state.output)
                            } else {
                                onError(ParseError(state.inputPosition) { "There is more input to consume" })
                            }
                        }
                    }
                }
            }.toMutableList()
        }
    }


    fun match(input: CharSequence, startIndex: Int = 0): Output {
        val errors = mutableListOf<ParseError>()
        parse(input, startIndex, allowDanglingInput = false, { errors.add(it) }, { _, out -> return@match out })
        errors.sortByDescending { it.position }
        // `errors` can not be empty because each parser will have (successes + failures) >= 1, and here, successes == 0
        ParseException(errors.first()).let {
            for (error in errors.drop(1)) {
                it.addSuppressed(ParseException(error))
            }
            throw it
        }
    }

    private fun <T> findLongestAtIndex(input: CharSequence, index: Int, transform: (Output) -> T?): Pair<T?, Int?> {
        var result: T? = null
        var maxParsed: Int? = null
        parse(input, index, allowDanglingInput = true, {}) { newIndex, out ->
            if (newIndex > (maxParsed ?: -1)) {
                maxParsed = newIndex
                transform(out)?.let {
                    result = it
                }
            }
        }
        return Pair(result, maxParsed)
    }

    fun<T> find(input: CharSequence, startIndex: Int = 0, transform: (Output) -> T?): T? {
        iterateOverIndices(input, startIndex) { index ->
            val (value, newIndex) = findLongestAtIndex(input, index, transform)
            if (value != null) {
                return value
            }
            newIndex
        }
        return null
    }

    fun<T> findAll(input: CharSequence, startIndex: Int = 0, transform: (Output) -> T?): List<T> =
        mutableListOf<T>().apply {
            iterateOverIndices(input, startIndex) { index ->
                val (value, newIndex) = findLongestAtIndex(input, index, transform)
                value?.let { add(it) }
                newIndex
            }
        }

    private inner class ParserState<Output>(
        val output: Output,
        val commandPosition: StructureIndex<Output>,
        val inputPosition: Int,
    )

    private class StructureIndex<in Output>(
        val operationIndex: Int,
        val parserStructure: ParserStructure<Output>
    )
}

/**
 * Iterates over the indices in [input] that are not in the middle of a number.
 *
 * The purpose is to avoid parsing "2020-01-01" in "202020-01-01".
 *
 * [startIndex] is the first index to consider.
 * [block] is invoked for each index that is not in the middle of a number. It may return the next index to consider.
 */
private inline fun iterateOverIndices(input: CharSequence, startIndex: Int, block: (Int) -> Int?) {
    // `Regex.find` allows lookbehinds to see the string before the `startIndex`, so we do that as well
    var i = if (startIndex > 0 && input[startIndex - 1].isLetterOrDigit()) startIndex
    else block(startIndex) ?: (startIndex + 1)
    while (i < input.length) {
        i = block(i) ?: (i + 1)
        if (input[i - 1].isDigit()) {
            while (i < input.length && input[i].isDigit()) {
                i++
            }
        }
    }
}

