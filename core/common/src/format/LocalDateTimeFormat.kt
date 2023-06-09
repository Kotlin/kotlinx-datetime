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

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }

    override fun toString(): String = actualFormat.builderString()
}
