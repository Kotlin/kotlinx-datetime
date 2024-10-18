/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.parser.Copyable
import kotlin.math.*

internal interface UtcOffsetFieldContainer {
    var isNegative: Boolean?
    var totalHoursAbs: Int?
    var minutesOfHour: Int?
    var secondsOfMinute: Int?
}

internal interface AbstractWithOffsetBuilder : DateTimeFormatBuilder.WithUtcOffset {
    fun addFormatStructureForOffset(structure: FormatStructure<UtcOffsetFieldContainer>)

    override fun offsetHours(padding: Padding) =
        addFormatStructureForOffset(
            SignedFormatStructure(
                BasicFormatStructure(UtcOffsetWholeHoursDirective(padding)),
                withPlusSign = true
            )
        )

    override fun offsetMinutesOfHour(padding: Padding) =
        addFormatStructureForOffset(BasicFormatStructure(UtcOffsetMinuteOfHourDirective(padding)))

    override fun offsetSecondsOfMinute(padding: Padding) =
        addFormatStructureForOffset(BasicFormatStructure(UtcOffsetSecondOfMinuteDirective(padding)))

    @Suppress("NO_ELSE_IN_WHEN")
    override fun offset(format: DateTimeFormat<UtcOffset>) = when (format) {
        is UtcOffsetFormat -> addFormatStructureForOffset(format.actualFormat)
    }
}

internal class UtcOffsetFormat(override val actualFormat: CachedFormatStructure<UtcOffsetFieldContainer>) :
    AbstractDateTimeFormat<UtcOffset, IncompleteUtcOffset>() {
    companion object {
        fun build(block: DateTimeFormatBuilder.WithUtcOffset.() -> Unit): UtcOffsetFormat {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return UtcOffsetFormat(builder.build())
        }
    }

    private class Builder(override val actualBuilder: AppendableFormatStructure<UtcOffsetFieldContainer>) :
        AbstractDateTimeFormatBuilder<UtcOffsetFieldContainer, Builder>, AbstractWithOffsetBuilder {

        override fun addFormatStructureForOffset(structure: FormatStructure<UtcOffsetFieldContainer>) {
            actualBuilder.add(structure)
        }

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }

    override fun intermediateFromValue(value: UtcOffset): IncompleteUtcOffset =
        IncompleteUtcOffset().apply { populateFrom(value) }

    override fun valueFromIntermediate(intermediate: IncompleteUtcOffset): UtcOffset = intermediate.toUtcOffset()

    override val emptyIntermediate: IncompleteUtcOffset get() = emptyIncompleteUtcOffset

}

internal enum class WhenToOutput {
    NEVER,
    IF_NONZERO,
    ALWAYS;
}

internal fun <T : DateTimeFormatBuilder> T.outputIfNeeded(whenToOutput: WhenToOutput, format: T.() -> Unit) {
    when (whenToOutput) {
        WhenToOutput.NEVER -> {}
        WhenToOutput.IF_NONZERO -> {
            optional {
                format()
            }
        }

        WhenToOutput.ALWAYS -> {
            format()
        }
    }
}

internal fun DateTimeFormatBuilder.WithUtcOffset.isoOffset(
    zOnZero: Boolean,
    useSeparator: Boolean,
    outputMinute: WhenToOutput,
    outputSecond: WhenToOutput
) {
    require(outputMinute >= outputSecond) { "Seconds cannot be included without minutes" }
    fun DateTimeFormatBuilder.WithUtcOffset.appendIsoOffsetWithoutZOnZero() {
        offsetHours()
        outputIfNeeded(outputMinute) {
            if (useSeparator) {
                char(':')
            }
            offsetMinutesOfHour()
            outputIfNeeded(outputSecond) {
                if (useSeparator) {
                    char(':')
                }
                offsetSecondsOfMinute()
            }
        }
    }
    if (zOnZero) {
        optional("Z") {
            alternativeParsing({
                char('z')
            }) {
                appendIsoOffsetWithoutZOnZero()
            }
        }
    } else {
        appendIsoOffsetWithoutZOnZero()
    }
}

private object OffsetFields {
    private val sign = object : FieldSign<UtcOffsetFieldContainer> {
        override val isNegative = PropertyAccessor(UtcOffsetFieldContainer::isNegative)
        override fun isZero(obj: UtcOffsetFieldContainer): Boolean =
            (obj.totalHoursAbs ?: 0) == 0 && (obj.minutesOfHour ?: 0) == 0 && (obj.secondsOfMinute ?: 0) == 0
    }
    val totalHoursAbs = UnsignedFieldSpec(
        PropertyAccessor(UtcOffsetFieldContainer::totalHoursAbs),
        defaultValue = 0,
        minValue = 0,
        maxValue = 18,
        sign = sign,
    )
    val minutesOfHour = UnsignedFieldSpec(
        PropertyAccessor(UtcOffsetFieldContainer::minutesOfHour),
        defaultValue = 0,
        minValue = 0,
        maxValue = 59,
        sign = sign,
    )
    val secondsOfMinute = UnsignedFieldSpec(
        PropertyAccessor(UtcOffsetFieldContainer::secondsOfMinute),
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

    fun populateFrom(offset: UtcOffset) {
        isNegative = offset.totalSeconds < 0
        val totalSecondsAbs = offset.totalSeconds.absoluteValue
        totalHoursAbs = totalSecondsAbs / 3600
        minutesOfHour = (totalSecondsAbs / 60) % 60
        secondsOfMinute = totalSecondsAbs % 60
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

internal class UtcOffsetWholeHoursDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<UtcOffsetFieldContainer>(
        OffsetFields.totalHoursAbs,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {

    override val builderRepresentation: String
        get() =
            "${DateTimeFormatBuilder.WithUtcOffset::offsetHours.name}(${padding.toKotlinCode()})"

    override fun equals(other: Any?): Boolean = other is UtcOffsetWholeHoursDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

private class UtcOffsetMinuteOfHourDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<UtcOffsetFieldContainer>(
        OffsetFields.minutesOfHour,
        minDigits = padding.minDigits(2), spacePadding = padding.spaces(2)
    ) {

    override val builderRepresentation: String
        get() = when (padding) {
            Padding.NONE -> "${DateTimeFormatBuilder.WithUtcOffset::offsetMinutesOfHour.name}()"
            else -> "${DateTimeFormatBuilder.WithUtcOffset::offsetMinutesOfHour.name}(${padding.toKotlinCode()})"
        }

    override fun equals(other: Any?): Boolean = other is UtcOffsetMinuteOfHourDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

private class UtcOffsetSecondOfMinuteDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<UtcOffsetFieldContainer>(
        OffsetFields.secondsOfMinute,
        minDigits = padding.minDigits(2), spacePadding = padding.spaces(2)
    ) {

    override val builderRepresentation: String
        get() = when (padding) {
            Padding.NONE -> "${DateTimeFormatBuilder.WithUtcOffset::offsetSecondsOfMinute.name}()"
            else -> "${DateTimeFormatBuilder.WithUtcOffset::offsetSecondsOfMinute.name}(${padding.toKotlinCode()})"
        }

    override fun equals(other: Any?): Boolean = other is UtcOffsetSecondOfMinuteDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

// these are constants so that the formats are not recreated every time they are used
internal val ISO_OFFSET by lazy {
    UtcOffsetFormat.build {
        alternativeParsing({ chars("z") }) {
            optional("Z") {
                offsetHours()
                char(':')
                offsetMinutesOfHour()
                optional {
                    char(':')
                    offsetSecondsOfMinute()
                }
            }
        }
    }
}
internal val ISO_OFFSET_BASIC by lazy {
    UtcOffsetFormat.build {
        alternativeParsing({ chars("z") }) {
            optional("Z") {
                offsetHours()
                optional {
                    offsetMinutesOfHour()
                    optional {
                        offsetSecondsOfMinute()
                    }
                }
            }
        }
    }
}

internal val FOUR_DIGIT_OFFSET by lazy {
    UtcOffsetFormat.build {
        offsetHours()
        offsetMinutesOfHour()
    }
}

private val emptyIncompleteUtcOffset = IncompleteUtcOffset()
