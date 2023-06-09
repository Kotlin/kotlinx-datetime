/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*

/**
 * Functions specific to the date-time format builders containing the local-time fields.
 */
public sealed interface TimeFormatBuilderFields: FormatBuilder {
    /**
     * Appends the number of hours.
     *
     * The number is padded with zeroes to the specified [minLength] when formatting.
     * When parsing, the number is expected to be at least [minLength] digits long.
     *
     * @throws IllegalArgumentException if [minLength] is not in the range 1..2.
     */
    public fun appendHour(minLength: Int = 1)

    /**
     * Appends the number of hours in the 12-hour clock.
     *
     * The number is padded with zeroes to the specified [minLength] when formatting.
     * When parsing, the number is expected to be at least [minLength] digits long.
     *
     * @throws IllegalArgumentException if [minLength] is not in the range 1..2.
     */
    public fun appendAmPmHour(minLength: Int = 1)

    /**
     * Appends the AM/PM marker, using the specified strings.
     *
     * [amString] is used for the AM marker (0-11 hours), [pmString] is used for the PM marker (12-23 hours).
     */
    public fun appendAmPmMarker(amString: String, pmString: String)

    /**
     * Appends the number of minutes.
     *
     * The number is padded with zeroes to the specified [minLength] when formatting.
     * When parsing, the number is expected to be at least [minLength] digits long.
     *
     * @throws IllegalArgumentException if [minLength] is not in the range 1..2.
     */
    public fun appendMinute(minLength: Int = 1)

    /**
     * Appends the number of seconds.
     *
     * The number is padded with zeroes to the specified [minLength] when formatting.
     * When parsing, the number is expected to be at least [minLength] digits long.
     *
     * @throws IllegalArgumentException if [minLength] is not in the range 1..2.
     */
    public fun appendSecond(minLength: Int = 1)

    /**
     * Appends the fractional part of the second without the leading dot.
     *
     * When formatting, the decimal fraction will add trailing zeroes to the specified [minLength] and will round the
     * number to fit in the specified [maxLength].
     *
     * @throws IllegalArgumentException if [minLength] is greater than [maxLength] or if either is not in the range 1..9.
     */
    public fun appendSecondFraction(minLength: Int = 1, maxLength: Int? = null)
}

internal fun TimeFormatBuilderFields.appendIsoTime() {
    appendHour(2)
    appendLiteral(':')
    appendMinute(2)
    appendOptional {
        appendLiteral(':')
        appendSecond(2)
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

internal class HourDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(TimeFields.hour, minDigits) {
    override val builderRepresentation: String = "${TimeFormatBuilderFields::appendHour.name}($minDigits)"
}

internal class AmPmHourDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(TimeFields.hourOfAmPm, minDigits) {
    override val builderRepresentation: String = "${TimeFormatBuilderFields::appendAmPmHour.name}($minDigits)"
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

internal class MinuteDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(TimeFields.minute, minDigits) {

    override val builderRepresentation: String = "${TimeFormatBuilderFields::appendMinute.name}($minDigits)"
}

internal class SecondDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(TimeFields.second, minDigits) {

    override val builderRepresentation: String = "${TimeFormatBuilderFields::appendSecond.name}($minDigits)"
}

internal class FractionalSecondDirective(minDigits: Int, maxDigits: Int? = null) :
    DecimalFractionFieldFormatDirective<TimeFieldContainer>(TimeFields.fractionOfSecond, minDigits, maxDigits) {

    override val builderRepresentation: String = when {
        minDigits == 1 && maxDigits == null -> "${TimeFormatBuilderFields::appendSecondFraction.name}()"
        minDigits == 1 -> "${TimeFormatBuilderFields::appendSecondFraction.name}(maxLength = $maxDigits)"
        maxDigits == null -> "${TimeFormatBuilderFields::appendSecondFraction.name}($minDigits)"
        else -> "${TimeFormatBuilderFields::appendSecondFraction.name}($minDigits, $maxDigits)"
    }
}

internal class LocalTimeFormat(private val actualFormat: StringFormat<TimeFieldContainer>) :
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

        val ISO: LocalTimeFormat = build {
            appendIsoTime()
        }
    }

    internal class Builder(override val actualBuilder: AppendableFormatStructure<TimeFieldContainer>) :
        AbstractFormatBuilder<TimeFieldContainer, Builder>, TimeFormatBuilderFields {
        override fun appendHour(minLength: Int) = actualBuilder.add(BasicFormatStructure(HourDirective(minLength)))
        override fun appendAmPmHour(minLength: Int) =
            actualBuilder.add(BasicFormatStructure(AmPmHourDirective(minLength)))

        override fun appendAmPmMarker(amString: String, pmString: String) =
            actualBuilder.add(BasicFormatStructure(AmPmMarkerDirective(amString, pmString)))

        override fun appendMinute(minLength: Int) = actualBuilder.add(BasicFormatStructure(MinuteDirective(minLength)))
        override fun appendSecond(minLength: Int) = actualBuilder.add(BasicFormatStructure(SecondDirective(minLength)))
        override fun appendSecondFraction(minLength: Int, maxLength: Int?) =
            actualBuilder.add(BasicFormatStructure(FractionalSecondDirective(minLength, maxLength)))

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }

    override fun toString(): String = actualFormat.builderString()

}
