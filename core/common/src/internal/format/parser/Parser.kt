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

    fun canStartWithNumber(): Boolean =
        when (operations.firstOrNull()?.let { it is NumberSpanParserOperation }) {
            null -> followedBy.any { it.canStartWithNumber() }
            true -> true
            false -> false
        }
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
        onError: (ParseException) -> Unit,
        onSuccess: (Output) -> Unit
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
                                onSuccess(state.output)
                            } else {
                                onError(ParseException("There is more input to consume", state.inputPosition))
                            }
                        }
                    }
                }
            }.toMutableList()
        }
    }


    fun match(input: CharSequence, startIndex: Int = 0): Output {
        val errors = mutableListOf<ParseException>()
        parse(input, startIndex, allowDanglingInput = false, { errors.add(it) }, { return@match it })
        errors.sortByDescending { it.position }
        // `errors` can not be empty because each parser will have (successes + failures) >= 1, and here, successes == 0
        errors.first().let {
            for (error in errors.drop(1)) {
                it.addSuppressed(error)
            }
            throw it
        }
    }

    // TODO: only take the longest match from the given start position
    fun find(input: CharSequence, startIndex: Int = 0): Output? {
        forEachNonInternalIndex(input, startIndex) { index ->
            parse(input, index, allowDanglingInput = true, {}, { return@find it })
        }
        return null
    }

    // TODO: skip the indices in already-parsed parts of the input
    // TODO: only take the longest match from the given start position
    fun findAll(input: CharSequence, startIndex: Int = 0): List<Output> =
        mutableListOf<Output>().apply {
            forEachNonInternalIndex(input, startIndex) { index ->
                parse(input, index, allowDanglingInput = true, {}, { add(it) })
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
 * Iterates over all the indices in [input] that are not in the middle of either a word or a number.
 *
 * For example, in the string "Tom is 10", the start indices of the following strings are returned:
 * - "Tom is 10"
 * - " is 10"
 * - "is 10"
 * - " 10"
 * - "10"
 *
 * The purpose is to avoid parsing "UTC" in "OUTCOME" or "2020-01-01" in "202020-01-01".
 */
// TODO: permit starting parsing in the middle of the word, since, in some languages, spaces are not used
private inline fun forEachNonInternalIndex(input: CharSequence, startIndex: Int, block: (Int) -> Unit) {
    val OUTSIDE_SPAN = 0
    val IN_NUMBER = 1
    val IN_WORD = 2
    var currentState = OUTSIDE_SPAN
    // `Regex.find` allows lookbehinds to see the string before the `startIndex`, so we do that as well
    if (startIndex != 0) {
        if (input[startIndex - 1].isDigit()) {
            currentState = IN_NUMBER
        } else if (input[startIndex - 1].isLetter()) {
            currentState = IN_WORD
        }
    }
    for (i in startIndex..input.length) {
        if (input[i].isDigit() && currentState == IN_NUMBER || input[i].isLetter() && currentState == IN_WORD) {
            continue
        }
        if (input[i].isDigit()) currentState = IN_NUMBER
        if (input[i].isLetter()) currentState = IN_WORD
        block(i)
    }
}

