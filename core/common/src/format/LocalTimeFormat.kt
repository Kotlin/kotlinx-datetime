/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.parser.Copyable
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

private object TimeFields {
    val hour = UnsignedFieldSpec(PropertyAccessor(TimeFieldContainer::hour), minValue = 0, maxValue = 23)
    val minute = UnsignedFieldSpec(PropertyAccessor(TimeFieldContainer::minute), minValue = 0, maxValue = 59)
    val second = UnsignedFieldSpec(PropertyAccessor(TimeFieldContainer::second), minValue = 0, maxValue = 59, defaultValue = 0)
    val fractionOfSecond = GenericFieldSpec(PropertyAccessor(TimeFieldContainer::fractionOfSecond), defaultValue = DecimalFraction(0, 9))
    val isPm = GenericFieldSpec(PropertyAccessor(TimeFieldContainer::isPm))
    val hourOfAmPm = UnsignedFieldSpec(PropertyAccessor(TimeFieldContainer::hourOfAmPm), minValue = 1, maxValue = 12)
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

internal interface AbstractWithTimeBuilder: DateTimeFormatBuilder.WithTime {
    fun addFormatStructureForTime(structure: FormatStructure<TimeFieldContainer>)

    override fun hour(padding: Padding) = addFormatStructureForTime(BasicFormatStructure(HourDirective(padding)))
    override fun amPmHour(padding: Padding) =
        addFormatStructureForTime(BasicFormatStructure(AmPmHourDirective(padding)))

    override fun amPmMarker(am: String, pm: String) =
        addFormatStructureForTime(BasicFormatStructure(AmPmMarkerDirective(am, pm)))

    override fun minute(padding: Padding) = addFormatStructureForTime(BasicFormatStructure(MinuteDirective(padding)))
    override fun second(padding: Padding) = addFormatStructureForTime(BasicFormatStructure(SecondDirective(padding)))
    override fun secondFraction(minLength: Int, maxLength: Int) =
        addFormatStructureForTime(BasicFormatStructure(FractionalSecondDirective(minLength, maxLength)))

    @Suppress("NO_ELSE_IN_WHEN")
    override fun time(format: DateTimeFormat<LocalTime>) = when (format) {
        is LocalTimeFormat -> addFormatStructureForTime(format.actualFormat.directives)
    }
}

private class HourDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(
        TimeFields.hour,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {
    override val builderRepresentation: String get() = when (padding) {
        Padding.ZERO -> "${DateTimeFormatBuilder.WithTime::hour.name}()"
        else -> "${DateTimeFormatBuilder.WithTime::hour.name}(${padding.toKotlinCode()})"
    }

    override fun equals(other: Any?): Boolean = other is HourDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

private class AmPmHourDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(
        TimeFields.hourOfAmPm, minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {
    override val builderRepresentation: String get() = when (padding) {
        Padding.ZERO -> "${DateTimeFormatBuilder.WithTime::amPmHour.name}()"
        else -> "${DateTimeFormatBuilder.WithTime::amPmHour.name}(${padding.toKotlinCode()})"
    }

    override fun equals(other: Any?): Boolean = other is AmPmHourDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

private class AmPmMarkerDirective(private val amString: String, private val pmString: String) :
    NamedEnumIntFieldFormatDirective<TimeFieldContainer, Boolean>(
        TimeFields.isPm, mapOf(
            false to amString,
            true to pmString,
        ),
        "AM/PM marker"
    ) {

    override val builderRepresentation: String get() =
        "${DateTimeFormatBuilder.WithTime::amPmMarker.name}($amString, $pmString)"

    override fun equals(other: Any?): Boolean =
        other is AmPmMarkerDirective && amString == other.amString && pmString == other.pmString
    override fun hashCode(): Int = 31 * amString.hashCode() + pmString.hashCode()
}

private class MinuteDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(
        TimeFields.minute,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {

    override val builderRepresentation: String get() = when (padding) {
        Padding.ZERO -> "${DateTimeFormatBuilder.WithTime::minute.name}()"
        else -> "${DateTimeFormatBuilder.WithTime::minute.name}(${padding.toKotlinCode()})"
    }

    override fun equals(other: Any?): Boolean = other is MinuteDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

private class SecondDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(
        TimeFields.second,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {

    override val builderRepresentation: String get() = when (padding) {
        Padding.ZERO -> "${DateTimeFormatBuilder.WithTime::second.name}()"
        else -> "${DateTimeFormatBuilder.WithTime::second.name}(${padding.toKotlinCode()})"
    }

    override fun equals(other: Any?): Boolean = other is SecondDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

internal class FractionalSecondDirective(
    private val minDigits: Int,
    private val maxDigits: Int,
    zerosToAdd: List<Int> = NO_EXTRA_ZEROS,
) :
    DecimalFractionFieldFormatDirective<TimeFieldContainer>(TimeFields.fractionOfSecond, minDigits, maxDigits, zerosToAdd) {

    override val builderRepresentation: String get() {
        val ref = "secondFraction" // can't directly reference `secondFraction` due to resolution ambiguity
        // we ignore `grouping`, as it's not representable in the end users' code
        return when {
            minDigits == 1 && maxDigits == 9 -> "$ref()"
            minDigits == 1 -> "$ref(maxLength = $maxDigits)"
            maxDigits == 1 -> "$ref(minLength = $minDigits)"
            maxDigits == minDigits -> "$ref($minDigits)"
            else -> "$ref($minDigits, $maxDigits)"
        }
    }

    override fun equals(other: Any?): Boolean =
        other is FractionalSecondDirective && minDigits == other.minDigits && maxDigits == other.maxDigits

    override fun hashCode(): Int = 31 * minDigits + maxDigits

    companion object {
        val NO_EXTRA_ZEROS = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0)
        val GROUP_BY_THREE = listOf(2, 1, 0, 2, 1, 0, 2, 1, 0)
    }
}

internal class LocalTimeFormat(override val actualFormat: StringFormat<TimeFieldContainer>) :
    AbstractDateTimeFormat<LocalTime, IncompleteLocalTime>() {
    override fun intermediateFromValue(value: LocalTime): IncompleteLocalTime =
        IncompleteLocalTime().apply { populateFrom(value) }

    override fun valueFromIntermediate(intermediate: IncompleteLocalTime): LocalTime = intermediate.toLocalTime()

    override val emptyIntermediate: IncompleteLocalTime get() = emptyIncompleteLocalTime

    companion object {
        fun build(block: DateTimeFormatBuilder.WithTime.() -> Unit): LocalTimeFormat {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return LocalTimeFormat(builder.build())
        }

    }

    private class Builder(override val actualBuilder: AppendableFormatStructure<TimeFieldContainer>) :
        AbstractDateTimeFormatBuilder<TimeFieldContainer, Builder>, AbstractWithTimeBuilder {

        override fun addFormatStructureForTime(structure: FormatStructure<TimeFieldContainer>) {
            actualBuilder.add(structure)
        }

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }

}

// these are constants so that the formats are not recreated every time they are used
@SharedImmutable
internal val ISO_TIME by lazy {
    LocalTimeFormat.build {
        hour()
        char(':')
        minute()
        alternativeParsing({
            // intentionally empty
        }) {
            char(':')
            second()
            optional {
                char('.')
                secondFractionInternal(1, 9, FractionalSecondDirective.GROUP_BY_THREE)
            }
        }
    }
}

@SharedImmutable
internal val ISO_TIME_OPTIONAL_SECONDS by lazy {
    LocalTimeFormat.build {
        hour()
        char(':')
        minute()
        optional {
            char(':')
            second()
            optional {
                char('.')
                secondFractionInternal(1, 9, FractionalSecondDirective.GROUP_BY_THREE)
            }
        }
    }
}

@SharedImmutable
private val emptyIncompleteLocalTime = IncompleteLocalTime()
