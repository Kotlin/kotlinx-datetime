/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*
import kotlin.math.*

internal interface UtcOffsetFieldContainer {
    var isNegative: Boolean?
    var totalHoursAbs: Int?
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

public class UtcOffsetFormat internal constructor(private val actualFormat: StringFormat<UtcOffsetFieldContainer>) :
    Format<UtcOffset> by UtcOffsetFormatImpl(actualFormat) {
    public companion object {
        public fun build(block: UtcOffsetFormatBuilder.() -> Unit): UtcOffsetFormat {
            val builder = Builder(AppendableFormatStructure(UtcOffsetFormatBuilderSpec))
            builder.block()
            return UtcOffsetFormat(builder.build())
        }

        public fun fromFormatString(formatString: String): UtcOffsetFormat = build { appendFormatString(formatString) }

        internal val Cache = LruCache<String, UtcOffsetFormat>(16) { fromFormatString(it) }
    }

    private class Builder(override val actualBuilder: AppendableFormatStructure<UtcOffsetFieldContainer>) :
        AbstractFormatBuilder<UtcOffsetFieldContainer, UtcOffsetFormatBuilder, Builder>, UtcOffsetFormatBuilder {

        override fun createEmpty(): Builder = Builder(actualBuilder.createSibling())
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

    override fun toString(): String = actualFormat.toString()

}

public fun UtcOffset.format(formatString: String): String =
    UtcOffsetFormat.Cache.get(formatString).format(this)

public fun UtcOffset.format(format: UtcOffsetFormat): String = format.format(this)

public fun UtcOffset.Companion.parse(input: String, formatString: String): UtcOffset =
    UtcOffsetFormat.Cache.get(formatString).parse(input)

public fun UtcOffset.Companion.parse(input: String, format: UtcOffsetFormat): UtcOffset = format.parse(input)

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
    override val formatStringRepresentation: Pair<String?, String> =
        UtcOffsetFormatBuilderSpec.name to "H".repeat(minDigits)

    override val builderRepresentation: String =
        "${UtcOffsetFormatBuilder::appendOffsetTotalHours.name}($minDigits)"
}

internal class UtcOffsetMinuteOfHourDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<UtcOffsetFieldContainer>(OffsetFields.minutesOfHour, minDigits) {
    override val formatStringRepresentation: Pair<String?, String> =
        UtcOffsetFormatBuilderSpec.name to "m".repeat(minDigits)

    override val builderRepresentation: String =
        "${UtcOffsetFormatBuilder::appendOffsetMinutesOfHour.name}($minDigits)"
}

internal class UtcOffsetSecondOfMinuteDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<UtcOffsetFieldContainer>(OffsetFields.secondsOfMinute, minDigits) {
    override val formatStringRepresentation: Pair<String?, String> =
        UtcOffsetFormatBuilderSpec.name to "s".repeat(minDigits)

    override val builderRepresentation: String =
        "${UtcOffsetFormatBuilder::appendOffsetSecondsOfMinute.name}($minDigits)"
}

internal object UtcOffsetFormatBuilderSpec : BuilderSpec<UtcOffsetFieldContainer>(
    mapOf(
        "uo" to UtcOffsetFormatBuilderSpec
    ),
    mapOf(
        'H' to { length -> BasicFormatStructure(UtcOffsetWholeHoursDirective(length)) },
        'm' to { length -> BasicFormatStructure(UtcOffsetMinuteOfHourDirective(length)) },
        's' to { length -> BasicFormatStructure(UtcOffsetSecondOfMinuteDirective(length)) },
    )
) {
    const val name = "uo"
}

private class UtcOffsetFormatImpl(actualFormat: StringFormat<UtcOffsetFieldContainer>) :
    AbstractFormat<UtcOffset, IncompleteUtcOffset>(actualFormat) {
    override fun intermediateFromValue(value: UtcOffset): IncompleteUtcOffset = value.toIncompleteUtcOffset()

    override fun valueFromIntermediate(intermediate: IncompleteUtcOffset): UtcOffset = intermediate.toUtcOffset()

    override fun newIntermediate(): IncompleteUtcOffset = IncompleteUtcOffset()
}
