/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.AlternativesFormatStructure

@DslMarker
public annotation class DateTimeBuilder

/**
 * Common functions for all the date-time format builders.
 */
public interface FormatBuilder<out Self> {
    /**
     * Appends a set of alternative blocks to the format.
     *
     * When parsing, the blocks are tried in order until one of them succeeds.
     *
     * When formatting, there is a requirement that the later blocks contain all the fields that are present in the
     * earlier blocks. Moreover, the additional fields must have a *default value* defined for them.
     * Then, during formatting, the block that has the most information is chosen.
     *
     * Example:
     * ```
     * appendAlternatives({
     *   appendLiteral("Z")
     * }, {
     *   appendOffsetHours()
     *   appendOptional {
     *     appendLiteral(":")
     *     appendOffsetMinutes()
     *     appendOptional {
     *       appendLiteral(":")
     *       appendOffsetSeconds()
     *     }
     *   }
     * })
     * ```
     * Here, all values have the default value of zero, so the first block is chosen when formatting `UtcOffset.ZERO`.
     */
    public fun appendAlternatives(vararg blocks: Self.() -> Unit)

    /**
     * Appends a literal string to the format.
     * When formatting, the string is appended to the result as is,
     * and when parsing, the string is expected to be present in the input.
     */
    public fun appendLiteral(string: String)

    /**
     * Appends a format string to the format.
     *
     * TODO. For now, see docs for [kotlinx.datetime.internal.format.appendFormatString].
     *
     * @throws IllegalArgumentException if the format string is invalid.
     */
    public fun appendFormatString(formatString: String)
}

/**
 * Appends an optional section to the format.
 *
 * When parsing, the section is parsed if it is present in the input.
 *
 * When formatting, the section is formatted if the value of any field in the block is not equal to the default value.
 * Only [appendOptional] calls where all the fields have default values are permitted when formatting.
 *
 * Example:
 * ```
 * appendHours()
 * appendLiteral(":")
 * appendMinutes()
 * appendOptional {
 *   appendLiteral(":")
 *   appendSeconds()
 * }
 * ```
 *
 * Here, because seconds have the default value of zero, they are formatted only if they are not equal to zero.
 *
 * This is a shorthand for `appendAlternatives({}, block)`.
 */
public fun <Self> FormatBuilder<Self>.appendOptional(block: Self.() -> Unit): Unit =
    appendAlternatives({}, block)

/**
 * Appends a literal character to the format.
 *
 * This is a shorthand for `appendLiteral(char.toString())`.
 */
public fun <Self> FormatBuilder<Self>.appendLiteral(char: Char): Unit = appendLiteral(char.toString())

internal interface AbstractFormatBuilder<Target, out UserSelf, ActualSelf> :
    FormatBuilder<UserSelf> where ActualSelf : AbstractFormatBuilder<Target, UserSelf, ActualSelf> {

    val actualBuilder: AppendableFormatStructure<Target>
    fun createEmpty(): ActualSelf
    fun castToGeneric(actualSelf: ActualSelf): UserSelf

    override fun appendAlternatives(vararg blocks: UserSelf.() -> Unit) {
        actualBuilder.add(AlternativesFormatStructure(blocks.map { block ->
            createEmpty().also { block(castToGeneric(it)) }.actualBuilder.build()
        }))
    }

    override fun appendLiteral(string: String) = actualBuilder.add(ConstantFormatStructure(string))

    override fun appendFormatString(formatString: String) {
        val end = actualBuilder.appendFormatString(formatString)
        require(end == formatString.length) {
            "Unexpected char '${formatString[end]}' in $formatString at position $end"
        }
    }

    fun withSharedSign(outputPlus: Boolean, block: UserSelf.() -> Unit) {
        actualBuilder.add(
            SignedFormatStructure(
                createEmpty().also { block(castToGeneric(it)) }.actualBuilder.build(),
                outputPlus
            )
        )
    }

    fun build(): Format<Target> = Format(actualBuilder.build())
}

internal interface Copyable<Self> {
    fun copy(): Self
}
