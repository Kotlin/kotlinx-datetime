/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.parser.Copyable

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

internal class LocalDateTimeFormat(override val actualFormat: CachedFormatStructure<DateTimeFieldContainer>) :
    AbstractDateTimeFormat<LocalDateTime, IncompleteLocalDateTime>() {
    override fun intermediateFromValue(value: LocalDateTime): IncompleteLocalDateTime =
        IncompleteLocalDateTime().apply { populateFrom(value) }

    override fun valueFromIntermediate(intermediate: IncompleteLocalDateTime): LocalDateTime =
        intermediate.toLocalDateTime()

    override val emptyIntermediate: IncompleteLocalDateTime get() = emptyIncompleteLocalDateTime

    companion object {
        fun build(block: DateTimeFormatBuilder.WithDateTime.() -> Unit): LocalDateTimeFormat {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return LocalDateTimeFormat(builder.build())
        }
    }

    internal class Builder(override val actualBuilder: AppendableFormatStructure<DateTimeFieldContainer>) :
        AbstractDateTimeFormatBuilder<DateTimeFieldContainer, Builder>, AbstractWithDateTimeBuilder {

        override fun addFormatStructureForDateTime(structure: FormatStructure<DateTimeFieldContainer>) {
            actualBuilder.add(structure)
        }

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }
}

internal interface AbstractWithDateTimeBuilder:
    AbstractWithDateBuilder, AbstractWithTimeBuilder, DateTimeFormatBuilder.WithDateTime
{
    fun addFormatStructureForDateTime(structure: FormatStructure<DateTimeFieldContainer>)

    override fun addFormatStructureForDate(structure: FormatStructure<DateFieldContainer>) {
        addFormatStructureForDateTime(structure)
    }

    override fun addFormatStructureForTime(structure: FormatStructure<TimeFieldContainer>) {
        addFormatStructureForDateTime(structure)
    }

    @Suppress("NO_ELSE_IN_WHEN")
    override fun dateTime(format: DateTimeFormat<LocalDateTime>) = when (format) {
        is LocalDateTimeFormat -> addFormatStructureForDateTime(format.actualFormat)
    }
}

// these are constants so that the formats are not recreated every time they are used
internal val ISO_DATETIME by lazy {
    LocalDateTimeFormat.build {
        date(ISO_DATE)
        alternativeParsing({ char('t') }) { char('T') }
        time(ISO_TIME)
    }
}

private val emptyIncompleteLocalDateTime = IncompleteLocalDateTime()
