/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.format.*
import kotlin.math.*

public sealed interface UtcOffsetFormatBuilderFields: FormatBuilder {
    public fun appendOffsetTotalHours(minDigits: Int = 1)
    public fun appendOffsetMinutesOfHour(minDigits: Int = 1)
    public fun appendOffsetSecondsOfMinute(minDigits: Int = 1)
}

internal interface UtcOffsetFieldContainer {
    var isNegative: Boolean?
    var totalHoursAbs: Int?
    var minutesOfHour: Int?
    var secondsOfMinute: Int?
}

internal class UtcOffsetFormat(private val actualFormat: StringFormat<UtcOffsetFieldContainer>) :
    Format<UtcOffset> by UtcOffsetFormatImpl(actualFormat) {
    companion object {
        fun build(block: UtcOffsetFormatBuilderFields.() -> Unit): UtcOffsetFormat {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return UtcOffsetFormat(builder.build())
        }
    }

    private class Builder(override val actualBuilder: AppendableFormatStructure<UtcOffsetFieldContainer>) :
        AbstractFormatBuilder<UtcOffsetFieldContainer, Builder>, UtcOffsetFormatBuilderFields {

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
        override fun appendOffsetTotalHours(minDigits: Int) =
            actualBuilder.add(BasicFormatStructure(UtcOffsetWholeHoursDirective(minDigits)))

        override fun appendOffsetMinutesOfHour(minDigits: Int) =
            actualBuilder.add(BasicFormatStructure(UtcOffsetMinuteOfHourDirective(minDigits)))

        override fun appendOffsetSecondsOfMinute(minDigits: Int) =
            actualBuilder.add(BasicFormatStructure(UtcOffsetSecondOfMinuteDirective(minDigits)))
    }

    override fun toString(): String = actualFormat.builderString()

}

internal enum class WhenToOutput {
    NEVER,
    IF_NONZERO,
    ALWAYS,
}

internal fun UtcOffsetFormatBuilderFields.appendIsoOffset(zOnZero: Boolean, useSeparator: Boolean, outputMinute: WhenToOutput, outputSecond: WhenToOutput) {
    require(outputMinute >= outputSecond) { "Seconds cannot be included without minutes" }
    fun UtcOffsetFormatBuilderFields.appendIsoOffsetWithoutZOnZero() {
        withSharedSign(outputPlus = true) {
            appendOffsetTotalHours(2)
            when (outputMinute) {
                WhenToOutput.NEVER -> {}
                WhenToOutput.IF_NONZERO -> {
                    appendOptional {
                        if (useSeparator) { appendLiteral(':') }
                        appendOffsetMinutesOfHour(2)
                        when (outputSecond) {
                            WhenToOutput.NEVER -> {}
                            WhenToOutput.IF_NONZERO -> {
                                appendOptional {
                                    if (useSeparator) { appendLiteral(':') }
                                    appendOffsetSecondsOfMinute(2)
                                }
                            }
                            WhenToOutput.ALWAYS -> {
                                if (useSeparator) { appendLiteral(':') }
                                appendOffsetSecondsOfMinute(2)
                            }
                        }
                    }
                }
                WhenToOutput.ALWAYS -> {
                    if (useSeparator) { appendLiteral(':') }
                    appendOffsetMinutesOfHour(2)
                    when (outputSecond) {
                        WhenToOutput.NEVER -> {}
                        WhenToOutput.IF_NONZERO -> {
                            appendOptional {
                                if (useSeparator) { appendLiteral(':') }
                                appendOffsetSecondsOfMinute(2)
                            }
                        }
                        WhenToOutput.ALWAYS -> {
                            if (useSeparator) { appendLiteral(':') }
                            appendOffsetSecondsOfMinute(2)
                        }
                    }
                }
            }
        }
    }
    if (zOnZero) {
        appendAlternatives({
            appendLiteral('Z')
        }, {
            appendLiteral('z')
        }, {
            appendIsoOffsetWithoutZOnZero()
        })
    } else {
        appendIsoOffsetWithoutZOnZero()
    }
}

internal fun UtcOffset.toIncompleteUtcOffset(): IncompleteUtcOffset =
    IncompleteUtcOffset(
        totalSeconds < 0,
        totalSeconds.absoluteValue / 3600,
        (totalSeconds.absoluteValue / 60) % 60,
        totalSeconds.absoluteValue % 60
    )

internal object OffsetFields {
    private val sign = object : FieldSign<UtcOffsetFieldContainer> {
        override val isNegative = UtcOffsetFieldContainer::isNegative
        override fun isZero(obj: UtcOffsetFieldContainer): Boolean =
            (obj.totalHoursAbs ?: 0) == 0 && (obj.minutesOfHour ?: 0) == 0 && (obj.secondsOfMinute ?: 0) == 0
    }
    val totalHoursAbs = UnsignedFieldSpec(
        UtcOffsetFieldContainer::totalHoursAbs,
        defaultValue = 0,
        minValue = 0,
        maxValue = 18,
        sign = sign,
    )
    val minutesOfHour = UnsignedFieldSpec(
        UtcOffsetFieldContainer::minutesOfHour,
        defaultValue = 0,
        minValue = 0,
        maxValue = 59,
        sign = sign,
    )
    val secondsOfMinute = UnsignedFieldSpec(
        UtcOffsetFieldContainer::secondsOfMinute,
        defaultValue = 0,
        minValue = 0,
        maxValue = 59,
        sign = sign,
    )
}

internal class IncompleteUtcOffset(
    override var isNegative: Boolean? = null,
    override var totalHoursAbs: Int? = null,
    override var minutesOfHour: Int? = null,
    override var secondsOfMinute: Int? = null,
) : UtcOffsetFieldContainer, Copyable<IncompleteUtcOffset> {

    fun toUtcOffset(): UtcOffset {
        val sign = if (isNegative == true) -1 else 1
        return UtcOffset(
            totalHoursAbs?.let { it * sign }, minutesOfHour?.let { it * sign }, secondsOfMinute?.let { it * sign }
        )
    }

    override fun equals(other: Any?): Boolean =
        other is IncompleteUtcOffset && isNegative == other.isNegative && totalHoursAbs == other.totalHoursAbs &&
            minutesOfHour == other.minutesOfHour && secondsOfMinute == other.secondsOfMinute

    override fun hashCode(): Int =
        isNegative.hashCode() + totalHoursAbs.hashCode() + minutesOfHour.hashCode() + secondsOfMinute.hashCode()

    override fun copy(): IncompleteUtcOffset =
        IncompleteUtcOffset(isNegative, totalHoursAbs, minutesOfHour, secondsOfMinute)

    override fun toString(): String =
        "${isNegative?.let { if (it) "-" else "+" } ?: " "}${totalHoursAbs ?: "??"}:${minutesOfHour ?: "??"}:${secondsOfMinute ?: "??"}"
}

internal class UtcOffsetWholeHoursDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<UtcOffsetFieldContainer>(OffsetFields.totalHoursAbs, minDigits) {

    override val builderRepresentation: String =
        "${UtcOffsetFormatBuilderFields::appendOffsetTotalHours.name}($minDigits)"
}

internal class UtcOffsetMinuteOfHourDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<UtcOffsetFieldContainer>(OffsetFields.minutesOfHour, minDigits) {

    override val builderRepresentation: String =
        "${UtcOffsetFormatBuilderFields::appendOffsetMinutesOfHour.name}($minDigits)"
}

internal class UtcOffsetSecondOfMinuteDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<UtcOffsetFieldContainer>(OffsetFields.secondsOfMinute, minDigits) {

    override val builderRepresentation: String =
        "${UtcOffsetFormatBuilderFields::appendOffsetSecondsOfMinute.name}($minDigits)"
}

private class UtcOffsetFormatImpl(actualFormat: StringFormat<UtcOffsetFieldContainer>) :
    AbstractFormat<UtcOffset, IncompleteUtcOffset>(actualFormat) {
    override fun intermediateFromValue(value: UtcOffset): IncompleteUtcOffset = value.toIncompleteUtcOffset()

    override fun valueFromIntermediate(intermediate: IncompleteUtcOffset): UtcOffset = intermediate.toUtcOffset()

    override fun newIntermediate(): IncompleteUtcOffset = IncompleteUtcOffset()
}
