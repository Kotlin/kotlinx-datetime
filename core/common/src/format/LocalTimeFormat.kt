/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.LocalTime
import kotlinx.datetime.DateTimeFormatException
import kotlinx.datetime.internal.DecimalFraction
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.parser.Copyable

/**
 * The AM/PM marker that indicates whether the hour in range `1..12` is before or after noon.
 */
public enum class AmPmMarker {
    /** The time is before noon. */
    AM,

    /** The time is after noon. */
    PM,
}

internal interface TimeFieldContainer {
    var minute: Int?
    var second: Int?
    var nanosecond: Int?
    var hour: Int?
    var hourOfAmPm: Int?
    var amPm: AmPmMarker?

    var fractionOfSecond: DecimalFraction?
        get() = nanosecond?.let { DecimalFraction(it, 9) }
        set(value) {
            nanosecond = value?.fractionalPartWithNDigits(9)
        }
}

private object TimeFields {
    val hour = UnsignedFieldSpec(PropertyAccessor(TimeFieldContainer::hour), minValue = 0, maxValue = 23)
    val minute = UnsignedFieldSpec(PropertyAccessor(TimeFieldContainer::minute), minValue = 0, maxValue = 59)
    val second =
        UnsignedFieldSpec(PropertyAccessor(TimeFieldContainer::second), minValue = 0, maxValue = 59, defaultValue = 0)
    val fractionOfSecond =
        GenericFieldSpec(PropertyAccessor(TimeFieldContainer::fractionOfSecond, "nanosecond"), defaultValue = DecimalFraction(0, 9))
    val amPm = GenericFieldSpec(PropertyAccessor(TimeFieldContainer::amPm))
    val hourOfAmPm = UnsignedFieldSpec(PropertyAccessor(TimeFieldContainer::hourOfAmPm), minValue = 1, maxValue = 12)
}

internal class IncompleteLocalTime(
    override var hour: Int? = null,
    override var hourOfAmPm: Int? = null,
    override var amPm: AmPmMarker? = null,
    override var minute: Int? = null,
    override var second: Int? = null,
    override var nanosecond: Int? = null
) : TimeFieldContainer, Copyable<IncompleteLocalTime> {
    fun toLocalTime(): LocalTime {
        val hour: Int = hour?.let { hour ->
            hourOfAmPm?.let {
                require((hour + 11) % 12 + 1 == it) { "Inconsistent hour and hour-of-am-pm: hour is $hour, but hour-of-am-pm is $it" }
            }
            amPm?.let { amPm ->
                require((amPm == AmPmMarker.PM) == (hour >= 12)) {
                    "Inconsistent hour and the AM/PM marker: hour is $hour, but the AM/PM marker is $amPm"
                }
            }
            hour
        } ?: hourOfAmPm?.let { hourOfAmPm ->
            amPm?.let { amPm ->
                hourOfAmPm.let { if (it == 12) 0 else it } + if (amPm == AmPmMarker.PM) 12 else 0
            }
        } ?: throw DateTimeFormatException("Incomplete time: missing hour")
        return LocalTime(
            hour,
            requireParsedField(minute, "minute"),
            second ?: 0,
            nanosecond ?: 0,
        )
    }

    fun populateFrom(localTime: LocalTime) {
        hour = localTime.hour
        hourOfAmPm = (localTime.hour + 11) % 12 + 1
        amPm = if (localTime.hour >= 12) AmPmMarker.PM else AmPmMarker.AM
        minute = localTime.minute
        second = localTime.second
        nanosecond = localTime.nanosecond
    }

    override fun copy(): IncompleteLocalTime = IncompleteLocalTime(hour, hourOfAmPm, amPm, minute, second, nanosecond)

    override fun equals(other: Any?): Boolean =
        other is IncompleteLocalTime && hour == other.hour && hourOfAmPm == other.hourOfAmPm && amPm == other.amPm &&
            minute == other.minute && second == other.second && nanosecond == other.nanosecond

    override fun hashCode(): Int =
        (hour ?: 0) * 31 + (hourOfAmPm ?: 0) * 31 + (amPm?.hashCode() ?: 0) * 31 + (minute ?: 0) * 31 +
            (second ?: 0) * 31 + (nanosecond ?: 0)

    override fun toString(): String =
        "${hour ?: "??"}:${minute ?: "??"}:${second ?: "??"}.${
            nanosecond?.let { nanos ->
                nanos.toString().let { it.padStart(9 - it.length, '0') }
            } ?: "???"
        }"
}

internal interface AbstractWithTimeBuilder : DateTimeFormatBuilder.WithTime {
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
        is LocalTimeFormat -> addFormatStructureForTime(format.actualFormat)
    }
}

private class HourDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(
        TimeFields.hour,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {
    override val builderRepresentation: String
        get() = when (padding) {
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
    override val builderRepresentation: String
        get() = when (padding) {
            Padding.ZERO -> "${DateTimeFormatBuilder.WithTime::amPmHour.name}()"
            else -> "${DateTimeFormatBuilder.WithTime::amPmHour.name}(${padding.toKotlinCode()})"
        }

    override fun equals(other: Any?): Boolean = other is AmPmHourDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

private class AmPmMarkerDirective(private val amString: String, private val pmString: String) :
    NamedEnumIntFieldFormatDirective<TimeFieldContainer, AmPmMarker>(
        TimeFields.amPm, mapOf(
            AmPmMarker.AM to amString,
            AmPmMarker.PM to pmString,
        ),
        "AM/PM marker"
    ) {

    override val builderRepresentation: String
        get() =
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

    override val builderRepresentation: String
        get() = when (padding) {
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

    override val builderRepresentation: String
        get() = when (padding) {
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
    DecimalFractionFieldFormatDirective<TimeFieldContainer>(
        TimeFields.fractionOfSecond,
        minDigits,
        maxDigits,
        zerosToAdd
    ) {

    override val builderRepresentation: String
        get() {
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

internal class LocalTimeFormat(override val actualFormat: CachedFormatStructure<TimeFieldContainer>) :
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
                secondFraction(1, 9)
            }
        }
    }
}

private val emptyIncompleteLocalTime = IncompleteLocalTime()
