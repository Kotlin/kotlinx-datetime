/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.parser.*

internal interface UtcOffsetFieldContainer {
    var totalHours: Int?
    var minutesOfHour: Int?
    var secondsOfMinute: Int?
}

public interface UtcOffsetFormatBuilderFields {
    public fun appendOffsetTotalHours(minDigits: Int = 1)
    public fun appendOffsetMinutesOfHour(minDigits: Int = 1)
    public fun appendOffsetSecondsOfMinute(minDigits: Int = 1)
}

@DateTimeBuilder
public interface UtcOffsetFormatBuilder : UtcOffsetFormatBuilderFields, FormatBuilder<UtcOffsetFormatBuilder> {
    public fun withSharedSign(outputPlus: Boolean, block: UtcOffsetFormatBuilder.() -> Unit)
}

public class UtcOffsetFormat internal constructor(private val actualFormat: Format<UtcOffsetFieldContainer>) {
    public companion object {
        public fun build(block: UtcOffsetFormatBuilder.() -> Unit): UtcOffsetFormat {
            val builder = Builder(UtcOffsetFieldContainerFormatBuilder())
            builder.block()
            return UtcOffsetFormat(builder.build())
        }

        public fun fromFormatString(formatString: String): UtcOffsetFormat = build { appendFormatString(formatString) }

        internal val Cache = LruCache<String, UtcOffsetFormat>(16) { fromFormatString(it) }
    }

    public fun format(date: UtcOffset): String =
        StringBuilder().also {
            actualFormat.formatter.format(date.toIncompleteUtcOffset(), it)
        }.toString()

    public fun parse(input: String): UtcOffset {
        val parser = Parser(::IncompleteUtcOffset, IncompleteUtcOffset::copy, actualFormat.parser)
        try {
            return parser.match(input).toUtcOffset()
        } catch (e: ParseException) {
            throw DateTimeFormatException("Failed to parse date from '$input'", e)
        } catch (e: IllegalArgumentException) {
            throw DateTimeFormatException("Invalid date '$input'", e)
        }
    }

    private class Builder(override val actualBuilder: UtcOffsetFieldContainerFormatBuilder) :
        AbstractFormatBuilder<UtcOffsetFieldContainer, UtcOffsetFormatBuilder, Builder>, UtcOffsetFormatBuilder {

        override fun createEmpty(): Builder = Builder(UtcOffsetFieldContainerFormatBuilder())
        override fun castToGeneric(actualSelf: Builder): UtcOffsetFormatBuilder = this
        override fun appendOffsetTotalHours(minDigits: Int) =
            actualBuilder.add(BasicFormatStructure(UtcOffsetWholeHoursDirective(minDigits)))

        override fun appendOffsetMinutesOfHour(minDigits: Int) =
            actualBuilder.add(BasicFormatStructure(UtcOffsetMinuteOfHourDirective(minDigits)))

        override fun appendOffsetSecondsOfMinute(minDigits: Int) =
            actualBuilder.add(BasicFormatStructure(UtcOffsetSecondOfMinuteDirective(minDigits)))

        override fun withSharedSign(outputPlus: Boolean, block: UtcOffsetFormatBuilder.() -> Unit) =
            super.withSharedSign(outputPlus, block)
    }

}

public fun UtcOffset.format(formatString: String): String =
    UtcOffsetFormat.Cache.get(formatString).format(this)

public fun UtcOffset.format(format: UtcOffsetFormat): String = format.format(this)

public fun UtcOffset.Companion.parse(input: String, formatString: String): UtcOffset =
    UtcOffsetFormat.Cache.get(formatString).parse(input)

public fun UtcOffset.Companion.parse(input: String, format: UtcOffsetFormat): UtcOffset = format.parse(input)

internal fun UtcOffset.toIncompleteUtcOffset(): IncompleteUtcOffset =
    IncompleteUtcOffset(totalSeconds / 3600, (totalSeconds / 60) % 60, totalSeconds % 60)

internal object OffsetFields {
    val totalHours = SignedFieldSpec(
        UtcOffsetFieldContainer::totalHours,
        defaultValue = 0,
        maxAbsoluteValue = 18,
    )
    val minutesOfHour = SignedFieldSpec(
        UtcOffsetFieldContainer::minutesOfHour,
        defaultValue = 0,
        maxAbsoluteValue = 59,
    )
    val secondsOfMinute = SignedFieldSpec(
        UtcOffsetFieldContainer::secondsOfMinute,
        defaultValue = 0,
        maxAbsoluteValue = 59,
    )
}

internal class IncompleteUtcOffset(
    override var totalHours: Int? = null,
    override var minutesOfHour: Int? = null,
    override var secondsOfMinute: Int? = null,
) : UtcOffsetFieldContainer, Copyable<IncompleteUtcOffset> {
    fun toUtcOffset(): UtcOffset = UtcOffset(totalHours, minutesOfHour, secondsOfMinute)

    override fun copy(): IncompleteUtcOffset = IncompleteUtcOffset(totalHours, minutesOfHour, secondsOfMinute)
}

internal class UtcOffsetWholeHoursDirective(minDigits: Int) :
    SignedIntFieldFormatDirective<UtcOffsetFieldContainer>(OffsetFields.totalHours, minDigits)

internal class UtcOffsetMinuteOfHourDirective(minDigits: Int) :
    SignedIntFieldFormatDirective<UtcOffsetFieldContainer>(OffsetFields.minutesOfHour, minDigits)

internal class UtcOffsetSecondOfMinuteDirective(minDigits: Int) :
    SignedIntFieldFormatDirective<UtcOffsetFieldContainer>(OffsetFields.secondsOfMinute, minDigits)

internal class UtcOffsetFieldContainerFormatBuilder : AbstractBuilder<UtcOffsetFieldContainer>() {
    companion object {
        const val name = "uo"
    }

    override fun formatFromSubBuilder(
        name: String,
        block: Builder<*>.() -> Unit
    ): FormatStructure<UtcOffsetFieldContainer>? =
        if (name == UtcOffsetFieldContainerFormatBuilder.name) UtcOffsetFieldContainerFormatBuilder().apply(block)
            .build() else null

    override fun formatFromDirective(letter: Char, length: Int): FormatStructure<UtcOffsetFieldContainer>? {
        return when (letter) {
            'H' -> BasicFormatStructure(UtcOffsetWholeHoursDirective(length))
            'm' -> BasicFormatStructure(UtcOffsetMinuteOfHourDirective(length))
            's' -> BasicFormatStructure(UtcOffsetSecondOfMinuteDirective(length))
            else -> null
        }
    }

    override fun createSibling(): Builder<UtcOffsetFieldContainer> = UtcOffsetFieldContainerFormatBuilder()
}
