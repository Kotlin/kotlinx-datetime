/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.LruCache
import kotlinx.datetime.internal.format.*

public interface DateTimeFormatBuilder : DateFormatBuilderFields, TimeFormatBuilderFields,
    FormatBuilder<DateTimeFormatBuilder>

public class LocalDateTimeFormat private constructor(private val actualFormat: StringFormat<DateTimeFieldContainer>) :
    Format<LocalDateTime> by LocalDateTimeFormatImpl(actualFormat) {
    public companion object {
        public fun build(block: DateTimeFormatBuilder.() -> Unit): LocalDateTimeFormat {
            val builder = Builder(AppendableFormatStructure(DateTimeFormatBuilderSpec))
            builder.block()
            return LocalDateTimeFormat(builder.build())
        }

        public fun fromFormatString(formatString: String): LocalDateTimeFormat =
            build { appendFormatString(formatString) }

        /**
         * ISO-8601 extended format, which is the format used by [LocalDateTime.toString] and [LocalDateTime.parse].
         *
         * Examples of date/time in ISO-8601 format:
         * - `2020-08-30T18:43`
         * - `2020-08-30T18:43:00`
         * - `2020-08-30T18:43:00.500`
         * - `2020-08-30T18:43:00.123456789`
         */
        public val ISO: LocalDateTimeFormat = build {
            appendYear(4, outputPlusOnExceededPadding = true)
            appendFormatString("ld<-mm-dd>('T'|'t')lt<hh:mm(|:ss(|.f))>")
        }

        internal val Cache = LruCache<String, LocalDateTimeFormat>(16) { fromFormatString(it) }
    }

    private class Builder(override val actualBuilder: AppendableFormatStructure<DateTimeFieldContainer>) :
        AbstractFormatBuilder<DateTimeFieldContainer, DateTimeFormatBuilder, Builder>, DateTimeFormatBuilder {
        override fun appendYear(minDigits: Int, outputPlusOnExceededPadding: Boolean) =
            actualBuilder.add(BasicFormatStructure(YearDirective(minDigits, outputPlusOnExceededPadding)))

        override fun appendMonthNumber(minLength: Int) =
            actualBuilder.add(BasicFormatStructure(MonthDirective(minLength)))

        override fun appendMonthName(names: List<String>) =
            actualBuilder.add(BasicFormatStructure(MonthNameDirective(names)))

        override fun appendDayOfMonth(minLength: Int) = actualBuilder.add(BasicFormatStructure(DayDirective(minLength)))
        override fun appendDayOfWeek(names: List<String>) =
            actualBuilder.add(BasicFormatStructure(DayOfWeekDirective(names)))

        override fun appendHour(minLength: Int) = actualBuilder.add(BasicFormatStructure(HourDirective(minLength)))
        override fun appendAmPmHour(minLength: Int) =
            actualBuilder.add(BasicFormatStructure(AmPmHourDirective(minLength)))

        override fun appendAmPmMarker(amString: String, pmString: String) =
            actualBuilder.add(BasicFormatStructure(AmPmMarkerDirective(amString, pmString)))

        override fun appendMinute(minLength: Int) = actualBuilder.add(BasicFormatStructure(MinuteDirective(minLength)))
        override fun appendSecond(minLength: Int) = actualBuilder.add(BasicFormatStructure(SecondDirective(minLength)))
        override fun appendSecondFraction(minLength: Int, maxLength: Int?) =
            actualBuilder.add(BasicFormatStructure(FractionalSecondDirective(minLength, maxLength)))

        override fun createEmpty(): Builder = Builder(actualBuilder.createSibling())
        override fun castToGeneric(actualSelf: Builder): DateTimeFormatBuilder = this
    }

    override fun toString(): String = actualFormat.builderString()

}

public fun LocalDateTime.format(formatString: String): String =
    LocalDateTimeFormat.Cache.get(formatString).format(this)

public fun LocalDateTime.format(format: LocalDateTimeFormat): String = format.format(this)

public fun LocalDateTime.Companion.parse(input: String, formatString: String): LocalDateTime =
    LocalDateTimeFormat.Cache.get(formatString).parse(input)

public fun LocalDateTime.Companion.parse(input: String, format: LocalDateTimeFormat): LocalDateTime =
    format.parse(input)

internal fun LocalDateTime.toIncompleteLocalDateTime(): IncompleteLocalDateTime =
    IncompleteLocalDateTime(date.toIncompleteLocalDate(), time.toIncompleteLocalTime())

internal interface DateTimeFieldContainer : DateFieldContainer, TimeFieldContainer, Copyable<DateTimeFieldContainer> {
    fun toLocalDateTime(): LocalDateTime
}

internal class IncompleteLocalDateTime(
    val date: IncompleteLocalDate = IncompleteLocalDate(),
    val time: IncompleteLocalTime = IncompleteLocalTime(),
) : DateTimeFieldContainer, DateFieldContainer by date, TimeFieldContainer by time, Copyable<DateTimeFieldContainer> {
    override fun toLocalDateTime(): LocalDateTime = LocalDateTime(date.toLocalDate(), time.toLocalTime())

    override fun copy(): IncompleteLocalDateTime = IncompleteLocalDateTime(date.copy(), time.copy())
}

internal object DateTimeFormatBuilderSpec : BuilderSpec<DateTimeFieldContainer>(
    mapOf(
        DateFormatBuilderSpec.name to DateFormatBuilderSpec,
        TimeFormatBuilderSpec.name to TimeFormatBuilderSpec,
    ),
    emptyMap()
) {
    const val name = "ld"
}

private class LocalDateTimeFormatImpl(actualFormat: StringFormat<DateTimeFieldContainer>) :
    AbstractFormat<LocalDateTime, DateTimeFieldContainer>(actualFormat) {
    override fun intermediateFromValue(value: LocalDateTime): DateTimeFieldContainer =
        value.toIncompleteLocalDateTime()

    override fun valueFromIntermediate(intermediate: DateTimeFieldContainer): LocalDateTime =
        intermediate.toLocalDateTime()

    override fun newIntermediate(): DateTimeFieldContainer = IncompleteLocalDateTime()
}
