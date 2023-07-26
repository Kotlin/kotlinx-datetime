/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*
import kotlin.native.concurrent.*

/**
 * Functions specific to the date-time format builders containing the local-time fields.
 */
public sealed interface TimeFormatBuilderFields : FormatBuilder {
    /**
     * Appends the number of hours.
     */
    public fun appendHour(padding: Padding = Padding.ZERO)

    /**
     * Appends the number of hours in the 12-hour clock.
     */
    public fun appendAmPmHour(padding: Padding = Padding.ZERO)

    /**
     * Appends the AM/PM marker, using the specified strings.
     *
     * [amString] is used for the AM marker (0-11 hours), [pmString] is used for the PM marker (12-23 hours).
     */
    public fun appendAmPmMarker(amString: String, pmString: String)

    /**
     * Appends the number of minutes.
     */
    public fun appendMinute(padding: Padding = Padding.ZERO)

    /**
     * Appends the number of seconds.
     */
    public fun appendSecond(padding: Padding = Padding.ZERO)

    /**
     * Appends the fractional part of the second without the leading dot.
     *
     * When formatting, the decimal fraction will add trailing zeroes to the specified [minLength] and will round the
     * number to fit in the specified [maxLength]. If [minLength] is `null`, the fraction will be formatted with
     * enough trailing zeros to make the number of digits displayed a multiple of three (e.g. `123.450`) for
     * readability. Explicitly set [minLength] to `1` to disable this behavior.
     *
     * @throws IllegalArgumentException if [minLength] is greater than [maxLength] or if either is not in the range 1..9.
     */
    public fun appendSecondFraction(minLength: Int? = null, maxLength: Int? = null)
}

internal fun TimeFormatBuilderFields.appendIsoTime() {
    appendHour()
    appendLiteral(':')
    appendMinute()
    appendOptional {
        appendLiteral(':')
        appendSecond()
        appendOptional {
            appendLiteral('.')
            appendSecondFraction()
        }
    }
}

internal fun LocalTime.toIncompleteLocalTime(): IncompleteLocalTime =
    IncompleteLocalTime(hour, minute, second, nanosecond)

internal interface TimeFieldContainer {
    var minute: Int?
    var second: Int?
    var nanosecond: Int?
    var hour: Int?
    var hourOfAmPm: Int?
    var isPm: Boolean?

    var fractionOfSecond: DecimalFraction?
        get() = nanosecond?.let { DecimalFraction(it, 9) }
        set(value) {
            nanosecond = value?.fractionalPartWithNDigits(9)
        }
}

internal object TimeFields {
    val hour = UnsignedFieldSpec(TimeFieldContainer::hour, minValue = 0, maxValue = 23)
    val minute = UnsignedFieldSpec(TimeFieldContainer::minute, minValue = 0, maxValue = 59)
    val second = UnsignedFieldSpec(TimeFieldContainer::second, minValue = 0, maxValue = 59, defaultValue = 0)
    val fractionOfSecond = GenericFieldSpec(TimeFieldContainer::fractionOfSecond, defaultValue = DecimalFraction(0, 9))
    val isPm = GenericFieldSpec(TimeFieldContainer::isPm)
    val hourOfAmPm = UnsignedFieldSpec(TimeFieldContainer::hourOfAmPm, minValue = 1, maxValue = 12)
}

internal class IncompleteLocalTime(
    hour: Int? = null,
    isPm: Boolean? = null,
    override var minute: Int? = null,
    override var second: Int? = null,
    override var nanosecond: Int? = null
) : TimeFieldContainer, Copyable<IncompleteLocalTime> {
    constructor(hour: Int?, minute: Int?, second: Int?, nanosecond: Int?) :
        this(hour, hour?.let { it >= 12 }, minute, second, nanosecond)

    // stores the hour in 24-hour format if `isPm` is not null, otherwise stores the hour in 12-hour format.
    var hourField: Int? = hour

    override var hourOfAmPm: Int?
        get() = hourField?.let { (it + 11) % 12 + 1 }
        set(value) {
            hourField = value?.let { it + if (isPm == true) 12 else 0 }
        }

    override var isPm: Boolean? = isPm
        set(value) {
            if (value != null) {
                hourField = hourField?.let { (it % 12) + if (value) 12 else 0 }
            }
            field = value
        }

    override var hour: Int?
        get() = if (isPm != null) hourField else null
        set(value) {
            if (value != null) {
                isPm = value.mod(24) >= 12
                hourField = value
            } else {
                isPm = null
                hourField = null
            }
        }

    fun toLocalTime(): LocalTime = LocalTime(
        getParsedField(hour, "hour"),
        getParsedField(minute, "minute"),
        second ?: 0,
        nanosecond ?: 0,
    )

    override fun copy(): IncompleteLocalTime = IncompleteLocalTime(hour, isPm, minute, second, nanosecond)

    override fun equals(other: Any?): Boolean =
        other is IncompleteLocalTime && hourField == other.hourField && minute == other.minute &&
            second == other.second && nanosecond == other.nanosecond

    override fun hashCode(): Int =
        hourField.hashCode() * 31 + minute.hashCode() * 31 + second.hashCode() * 31 + nanosecond.hashCode()

    override fun toString(): String =
        "${hour ?: "??"}:${minute ?: "??"}:${second ?: "??"}.${
            nanosecond?.let { nanos ->
                nanos.toString().let { it.padStart(9 - it.length, '0') }
            } ?: "???"
        }"
}

internal class HourDirective(padding: Padding) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(
        TimeFields.hour,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {
    override val builderRepresentation: String = when (padding) {
        Padding.ZERO -> "${TimeFormatBuilderFields::appendHour.name}()"
        else -> "${TimeFormatBuilderFields::appendHour.name}($padding)"
    }
}

internal class AmPmHourDirective(padding: Padding) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(
        TimeFields.hourOfAmPm, minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {
    override val builderRepresentation: String = when (padding) {
        Padding.ZERO -> "${TimeFormatBuilderFields::appendAmPmHour.name}()"
        else -> "${TimeFormatBuilderFields::appendAmPmHour.name}($padding)"
    }
}

internal class AmPmMarkerDirective(amString: String, pmString: String) :
    NamedEnumIntFieldFormatDirective<TimeFieldContainer, Boolean>(
        TimeFields.isPm, mapOf(
            false to amString,
            true to pmString,
        )
    ) {

    override val builderRepresentation: String =
        "${TimeFormatBuilderFields::appendAmPmMarker.name}($amString, $pmString)"
}

internal class MinuteDirective(padding: Padding) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(
        TimeFields.minute,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {

    override val builderRepresentation: String = when (padding) {
        Padding.ZERO -> "${TimeFormatBuilderFields::appendMinute.name}()"
        else -> "${TimeFormatBuilderFields::appendMinute.name}($padding)"
    }
}

internal class SecondDirective(padding: Padding) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(
        TimeFields.second,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {

    override val builderRepresentation: String = when (padding) {
        Padding.ZERO -> "${TimeFormatBuilderFields::appendSecond.name}()"
        else -> "${TimeFormatBuilderFields::appendSecond.name}($padding)"
    }
}

internal class FractionalSecondDirective(minDigits: Int? = null, maxDigits: Int? = null) :
    DecimalFractionFieldFormatDirective<TimeFieldContainer>(TimeFields.fractionOfSecond, minDigits, maxDigits) {

    override val builderRepresentation: String = when {
        minDigits == 1 && maxDigits == null -> "${TimeFormatBuilderFields::appendSecondFraction.name}()"
        minDigits == 1 -> "${TimeFormatBuilderFields::appendSecondFraction.name}(maxLength = $maxDigits)"
        maxDigits == null -> "${TimeFormatBuilderFields::appendSecondFraction.name}($minDigits)"
        else -> "${TimeFormatBuilderFields::appendSecondFraction.name}($minDigits, $maxDigits)"
    }
}

internal class LocalTimeFormat(val actualFormat: StringFormat<TimeFieldContainer>) :
    AbstractFormat<LocalTime, IncompleteLocalTime>(actualFormat) {
    override fun intermediateFromValue(value: LocalTime): IncompleteLocalTime = value.toIncompleteLocalTime()

    override fun valueFromIntermediate(intermediate: IncompleteLocalTime): LocalTime = intermediate.toLocalTime()

    override fun newIntermediate(): IncompleteLocalTime = IncompleteLocalTime()

    companion object {
        fun build(block: TimeFormatBuilderFields.() -> Unit): LocalTimeFormat {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return LocalTimeFormat(builder.build())
        }

    }

    internal class Builder(override val actualBuilder: AppendableFormatStructure<TimeFieldContainer>) :
        AbstractFormatBuilder<TimeFieldContainer, Builder>, TimeFormatBuilderFields {
        override fun appendHour(padding: Padding) = actualBuilder.add(BasicFormatStructure(HourDirective(padding)))
        override fun appendAmPmHour(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(AmPmHourDirective(padding)))

        override fun appendAmPmMarker(amString: String, pmString: String) =
            actualBuilder.add(BasicFormatStructure(AmPmMarkerDirective(amString, pmString)))

        override fun appendMinute(padding: Padding) = actualBuilder.add(BasicFormatStructure(MinuteDirective(padding)))
        override fun appendSecond(padding: Padding) = actualBuilder.add(BasicFormatStructure(SecondDirective(padding)))
        override fun appendSecondFraction(minLength: Int?, maxLength: Int?) =
            actualBuilder.add(BasicFormatStructure(FractionalSecondDirective(minLength, maxLength)))

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }

    override fun toString(): String = actualFormat.builderString()

}

// these are constants so that the formats are not recreated every time they are used
@SharedImmutable
internal val ISO_TIME by lazy {
    LocalTimeFormat.build { appendIsoTime() }
}
@SharedImmutable
internal val ISO_TIME_BASIC by lazy {
    LocalTimeFormat.build {
        alternativeParsing({ appendLiteral('t') }) { appendLiteral('T') }
        appendHour(); appendMinute()
        appendOptional {
            appendSecond()
            appendOptional {
                appendLiteral('.')
                appendSecondFraction()
            }
        }
    }
}
