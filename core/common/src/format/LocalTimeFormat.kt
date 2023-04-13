/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.parser.*


public interface TimeFormatBuilderFields {
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
    public fun appendSecondFraction(minLength: Int? = null, maxLength: Int? = null)
}

@DateTimeBuilder
public interface TimeFormatBuilder : TimeFormatBuilderFields, FormatBuilder<TimeFormatBuilder> {
    /**
     * Appends a format string to the builder.
     *
     * For rules common for all format strings, see [FormatBuilder.appendFormatString].
     *
     * For time format strings, the following pattern letters are available:
     * * `h` - hour of day, 0-23.
     * * `m` - minute of hour, 0-59.
     * * `s` - second of minute, 0-59. Has the default value of 0.
     * * `f` - decimal fraction of the second, between 0 (inclusive) and 1 (non-inclusive), with at most 9 digits
     *   of precision. Has the default value of 0.
     *
     * The directives can be repeated to specify the minimum length of the field.
     *
     * Example: `hh:mm:ss` will format `LocalTime(1, 2, 3)` as `01:02:03`.
     */
    // overriding the documentation.
    override fun appendFormatString(formatString: String)
}

public class LocalTimeFormat private constructor(private val actualFormat: Format<TimeFieldContainer>) {
    public companion object {
        public fun build(block: TimeFormatBuilder.() -> Unit): LocalTimeFormat {
            val builder = Builder(AppendableFormatStructure(TimeFormatBuilderSpec))
            builder.block()
            return LocalTimeFormat(builder.build())
        }

        public fun fromFormatString(formatString: String): LocalTimeFormat = build { appendFormatString(formatString) }

        /**
         * ISO-8601 extended format, used by [LocalTime.toString] and [LocalTime.parse].
         *
         * Examples: `12:34`, `12:34:56`, `12:34:56.789`.
         */
        public val ISO: LocalTimeFormat = fromFormatString("hh':'mm(|':'ss(|'.'f))")

        internal val Cache = LruCache<String, LocalTimeFormat>(16) { fromFormatString(it) }
    }

    public fun format(time: LocalTime): String =
        StringBuilder().also {
            actualFormat.formatter.format(time.toIncompleteLocalTime(), it)
        }.toString()

    public fun parse(input: String): LocalTime {
        val parser = Parser(::IncompleteLocalTime, IncompleteLocalTime::copy, actualFormat.parser)
        try {
            return parser.match(input).toLocalTime()
        } catch (e: ParseException) {
            throw DateTimeFormatException("Failed to parse time from '$input'", e)
        } catch (e: IllegalArgumentException) {
            throw DateTimeFormatException("Invalid time '$input'", e)
        }
    }

    private class Builder(override val actualBuilder: AppendableFormatStructure<TimeFieldContainer>) :
        AbstractFormatBuilder<TimeFieldContainer, TimeFormatBuilder, Builder>, TimeFormatBuilder {
        override fun appendHour(minLength: Int) = actualBuilder.add(BasicFormatStructure(HourDirective(minLength)))
        override fun appendAmPmHour(minLength: Int) =
            actualBuilder.add(BasicFormatStructure(AmPmHourDirective(minLength)))

        override fun appendAmPmMarker(amString: String, pmString: String) =
            actualBuilder.add(BasicFormatStructure(AmPmMarkerDirective(amString, pmString)))

        override fun appendMinute(minLength: Int) = actualBuilder.add(BasicFormatStructure(MinuteDirective(minLength)))
        override fun appendSecond(minLength: Int) = actualBuilder.add(BasicFormatStructure(SecondDirective(minLength)))
        override fun appendSecondFraction(minLength: Int?, maxLength: Int?) =
            actualBuilder.add(BasicFormatStructure(FractionalSecondDirective(minLength, maxLength)))

        override fun createEmpty(): Builder = Builder(actualBuilder.createSibling())
        override fun castToGeneric(actualSelf: Builder): TimeFormatBuilder = this
        override fun appendFormatString(formatString: String) = super.appendFormatString(formatString)
    }

}

public fun LocalTime.format(formatString: String): String =
    LocalTimeFormat.Cache.get(formatString).format(this)

public fun LocalTime.format(format: LocalTimeFormat): String = format.format(this)

public fun LocalTime.Companion.parse(input: String, formatString: String): LocalTime =
    LocalTimeFormat.Cache.get(formatString).parse(input)

public fun LocalTime.Companion.parse(input: String, format: LocalTimeFormat): LocalTime = format.parse(input)

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
                hourField = value
                isPm = value >= 12
            } else {
                hourField = null
                isPm = null
            }
        }

    fun toLocalTime(): LocalTime = LocalTime(
        getParsedField(hour, "hour"),
        getParsedField(minute, "minute"),
        second ?: 0,
        nanosecond ?: 0,
    )

    override fun copy(): IncompleteLocalTime = IncompleteLocalTime(hour, isPm, minute, second, nanosecond)

    override fun toString(): String =
        "${hour ?: "??"}:${minute ?: "??"}:${second ?: "??"}.${
            nanosecond?.let {
                it.toString().let { it.padStart(9 - it.length, '0') }
            } ?: "???"
        }"
}

internal class HourDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(TimeFields.hour, minDigits)

internal class AmPmHourDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(TimeFields.hourOfAmPm, minDigits)

internal class AmPmMarkerDirective(amString: String, pmString: String) :
    NamedEnumIntFieldFormatDirective<TimeFieldContainer, Boolean>(
        TimeFields.isPm, mapOf(
            false to amString,
            true to pmString,
        )
    )

internal class MinuteDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(TimeFields.minute, minDigits)

internal class SecondDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<TimeFieldContainer>(TimeFields.second, minDigits)

internal class FractionalSecondDirective(minDigits: Int? = null, maxDigits: Int? = null) :
    DecimalFractionFieldFormatDirective<TimeFieldContainer>(TimeFields.fractionOfSecond, minDigits, maxDigits)

internal object TimeFormatBuilderSpec : BuilderSpec<TimeFieldContainer>(
    mapOf(
        "lt" to TimeFormatBuilderSpec
    ),
    mapOf(
        'h' to { length -> BasicFormatStructure(HourDirective(length)) },
        'm' to { length -> BasicFormatStructure(MinuteDirective(length)) },
        's' to { length -> BasicFormatStructure(SecondDirective(length)) },
        'f' to { length -> BasicFormatStructure(FractionalSecondDirective(length, null)) },
    )
) {
    const val name = "lt"
}
