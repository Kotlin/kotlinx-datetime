/*
 * Copyright 2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format.parser

import kotlin.jvm.JvmInline

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

internal interface Copyable<Self> {
    fun copy(): Self
}

@JvmInline
internal value class Parser<Output : Copyable<Output>>(
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
        initialContainer: Output,
        allowDanglingInput: Boolean,
        onError: (ParseError) -> Unit,
        onSuccess: (Int, Output) -> Unit
    ) {
        val parseOptions = mutableListOf(ParserState(initialContainer, commands, startIndex))
        iterate_over_alternatives@ while (true) {
            val state = parseOptions.removeLastOrNull() ?: break
            val output = state.output.copy()
            var inputPosition = state.inputPosition
            val parserStructure = state.parserStructure
            run parse_one_alternative@{
                for (ix in parserStructure.operations.indices) {
                    parserStructure.operations[ix].consume(output, input, inputPosition).match(
                        { inputPosition = it },
                        {
                            onError(it)
                            return@parse_one_alternative // continue@iterate_over_alternatives, if that were supported
                        }
                    )
                }
                if (parserStructure.followedBy.isEmpty()) {
                    if (allowDanglingInput || inputPosition == input.length) {
                        onSuccess(inputPosition, output)
                    } else {
                        onError(ParseError(inputPosition) { "There is more input to consume" })
                    }
                } else {
                    for (ix in parserStructure.followedBy.indices.reversed()) {
                        parseOptions.add(ParserState(output, parserStructure.followedBy[ix], inputPosition))
                    }
                }
            }
        }
    }

    fun match(input: CharSequence, initialContainer: Output, startIndex: Int = 0): Output {
        val errors = mutableListOf<ParseError>()
        parse(input, startIndex, initialContainer, allowDanglingInput = false, { errors.add(it) }, { _, out -> return@match out })
        /*
         * We do care about **all** parser errors and provide diagnostic information to make the error message approacheable
         * for authors of non-trivial formatters with a multitude of potential parsing paths.
         * For that, we sort errors so that the most successful parsing paths are at the top, and
         * add them all to the parse exception message.
         */
        errors.sortByDescending { it.position }
        // `errors` can not be empty because each parser will have (successes + failures) >= 1, and here, successes == 0
        throw ParseException(errors)
    }

    fun matchOrNull(input: CharSequence, initialContainer: Output, startIndex: Int = 0): Output? {
        parse(input, startIndex, initialContainer, allowDanglingInput = false, { }, { _, out -> return@matchOrNull out })
        return null
    }

    private class ParserState<Output>(
        val output: Output,
        val parserStructure: ParserStructure<Output>,
        val inputPosition: Int,
    )
}

internal class ParseException(errors: List<ParseError>) : Exception(formatError(errors))

private fun formatError(errors: List<ParseError>): String {
    if (errors.size == 1) {
        return "Position ${errors[0].position}: ${errors[0].message()}"
    }
    // 20 For average error string: "Expected X but got Y"
    // 13 for static part "Position   :,"
    val averageMessageLength = 20 + 13
    return errors.joinTo(
        StringBuilder(averageMessageLength * errors.size),
        prefix = "Errors: ",
        separator = ", "
    ) { "position ${it.position}: '${it.message()}'" }.toString()
}
