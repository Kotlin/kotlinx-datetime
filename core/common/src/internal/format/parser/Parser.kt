/*
 * Copyright 2023-2025 JetBrains s.r.o. and contributors.
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

internal fun <T> List<ParserStructure<T>>.concat(): ParserStructure<T> {
    /**
     * Merges pending operations (base operations, number span, and unconditional modifications)
     * with a simplified parser structure.
     *
     * Invariant: this function should only be called when `simplifiedParserStructure.operations`
     * is non-empty. If the structure consists solely of alternatives (empty operations with
     * non-empty followedBy), this function should NOT be called.
     *
     * @param baseOperations Operations to prepend (already processed operations from this parser)
     * @param numberSpan Pending number consumers that need to be merged or added (`null` if none pending)
     * @param unconditionalModifications Operations that must execute after all others
     * @param simplifiedParserStructure The simplified parser structure to merge with (must have operations)
     *
     * @return A new parser structure with all operations merged and the alternatives from [simplifiedParserStructure]
     */
    fun mergeOperations(
        baseOperations: List<ParserOperation<T>>,
        numberSpan: List<NumberConsumer<T>>?,
        unconditionalModifications: List<UnconditionalModification<T>>,
        simplifiedParserStructure: ParserStructure<T>,
    ): ParserStructure<T> {
        val operationsToMerge = simplifiedParserStructure.operations
        val firstOperation = operationsToMerge.firstOrNull()
        val mergedOperations = buildList {
            addAll(baseOperations)
            when {
                // No pending number span: just append all operations
                numberSpan == null -> {
                    addAll(operationsToMerge)
                }
                // Merge the pending number span with the first operation if it's also a number span
                firstOperation is NumberSpanParserOperation -> {
                    add(NumberSpanParserOperation(numberSpan + firstOperation.consumers))
                    for (i in 1..operationsToMerge.lastIndex) {
                        add(operationsToMerge[i])
                    }
                }
                // Add the pending number span as a separate operation before the others
                else -> {
                    add(NumberSpanParserOperation(numberSpan))
                    addAll(operationsToMerge)
                }
            }
            // Unconditional modifications always go at the end of the branch
            addAll(unconditionalModifications)
        }
        return ParserStructure(mergedOperations, simplifiedParserStructure.followedBy)
    }

    /**
     * Simplifies this parser structure and appends [other] to all execution paths.
     *
     * Simplification includes:
     * - Merging consecutive number spans into single operations
     * - Collecting unconditional modifications and applying them before regular operations or at branch ends
     * - Flattening nested alternatives
     *
     * Number span handling at branch ends:
     * - If no alternative starts with a number span, the pending number span is added as a separate operation
     * - If any alternative starts with a number span, the pending number span is distributed to all alternatives
     *   via [mergeOperations] for proper merging
     *
     * Invariant: [mergeOperations] is only called when the target structure has non-empty
     * operations, ensuring correct merging and unconditional modification placement.
     *
     * @param other The simplified parser structure to append
     * @return A new parser structure representing the simplified concatenation
     */
    fun ParserStructure<T>.simplifyAndAppend(other: ParserStructure<T>): ParserStructure<T> {
        val newOperations = mutableListOf<ParserOperation<T>>()
        var currentNumberSpan: MutableList<NumberConsumer<T>>? = null
        val unconditionalModifications = mutableListOf<UnconditionalModification<T>>()

        // Joining together the number consumers in this parser before the first alternative.
        // Collecting the unconditional modifications.
        for (op in operations) {
            if (op is NumberSpanParserOperation) {
                if (currentNumberSpan != null) {
                    currentNumberSpan.addAll(op.consumers)
                } else {
                    currentNumberSpan = op.consumers.toMutableList()
                }
            } else if (op is UnconditionalModification) {
                unconditionalModifications.add(op)
            } else {
                // Flush pending number span and unconditional modifications before regular operations
                if (currentNumberSpan != null) {
                    newOperations.add(NumberSpanParserOperation(currentNumberSpan))
                    currentNumberSpan = null
                    newOperations.addAll(unconditionalModifications)
                    unconditionalModifications.clear()
                }
                newOperations.add(op)
            }
        }

        // Recursively process alternatives, appending [other] and flattening nested structures
        val mergedTails = followedBy.flatMap {
            val simplified = it.simplifyAndAppend(other)
            // parser `ParserStructure(emptyList(), p)` is equivalent to `p`,
            // unless `p` is empty. For example, ((a|b)|(c|d)) is equivalent to (a|b|c|d).
            // As a special case, `ParserStructure(emptyList(), emptyList())` represents a parser that recognizes an empty
            // string. For example, (|a|b) is not equivalent to (a|b).
            if (simplified.operations.isEmpty())
                simplified.followedBy.ifEmpty { listOf(simplified) }
            else
                listOf(simplified)
        }.ifEmpty {
            if (other.operations.isNotEmpty()) {
                // Safe to call mergeOperations: target has operations
                return mergeOperations(newOperations, currentNumberSpan, unconditionalModifications, other)
            }
            // [other] has no operations, just alternatives; use them as our tails
            other.followedBy
        }

        return if (currentNumberSpan == null) {
            // The last operation was not a number span, or it was a number span that we are allowed to interrupt
            newOperations.addAll(unconditionalModifications)
            ParserStructure(newOperations, mergedTails)
        } else if (mergedTails.none { it.operations.firstOrNull() is NumberSpanParserOperation }) {
            // The last operation was a number span, but there are no alternatives that start with a number span.
            newOperations.add(NumberSpanParserOperation(currentNumberSpan))
            newOperations.addAll(unconditionalModifications)
            ParserStructure(newOperations, mergedTails)
        } else {
            // The last operation was a number span, and some alternatives start with one: distribute for merging.
            // [mergeOperations] is safe here because each alternative in [mergedTails] has operations
            // (verified by the structure coming from the recursive [simplifyAndAppend] calls).
            val newTails = mergedTails.map {
                mergeOperations(emptyList(), currentNumberSpan, unconditionalModifications, it)
            }
            ParserStructure(newOperations, newTails)
        }
    }

    // Combine parsers in reverse order, batching operations from parsers without followedBy.
    var result = ParserStructure<T>(emptyList(), emptyList())
    val accumulatedOperations = mutableListOf<List<ParserOperation<T>>>()

    fun flushAccumulatedOperations() {
        if (accumulatedOperations.isNotEmpty()) {
            // Reverse to restore the original order (since parsers are processed in reverse).
            val operations = buildList {
                for (parserOperations in accumulatedOperations.asReversed()) {
                    addAll(parserOperations)
                }
            }
            result = ParserStructure(operations, emptyList()).simplifyAndAppend(result)
            accumulatedOperations.clear()
        }
    }

    for (parser in this.asReversed()) {
        if (parser.followedBy.isEmpty()) {
            // No followedBy: accumulate for batch processing.
            accumulatedOperations.add(parser.operations)
        } else {
            // Has followedBy: flush accumulated operations, then process individually.
            flushAccumulatedOperations()
            result = parser.simplifyAndAppend(result)
        }
    }

    // Flush remaining accumulated operations.
    flushAccumulatedOperations()
    return result
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
