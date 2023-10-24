/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*
import kotlin.native.concurrent.*

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

internal class HourDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(
        TimeFields.hour,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {
    override val builderRepresentation: String get() = when (padding) {
        Padding.ZERO -> "${DateTimeFormatBuilder.WithTime::appendHour.name}()"
        else -> "${DateTimeFormatBuilder.WithTime::appendHour.name}(${padding.toKotlinCode()})"
    }

    override fun equals(other: Any?): Boolean = other is HourDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

internal class AmPmHourDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(
        TimeFields.hourOfAmPm, minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {
    override val builderRepresentation: String get() = when (padding) {
        Padding.ZERO -> "${DateTimeFormatBuilder.WithTime::appendAmPmHour.name}()"
        else -> "${DateTimeFormatBuilder.WithTime::appendAmPmHour.name}(${padding.toKotlinCode()})"
    }

    override fun equals(other: Any?): Boolean = other is AmPmHourDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

internal class AmPmMarkerDirective(private val amString: String, private val pmString: String) :
    NamedEnumIntFieldFormatDirective<TimeFieldContainer, Boolean>(
        TimeFields.isPm, mapOf(
            false to amString,
            true to pmString,
        )
    ) {

    override val builderRepresentation: String get() =
        "${DateTimeFormatBuilder.WithTime::appendAmPmMarker.name}($amString, $pmString)"

    override fun equals(other: Any?): Boolean =
        other is AmPmMarkerDirective && amString == other.amString && pmString == other.pmString
    override fun hashCode(): Int = 31 * amString.hashCode() + pmString.hashCode()
}

internal class MinuteDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(
        TimeFields.minute,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {

    override val builderRepresentation: String get() = when (padding) {
        Padding.ZERO -> "${DateTimeFormatBuilder.WithTime::appendMinute.name}()"
        else -> "${DateTimeFormatBuilder.WithTime::appendMinute.name}(${padding.toKotlinCode()})"
    }

    override fun equals(other: Any?): Boolean = other is MinuteDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

internal class SecondDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(
        TimeFields.second,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {

    override val builderRepresentation: String get() = when (padding) {
        Padding.ZERO -> "${DateTimeFormatBuilder.WithTime::appendSecond.name}()"
        else -> "${DateTimeFormatBuilder.WithTime::appendSecond.name}(${padding.toKotlinCode()})"
    }

    override fun equals(other: Any?): Boolean = other is SecondDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

internal class FractionalSecondDirective(private val minDigits: Int? = null, private val maxDigits: Int? = null) :
    DecimalFractionFieldFormatDirective<TimeFieldContainer>(TimeFields.fractionOfSecond, minDigits, maxDigits) {

    override val builderRepresentation: String get() = when {
        minDigits == 1 && maxDigits == null -> "${DateTimeFormatBuilder.WithTime::appendSecondFraction.name}()"
        minDigits == 1 -> "${DateTimeFormatBuilder.WithTime::appendSecondFraction.name}(maxLength = $maxDigits)"
        maxDigits == null -> "${DateTimeFormatBuilder.WithTime::appendSecondFraction.name}($minDigits)"
        else -> "${DateTimeFormatBuilder.WithTime::appendSecondFraction.name}($minDigits, $maxDigits)"
    }

    override fun equals(other: Any?): Boolean =
        other is FractionalSecondDirective && minDigits == other.minDigits && maxDigits == other.maxDigits

    override fun hashCode(): Int = 31 * (minDigits ?: 0) + (maxDigits ?: 0)
}

internal class LocalTimeFormat(override val actualFormat: StringFormat<TimeFieldContainer>) :
    AbstractDateTimeFormat<LocalTime, IncompleteLocalTime>() {
    override fun intermediateFromValue(value: LocalTime): IncompleteLocalTime =
        IncompleteLocalTime().apply { populateFrom(value) }

    override fun valueFromIntermediate(intermediate: IncompleteLocalTime): LocalTime = intermediate.toLocalTime()

    override fun newIntermediate(): IncompleteLocalTime = IncompleteLocalTime()

    companion object {
        fun build(block: DateTimeFormatBuilder.WithTime.() -> Unit): LocalTimeFormat {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return LocalTimeFormat(builder.build())
        }

    }

    internal class Builder(override val actualBuilder: AppendableFormatStructure<TimeFieldContainer>) :
        AbstractDateTimeFormatBuilder<TimeFieldContainer, Builder>, DateTimeFormatBuilder.WithTime {
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

}

// these are constants so that the formats are not recreated every time they are used
@SharedImmutable
internal val ISO_TIME by lazy {
    LocalTimeFormat.build {
        appendHour()
        char(':')
        appendMinute()
        optional {
            char(':')
            appendSecond()
            optional {
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
        optional {
            appendSecond()
            optional {
                char('.')
                appendSecondFraction()
            }
        }
    }
}
