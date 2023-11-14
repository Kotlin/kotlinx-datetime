/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.format.*
import kotlin.native.concurrent.*

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

internal class LocalDateTimeFormat(override val actualFormat: StringFormat<DateTimeFieldContainer>) :
    AbstractDateTimeFormat<LocalDateTime, IncompleteLocalDateTime>() {
    override fun intermediateFromValue(value: LocalDateTime): IncompleteLocalDateTime =
        IncompleteLocalDateTime().apply { populateFrom(value) }

    override fun valueFromIntermediate(intermediate: IncompleteLocalDateTime): LocalDateTime =
        intermediate.toLocalDateTime()

    override fun newIntermediate(): IncompleteLocalDateTime = IncompleteLocalDateTime()

    companion object {
        fun build(block: DateTimeFormatBuilder.WithDateTime.() -> Unit): LocalDateTimeFormat {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return LocalDateTimeFormat(builder.build())
        }
    }

    private class Builder(override val actualBuilder: AppendableFormatStructure<DateTimeFieldContainer>) :
        AbstractDateTimeFormatBuilder<DateTimeFieldContainer, Builder>, DateTimeFormatBuilder.WithDateTime {

        override fun year(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(YearDirective(padding)))

        override fun yearTwoDigits(baseYear: Int) =
            actualBuilder.add(BasicFormatStructure(ReducedYearDirective(baseYear)))

        override fun monthNumber(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(MonthDirective(padding)))

        override fun monthName(names: MonthNames) =
            actualBuilder.add(BasicFormatStructure(MonthNameDirective(names)))

        override fun dayOfMonth(padding: Padding) = actualBuilder.add(BasicFormatStructure(DayDirective(padding)))

        override fun dayOfWeek(names: DayOfWeekNames) =
            actualBuilder.add(BasicFormatStructure(DayOfWeekDirective(names)))

        override fun hour(padding: Padding) = actualBuilder.add(BasicFormatStructure(HourDirective(padding)))
        override fun amPmHour(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(AmPmHourDirective(padding)))

        override fun amPmMarker(am: String, pm: String) =
            actualBuilder.add(BasicFormatStructure(AmPmMarkerDirective(am, pm)))

        override fun minute(padding: Padding) = actualBuilder.add(BasicFormatStructure(MinuteDirective(padding)))
        override fun second(padding: Padding) = actualBuilder.add(BasicFormatStructure(SecondDirective(padding)))
        override fun secondFraction(minLength: Int?, maxLength: Int?) =
            actualBuilder.add(BasicFormatStructure(FractionalSecondDirective(minLength, maxLength)))

        @Suppress("NO_ELSE_IN_WHEN")
        override fun date(format: DateTimeFormat<LocalDate>) = when (format) {
            is LocalDateFormat -> actualBuilder.add(format.actualFormat.directives)
        }

        @Suppress("NO_ELSE_IN_WHEN")
        override fun time(format: DateTimeFormat<LocalTime>) = when (format) {
            is LocalTimeFormat -> actualBuilder.add(format.actualFormat.directives)
        }

        @Suppress("NO_ELSE_IN_WHEN")
        override fun dateTime(format: DateTimeFormat<LocalDateTime>) = when (format) {
            is LocalDateTimeFormat -> actualBuilder.add(format.actualFormat.directives)
        }

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }
}

// these are constants so that the formats are not recreated every time they are used
@SharedImmutable
internal val ISO_DATETIME by lazy {
    LocalDateTimeFormat.build {
        date(ISO_DATE)
        alternativeParsing({ char('t') }) { char('T') }
        time(ISO_TIME)
    }
}

@SharedImmutable
internal val ISO_DATETIME_OPTIONAL_SECONDS by lazy {
    LocalDateTimeFormat.build {
        date(ISO_DATE)
        alternativeParsing({ char('t') }) { char('T') }
        time(ISO_TIME_OPTIONAL_SECONDS)
    }
}
