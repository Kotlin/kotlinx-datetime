/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.format.*
import kotlin.native.concurrent.*

public sealed interface DateTimeFormatBuilder : DateFormatBuilder, TimeFormatBuilderFields

internal fun DateTimeFormatBuilder.appendIsoDateTime() {
    appendIsoDate()
    alternativeParsing({
        appendLiteral('t')
    }) {
        appendLiteral('T')
    }
    appendIsoTime()
}

internal fun LocalDateTime.toIncompleteLocalDateTime(): IncompleteLocalDateTime =
    IncompleteLocalDateTime(date.toIncompleteLocalDate(), time.toIncompleteLocalTime())

internal interface DateTimeFieldContainer : DateFieldContainer, TimeFieldContainer

internal class IncompleteLocalDateTime(
    val date: IncompleteLocalDate = IncompleteLocalDate(),
    val time: IncompleteLocalTime = IncompleteLocalTime(),
) : DateTimeFieldContainer, DateFieldContainer by date, TimeFieldContainer by time, Copyable<IncompleteLocalDateTime> {
    fun toLocalDateTime(): LocalDateTime = LocalDateTime(date.toLocalDate(), time.toLocalTime())

    override fun copy(): IncompleteLocalDateTime = IncompleteLocalDateTime(date.copy(), time.copy())
}

internal class LocalDateTimeFormat(val actualFormat: StringFormat<DateTimeFieldContainer>) :
    AbstractFormat<LocalDateTime, IncompleteLocalDateTime>(actualFormat) {
    override fun intermediateFromValue(value: LocalDateTime): IncompleteLocalDateTime =
        value.toIncompleteLocalDateTime()

    override fun valueFromIntermediate(intermediate: IncompleteLocalDateTime): LocalDateTime =
        intermediate.toLocalDateTime()

    override fun newIntermediate(): IncompleteLocalDateTime = IncompleteLocalDateTime()

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
        override fun appendSecondFraction(minLength: Int, maxLength: Int?) =
            actualBuilder.add(BasicFormatStructure(FractionalSecondDirective(minLength, maxLength)))

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }

    override fun toString(): String = actualFormat.builderString()
}

// these are constants so that the formats are not recreated every time they are used
@SharedImmutable
internal val ISO_DATETIME by lazy {
    LocalDateTimeFormat.build {
        appendIsoDate()
        alternativeParsing({ appendLiteral('t') }) { appendLiteral('T') }
        appendIsoTime()
    }
}
@SharedImmutable
internal val ISO_DATETIME_BASIC by lazy {
    LocalDateTimeFormat.build {
        appendYear(); appendMonthNumber(); appendDayOfMonth()
        alternativeParsing({ appendLiteral('t') }) { appendLiteral('T') }
        appendHour(); appendMinute()
        appendOptional {
            appendSecond()
            appendOptional {
                appendLiteral('.')
                appendSecondFraction()
            }
        }
    }
}
