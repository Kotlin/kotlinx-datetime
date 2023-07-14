/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.format.*

public sealed interface DateTimeFormatBuilder : DateFormatBuilder, TimeFormatBuilderFields

internal fun DateTimeFormatBuilder.appendIsoDateTime() {
    appendIsoDate()
    appendAlternatives({
        appendLiteral('T')
    }, {
        appendLiteral('t')
    })
    appendIsoTime()
}

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

internal class LocalDateTimeFormat(private val actualFormat: StringFormat<DateTimeFieldContainer>) :
    AbstractFormat<LocalDateTime, DateTimeFieldContainer>(actualFormat) {
    override fun intermediateFromValue(value: LocalDateTime): DateTimeFieldContainer =
        value.toIncompleteLocalDateTime()

    override fun valueFromIntermediate(intermediate: DateTimeFieldContainer): LocalDateTime =
        intermediate.toLocalDateTime()

    override fun newIntermediate(): DateTimeFieldContainer = IncompleteLocalDateTime()

    companion object {
        fun build(block: DateTimeFormatBuilder.() -> Unit): LocalDateTimeFormat {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return LocalDateTimeFormat(builder.build())
        }

        val ISO: LocalDateTimeFormat = build {
            appendIsoDateTime()
        }
    }

    private class Builder(override val actualBuilder: AppendableFormatStructure<DateTimeFieldContainer>) :
        AbstractFormatBuilder<DateTimeFieldContainer, Builder>, DateTimeFormatBuilder {
        override fun appendYear(padding: Padding, outputPlusOnExceededPadding: Boolean) =
            actualBuilder.add(BasicFormatStructure(YearDirective(padding, outputPlusOnExceededPadding)))

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
        override fun appendSecondFraction(minLength: Int, maxLength: Int?) =
            actualBuilder.add(BasicFormatStructure(FractionalSecondDirective(minLength, maxLength)))

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }

    override fun toString(): String = actualFormat.builderString()
}
