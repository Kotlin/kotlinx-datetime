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

/**
 * Concatenates a list of (potentially non-*valid*) parser structures into a single *valid* structure.
 *
 * A *valid* parser is one where:
 *
 * 1. Consecutive number parsers one any parsing path are represented as a single
 *   [NumberSpanParserOperation].
 * 2. A span of [UnconditionalModification] can not precede a [NumberSpanParserOperation],
 *    unless the span itself is preceded by a non-numeric non-zero-width parser.
 * 3. Every parser in every [ParserStructure.followedBy] either has non-empty [ParserStructure.operations]
 *    or is exactly `ParserStructure(emptyList(), emptyList())`.
 *
 * Together, the first two rules ensure that whenever numeric values are parsed consecutively,
 * even with zero-width parser operations between them (at the moment, these are only
 * [UnconditionalModification]), they will be treated as a single number that's then
 * split into components.
 *
 * Rule 3 means there's no excessive structure to the parser and is also useful in the [concat] implementation.
 */
internal fun <T> List<ParserStructure<T>>.concat(): ParserStructure<T> {
    /**
     * Returns a *valid* parser obtained by prepending [baseOperations] followed by [numberSpan]
     * to [simplifiedParserStructure],
     * while ensuring that [unconditionalModifications] are present in the result.
     *
     * Guarantees:
     * - If `simplifiedParserStructure.followedBy` is empty, the resulting `followedBy` will also be empty.
     * - If `simplifiedParserStructure.operations` is non-empty, the resulting `operations` will also be non-empty.
     *
     * Requirements:
     * - [simplifiedParserStructure] must either have non-empty [ParserStructure.operations] or be the empty parser.
     * - [simplifiedParserStructure] is a *valid* parser.
     * - [baseOperations] can not end with either an [UnconditionalModification] or a [NumberSpanParserOperation].
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
            // Currently, `this` is either empty or ends with a non-numeric non-zero-width parser.
            when {
                numberSpan == null -> {
                    addAll(operationsToMerge)
                }
                firstOperation is NumberSpanParserOperation -> {
                    add(NumberSpanParserOperation(numberSpan + firstOperation.consumers))
                    for (i in 1..operationsToMerge.lastIndex) {
                        add(operationsToMerge[i])
                    }
                }
                else -> {
                    add(NumberSpanParserOperation(numberSpan))
                    addAll(operationsToMerge)
                }
            }
            // Currently, `this` ends with the operations from `operationsToMerge`.
            // If `operationsToMerge` was not empty, and its `lastOrNull()` is non-empty, then
            // - If it's a `NumberSpanParserOperation`,
            //   this means its `followedBy` do not start with a `NumberSpanParserOperation`,
            //   since `simplifiedParserStructure` is *valid*.
            //   This means it's valid to append `unconditionalModifications`.
            // - If it's an `UnconditionalModification`,
            //   this means either that its `followedBy` do not start with a `NumberSpanParserOperation`,
            //   or that some non-zero-width non-numeric parsers precede it in `operationsToMerge`.
            //   Adding new `unconditionalModifications` to the existing span does not break correctness.
            // - If it's some other parser,
            //   then `unconditionalModifications` is preceded by a non-zero-width non-numeric parser,
            //   which is valid.
            //
            // If `operationsToMerge` was empty, then `simplifiedParserStructure` is fully empty,
            // so `unconditionalModifications` precedes nothing at all.
            addAll(unconditionalModifications)
        }
        // The first two rules of validity hold by the considerations in the `mergedOperations` block.
        // The third rule holds because `simplifiedParserStructure.followedBy` must be valid.
        return ParserStructure(mergedOperations, simplifiedParserStructure.followedBy)
    }

    /**
     * Returns a *valid* parser obtained by prepending *any* parser `this` to a *valid* parser [other].
     */
    fun ParserStructure<T>.simplifyAndAppend(other: ParserStructure<T>): ParserStructure<T> {
        val newOperations = mutableListOf<ParserOperation<T>>()
        var currentNumberSpan: MutableList<NumberConsumer<T>>? = null
        val unconditionalModifications = mutableListOf<UnconditionalModification<T>>()

        // Loop invariant:
        //
        //                                           |- zero-width parsers interspersing the number span
        //                                           |
        //                                           unconditionalModifications
        //                                           \-------------------------/
        // operation, ..., operation, number, number, UnconditionalModification, number, operation, operation
        // \_______________________/  \______________ . . . . . . . . . . . . . ______/  \_______/
        //      newOperations                        currentNumberSpan                      op
        //      |                                    |                                      |- next operation
        //      |- operations where spans of         |- the continued span of
        //         number parsers are merged into       number parsers
        //         `NumberSpanParserOperation`
        for (op in operations) {
            when (op) {
                is NumberSpanParserOperation -> {
                    if (currentNumberSpan != null) {
                        currentNumberSpan.addAll(op.consumers)
                    } else {
                        currentNumberSpan = op.consumers.toMutableList()
                    }
                }
                is UnconditionalModification -> unconditionalModifications.add(op)
                else -> {
                    if (currentNumberSpan != null) {
                        newOperations.add(NumberSpanParserOperation(currentNumberSpan))
                        currentNumberSpan = null
                        newOperations.addAll(unconditionalModifications)
                        unconditionalModifications.clear()
                    }
                    newOperations.add(op)
                }
            }
        }

        // *Valid* parsers resulting from appending [other] to every parser in `this.followedBy`.
        //
        // Every parser in this list is guaranteed to be a valid `followedBy` element, that is,
        // either have non-empty `ParserStructure.operations` or be exactly `ParserStructure(emptyList(), emptyList())`. 
        val mergedTails = followedBy.flatMap {
            val simplified = it.simplifyAndAppend(other)
            // Parser `ParserStructure(emptyList(), p)` is equivalent to `p`,
            // unless `p` is empty. For example, ((a|b)|(c|d)) is equivalent to (a|b|c|d).
            // As a special case, `ParserStructure(emptyList(), emptyList())` represents a parser that recognizes an empty
            // string. For example, (|a|b) is not equivalent to (a|b).
            if (simplified.operations.isEmpty())
                simplified.followedBy.ifEmpty { listOf(simplified) }
            else
                listOf(simplified)
        }.ifEmpty {
            // We only enter this branch if [followedBy] is empty.
            // In that case, [mergedTails] is exactly `listOf(other)`.
            // We optimize this common case here as a fast-path and to reduce indirection in the resulting parser.
            if (other.operations.isNotEmpty()) {
                // Directly append `other` to the simplified `this`.
                // The call is valid: `other.operations` is non-empty
                return mergeOperations(newOperations, currentNumberSpan, unconditionalModifications, other)
            }
            // [other] has no operations, just alternatives; use them as our tails
            other.followedBy
        }

        return if (
            currentNumberSpan == null && newOperations.isNotEmpty() ||
            mergedTails.none { it.operations.firstOrNull() is NumberSpanParserOperation }
        ) {
            if (currentNumberSpan != null) {
                newOperations.add(NumberSpanParserOperation(currentNumberSpan))
            }
            newOperations.addAll(unconditionalModifications)
            // Either the merged tails do not start with a `NumberSpanParserOperation`,
            // or the last non-zero-width parser `newOperations` exists and is not a number parser.
            //
            // In the first case, the resulting parser is *valid*:
            // `unconditionalModifications` does not precede a number parser, and in `newOperations`,
            // consecutive number parsers are merged into one.
            //
            // In the second case, the resulting parser is also *valid*:
            // `unconditionalModifications` may precede a number parser, but it also has
            // a non-zero-width non-number parser before it.
            ParserStructure(newOperations, mergedTails)
        } else {
            // Some `mergedTails` begin with a number parser, and also, either
            // the current number span isn't empty, or there are no non-zero-width non-number parsers preceding it.
            val newTails = mergedTails.map { structure ->
                // This is a valid `followedBy` element:
                // - If [structure] is the empty parser,
                //   the resulting parser will have an empty `followedBy` list.
                //   Such `followedBy` elements are always valid.
                // - If [structure] is a non-empty parser,
                //   it must have a non-empty `followedBy` list
                //   *and* non-empty `operations`.
                //   The resulting parser will also have non-empty `operations`,
                //   which makes it a valid `followedBy` element.
                mergeOperations(emptyList(), currentNumberSpan, unconditionalModifications, structure)
            }
            // [newTails] only contains *valid* parsers that are also valid `followedBy` elements.
            // They also start with the current number span.
            //
            // The resulting parser is *valid*, because furthermore, it is always valid for [currentNumberSpan],
            // with which every [newTails] starts, to follow [newOperations].
            ParserStructure(newOperations, newTails)
        }
    }

    var result = ParserStructure<T>(emptyList(), emptyList())
    val accumulatedOperations = mutableListOf<List<ParserOperation<T>>>()

    fun flushAccumulatedOperations() {
        if (accumulatedOperations.isNotEmpty()) {
            val operations = buildList {
                for (parserOperations in accumulatedOperations.asReversed()) {
                    addAll(parserOperations)
                }
            }
            result = ParserStructure(operations, emptyList()).simplifyAndAppend(result)
            accumulatedOperations.clear()
        }
    }

    // Loop invariant:
    //
    // this = Parser, ..., Parser, operations, operations, operations, Parser, Parser, ...
    //                     \____/  \________________________________/  \_________________/
    //                     parser   accumulatedOperations.reversed()        result
    //                     |        |                                       |- simplified parser
    //                     |        |- span of parsers without branching
    //                     |
    //                     |- next parser to be processed
    for (parser in this.asReversed()) {
        if (parser.followedBy.isEmpty()) {
            accumulatedOperations.add(parser.operations)
        } else {
            flushAccumulatedOperations()
            result = parser.simplifyAndAppend(result)
        }
    }

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
