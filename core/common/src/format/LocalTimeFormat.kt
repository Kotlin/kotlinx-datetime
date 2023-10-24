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
     *
     * By default, it's zero-padded to two digits, but this can be changed with [padding].
     */
    public fun appendHour(padding: Padding = Padding.ZERO)

    /**
     * Appends the number of hours in the 12-hour clock.
     *
     * By default, it's zero-padded to two digits, but this can be changed with [padding].
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
     *
     * By default, it's zero-padded to two digits, but this can be changed with [padding].
     */
    public fun appendMinute(padding: Padding = Padding.ZERO)

    /**
     * Appends the number of seconds.
     *
     * By default, it's zero-padded to two digits, but this can be changed with [padding].
     *
     * This field has the default value of 0. If you want to omit it, use [appendOptional].
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
     * This field has the default value of 0. If you want to omit it, use [appendOptional].
     *
     * @throws IllegalArgumentException if [minLength] is greater than [maxLength] or if either is not in the range 1..9.
     */
    public fun appendSecondFraction(minLength: Int? = null, maxLength: Int? = null)

    /**
     * Appends an existing [DateTimeFormat] for the time part.
     *
     * Example:
     * ```
     * appendTime(LocalTime.Format.ISO)
     * ```
     */
    public fun appendTime(format: DateTimeFormat<LocalTime>)
}

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
    override var hour: Int? = null,
    override var hourOfAmPm: Int? = null,
    override var isPm: Boolean? = null,
    override var minute: Int? = null,
    override var second: Int? = null,
    override var nanosecond: Int? = null
) : TimeFieldContainer, Copyable<IncompleteLocalTime> {
    fun toLocalTime(): LocalTime {
        val hour: Int = hour?.let { hour ->
            hourOfAmPm?.let {
                require((hour + 11) % 12 + 1 == it) { "Inconsistent hour and hour-of-am-pm: hour is $hour, but hour-of-am-pm is $it" }
            }
            isPm?.let {
                require(isPm != null && isPm != (hour >= 12)) {
                    "Inconsistent hour and the AM/PM marker: hour is $hour, but the AM/PM marker is ${if (it) "PM" else "AM"}"
                }
            }
            hour
        } ?: hourOfAmPm?.let { hourOfAmPm ->
            isPm?.let { isPm ->
                hourOfAmPm.let { if (it == 12) 0 else it } + if (isPm) 12 else 0
            }
        } ?: throw DateTimeFormatException("Incomplete time: missing hour")
        return LocalTime(
            hour,
            getParsedField(minute, "minute"),
            second ?: 0,
            nanosecond ?: 0,
        )
    }

    fun populateFrom(localTime: LocalTime) {
        hour = localTime.hour
        hourOfAmPm = (localTime.hour + 11) % 12 + 1
        isPm = localTime.hour >= 12
        minute = localTime.minute
        second = localTime.second
        nanosecond = localTime.nanosecond
    }

    override fun copy(): IncompleteLocalTime = IncompleteLocalTime(hour, hourOfAmPm, isPm, minute, second, nanosecond)

    override fun equals(other: Any?): Boolean =
        other is IncompleteLocalTime && hour == other.hour && hourOfAmPm == other.hourOfAmPm && isPm == other.isPm &&
            minute == other.minute && second == other.second && nanosecond == other.nanosecond

    override fun hashCode(): Int =
        (hour ?: 0) * 31 + (hourOfAmPm ?: 0) * 31 + (isPm?.hashCode() ?: 0) * 31 + (minute ?: 0) * 31 +
            (second ?: 0) * 31 + (nanosecond ?: 0)

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
    AbstractDateTimeFormat<LocalTime, IncompleteLocalTime>(actualFormat) {
    override fun intermediateFromValue(value: LocalTime): IncompleteLocalTime =
        IncompleteLocalTime().apply { populateFrom(value) }

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

        @Suppress("NO_ELSE_IN_WHEN")
        override fun appendTime(format: DateTimeFormat<LocalTime>) = when (format) {
            is LocalTimeFormat -> actualBuilder.add(format.actualFormat.directives)
        }

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }

    override fun toString(): String = actualFormat.builderString()

}

// these are constants so that the formats are not recreated every time they are used
@SharedImmutable
internal val ISO_TIME by lazy {
    LocalTimeFormat.build {
        appendHour()
        char(':')
        appendMinute()
        appendOptional {
            char(':')
            appendSecond()
            appendOptional {
                char('.')
                appendSecondFraction()
            }
        }
    }
}
@SharedImmutable
internal val ISO_TIME_BASIC by lazy {
    LocalTimeFormat.build {
        alternativeParsing({ char('t') }) { char('T') }
        appendHour(); appendMinute()
        appendOptional {
            appendSecond()
            appendOptional {
                char('.')
                appendSecondFraction()
            }
        }
    }
}
