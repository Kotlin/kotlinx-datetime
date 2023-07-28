/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.format.*
import kotlin.native.concurrent.*

/**
 * Functions specific to the date-time format builders containing the local-date and local-time fields.
 */
public sealed interface DateTimeFormatBuilder : DateFormatBuilder, TimeFormatBuilderFields {
    /**
     * Appends an existing [Format] for the date-time part.
     *
     * Example:
     * ```
     * appendDateTime(LocalDateTime.Format.ISO)
     * ```
     */
    public fun appendDateTime(format: Format<LocalDateTime>)
}

internal interface DateTimeFieldContainer : DateFieldContainer, TimeFieldContainer

internal class IncompleteLocalDateTime(
    val date: IncompleteLocalDate = IncompleteLocalDate(),
    val time: IncompleteLocalTime = IncompleteLocalTime(),
) : DateTimeFieldContainer, DateFieldContainer by date, TimeFieldContainer by time, Copyable<IncompleteLocalDateTime> {
    fun toLocalDateTime(): LocalDateTime = LocalDateTime(date.toLocalDate(), time.toLocalTime())

    fun populateFrom(dateTime: LocalDateTime) {
        date.populateFrom(dateTime.date)
        time.populateFrom(dateTime.time)
    }

    override fun copy(): IncompleteLocalDateTime = IncompleteLocalDateTime(date.copy(), time.copy())
}

internal class LocalDateTimeFormat(val actualFormat: StringFormat<DateTimeFieldContainer>) :
    AbstractFormat<LocalDateTime, IncompleteLocalDateTime>(actualFormat) {
    override fun intermediateFromValue(value: LocalDateTime): IncompleteLocalDateTime =
        IncompleteLocalDateTime().apply { populateFrom(value) }

    override fun valueFromIntermediate(intermediate: IncompleteLocalDateTime): LocalDateTime =
        intermediate.toLocalDateTime()

    override fun newIntermediate(): IncompleteLocalDateTime = IncompleteLocalDateTime()

    companion object {
        fun build(block: DateTimeFormatBuilder.() -> Unit): LocalDateTimeFormat {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return LocalDateTimeFormat(builder.build())
        }
    }

    private class Builder(override val actualBuilder: AppendableFormatStructure<DateTimeFieldContainer>) :
        AbstractFormatBuilder<DateTimeFieldContainer, Builder>, DateTimeFormatBuilder {

        override fun appendYear(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(YearDirective(padding)))

        override fun appendYearTwoDigits(base: Int) =
            actualBuilder.add(BasicFormatStructure(ReducedYearDirective(base)))

        override fun appendMonthNumber(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(MonthDirective(padding)))

        override fun appendMonthName(names: MonthNames) =
            actualBuilder.add(BasicFormatStructure(MonthNameDirective(names.names)))

        override fun appendDayOfMonth(padding: Padding) = actualBuilder.add(BasicFormatStructure(DayDirective(padding)))

        override fun appendDayOfWeek(names: DayOfWeekNames) =
            actualBuilder.add(BasicFormatStructure(DayOfWeekDirective(names.names)))

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
        override fun appendDate(dateFormat: Format<LocalDate>) = when (dateFormat) {
            is LocalDateFormat -> actualBuilder.add(dateFormat.actualFormat.directives)
        }

        @Suppress("NO_ELSE_IN_WHEN")
        override fun appendTime(format: Format<LocalTime>) = when (format) {
            is LocalTimeFormat -> actualBuilder.add(format.actualFormat.directives)
        }

        @Suppress("NO_ELSE_IN_WHEN")
        override fun appendDateTime(format: Format<LocalDateTime>) = when (format) {
            is LocalDateTimeFormat -> actualBuilder.add(format.actualFormat.directives)
        }

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }

    override fun toString(): String = actualFormat.builderString()
}

// these are constants so that the formats are not recreated every time they are used
@SharedImmutable
internal val ISO_DATETIME by lazy {
    LocalDateTimeFormat.build {
        appendDate(ISO_DATE)
        alternativeParsing({ appendLiteral('t') }) { appendLiteral('T') }
        appendTime(ISO_TIME)
    }
}

@SharedImmutable
internal val ISO_DATETIME_BASIC by lazy {
    LocalDateTimeFormat.build {
        appendDate(ISO_DATE_BASIC)
        appendTime(ISO_TIME_BASIC)
    }
}
