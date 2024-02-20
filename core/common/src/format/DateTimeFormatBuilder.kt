/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.internal.format.*

/**
 * Common functions for all format builders.
 */
public sealed interface DateTimeFormatBuilder {
    /**
     * A literal string.
     *
     * When formatting, the string is appended to the result as is,
     * and when parsing, the string is expected to be present in the input verbatim.
     */
    public fun chars(value: String)
}

/**
 * A format along with other ways to parse the same portion of the value.
 *
 * When parsing, first, [primaryFormat] is used; if parsing the whole string fails using that, the formats
 * from [alternativeFormats] are tried in order.
 *
 * When formatting, the [primaryFormat] is used to format the value, and [alternativeFormats] are ignored.
 *
 * Example:
 * ```
 * alternativeParsing(
 *   { dayOfMonth(); char('-'); monthNumber() },
 *   { monthNumber(); char(' '); dayOfMonth() },
 * ) { monthNumber(); char('/'); dayOfMonth() }
 * ```
 *
 * This will always format a date as `MM/DD`, but will also accept `DD-MM` and `MM DD`.
 */
@Suppress("UNCHECKED_CAST")
public fun <T : DateTimeFormatBuilder> T.alternativeParsing(
    vararg alternativeFormats: T.() -> Unit,
    primaryFormat: T.() -> Unit
): Unit = when (this) {
    is AbstractDateTimeFormatBuilder<*, *> ->
        appendAlternativeParsingImpl(
            *alternativeFormats as Array<out AbstractDateTimeFormatBuilder<*, *>.() -> Unit>,
            mainFormat = primaryFormat as (AbstractDateTimeFormatBuilder<*, *>.() -> Unit)
        )

    else -> throw IllegalStateException("impossible")
}

/**
 * An optional section.
 *
 * When formatting, the section is formatted if the value of any field in the block is not equal to the default value.
 * Only [optional] calls where all the fields have default values are permitted.
 *
 * Example:
 * ```
 * offsetHours(); char(':'); offsetMinutesOfHour()
 * optional { char(':'); offsetSecondsOfMinute() }
 * ```
 *
 * Here, because seconds have the default value of zero, they are formatted only if they are not equal to zero, so the
 * UTC offset `+18:30:00` gets formatted as `"+18:30"`, but `+18:30:01` becomes `"+18:30:01"`.
 *
 * When parsing, either [format] or, if that fails, the literal [ifZero] are parsed. If the [ifZero] string is parsed,
 * the values in [format] get assigned their default values.
 *
 * [ifZero] defines the string that is used if values are the default ones.
 *
 * @throws IllegalArgumentException if not all fields used in [format] have a default value.
 */
@Suppress("UNCHECKED_CAST")
public fun <T : DateTimeFormatBuilder> T.optional(ifZero: String = "", format: T.() -> Unit): Unit = when (this) {
    is AbstractDateTimeFormatBuilder<*, *> -> appendOptionalImpl(
        onZero = ifZero,
        format as (AbstractDateTimeFormatBuilder<*, *>.() -> Unit)
    )

    else -> throw IllegalStateException("impossible")
}

/**
 * A literal character.
 *
 * This is a shorthand for `chars(value.toString())`.
 */
public fun DateTimeFormatBuilder.char(value: Char): Unit = chars(value.toString())

internal interface AbstractDateTimeFormatBuilder<Target, ActualSelf> :
    DateTimeFormatBuilder where ActualSelf : AbstractDateTimeFormatBuilder<Target, ActualSelf> {

    val actualBuilder: AppendableFormatStructure<Target>
    fun createEmpty(): ActualSelf

    fun appendAlternativeParsingImpl(
        vararg otherFormats: ActualSelf.() -> Unit,
        mainFormat: ActualSelf.() -> Unit
    ) {
        val others = otherFormats.map { block ->
            createEmpty().also { block(it) }.actualBuilder.build()
        }
        val main = createEmpty().also { mainFormat(it) }.actualBuilder.build()
        actualBuilder.add(AlternativesParsingFormatStructure(main, others))
    }

    fun appendOptionalImpl(
        onZero: String,
        format: ActualSelf.() -> Unit
    ) {
        actualBuilder.add(OptionalFormatStructure(onZero, createEmpty().also { format(it) }.actualBuilder.build()))
    }

    override fun chars(value: String) = actualBuilder.add(ConstantFormatStructure(value))

    fun build(): CachedFormatStructure<Target> = CachedFormatStructure(actualBuilder.build().formats)
}
