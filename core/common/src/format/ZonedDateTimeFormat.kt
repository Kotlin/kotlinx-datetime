/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.TimeZone
import kotlinx.datetime.UnresolvedZonedDateTime
import kotlinx.datetime.ZonedDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.formatter.FormatterStructure
import kotlinx.datetime.internal.format.formatter.StringFormatterStructure
import kotlinx.datetime.internal.format.parser.Copyable
import kotlinx.datetime.internal.format.parser.ParserStructure
import kotlinx.datetime.internal.format.parser.TimeZoneParserOperation

internal interface DateTimeOffsetZoneFieldContainer : DateTimeFieldContainer, UtcOffsetFieldContainer {
    var timeZoneId: String?
}

internal class IncompleteDateTimeOffsetZone internal constructor(
    val date: IncompleteLocalDate = IncompleteLocalDate(),
    val time: IncompleteLocalTime = IncompleteLocalTime(),
    val offset: IncompleteUtcOffset = IncompleteUtcOffset(),
    override var timeZoneId: String? = null,
) : DateFieldContainer by date, TimeFieldContainer by time, UtcOffsetFieldContainer by offset,
    DateTimeFieldContainer, DateTimeOffsetZoneFieldContainer, Copyable<IncompleteDateTimeOffsetZone> {
    override fun copy(): IncompleteDateTimeOffsetZone =
        IncompleteDateTimeOffsetZone(date.copy(), time.copy(), offset.copy(), timeZoneId)

    override fun equals(other: Any?): Boolean =
        other is IncompleteDateTimeOffsetZone && other.date == date && other.time == time &&
            other.offset == offset && other.timeZoneId == timeZoneId

    override fun hashCode(): Int =
        date.hashCode() xor time.hashCode() xor offset.hashCode() xor (timeZoneId?.hashCode() ?: 0)

    fun toZonedDateTime(): UnresolvedZonedDateTime {
        val timeZoneId = this.timeZoneId ?: throw IllegalArgumentException(
            "ZonedDateTimeFormat requires a time zone ID to be set."
        )
        return UnresolvedZonedDateTime(
            localDateTime = date.toLocalDate().atTime(time.toLocalTime()),
            timeZone = TimeZone.of(timeZoneId),
            preferredUtcOffset = offset.offsetIsNegative?.let { offset.toUtcOffset() },
        )
    }
}

internal val timeZoneField = GenericFieldSpec(PropertyAccessor(IncompleteDateTimeOffsetZone::timeZoneId))

internal class TimeZoneIdDirective() : FieldFormatDirective<IncompleteDateTimeOffsetZone> {
    override val field: FieldSpec<IncompleteDateTimeOffsetZone, String>
        get() = timeZoneField

    override val builderRepresentation: String
        get() = "${DateTimeFormatBuilder.WithDateTimeComponents::timeZoneId.name}()"

    override fun formatter(): FormatterStructure<IncompleteDateTimeOffsetZone> {
        return StringFormatterStructure(field.accessor::getterNotNull)
    }

    override fun parser(): ParserStructure<IncompleteDateTimeOffsetZone> =
        ParserStructure(
            listOf(TimeZoneParserOperation(timeZoneField.accessor)),
            emptyList()
        )
}

internal class DateTimeComponentsFormat(override val actualFormat: CachedFormatStructure<IncompleteDateTimeOffsetZone>) :
    AbstractDateTimeFormat<DateTimeComponents, IncompleteDateTimeOffsetZone>() {
    override fun intermediateFromValue(value: DateTimeComponents): IncompleteDateTimeOffsetZone = value.contents

    override fun valueFromIntermediate(intermediate: IncompleteDateTimeOffsetZone): DateTimeComponents =
        DateTimeComponents(intermediate)

    override val emptyIntermediate get() = emptyDateTimeOffsetZoneContents

    class Builder(override val actualBuilder: AppendableFormatStructure<IncompleteDateTimeOffsetZone>) :
        AbstractDateTimeFormatBuilder<IncompleteDateTimeOffsetZone, Builder>, AbstractWithDateTimeBuilder,
        AbstractWithOffsetBuilder, DateTimeFormatBuilder.WithDateTimeComponents {
        override fun addFormatStructureForDateTime(structure: FormatStructure<DateTimeFieldContainer>) {
            actualBuilder.add(structure)
        }

        override fun addFormatStructureForOffset(structure: FormatStructure<UtcOffsetFieldContainer>) {
            actualBuilder.add(structure)
        }

        override fun timeZoneId() =
            actualBuilder.add(BasicFormatStructure(TimeZoneIdDirective()))

        @Suppress("NO_ELSE_IN_WHEN")
        override fun zonedDateTime(format: DateTimeFormat<UnresolvedZonedDateTime>) = when (format) {
            is DateTimeComponentsFormat -> actualBuilder.add(format.actualFormat)
        }

        @Suppress("NO_ELSE_IN_WHEN")
        override fun dateTimeComponents(format: DateTimeFormat<DateTimeComponents>) = when (format) {
            is DateTimeComponentsFormat -> actualBuilder.add(format.actualFormat)
        }

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }
}

internal class ZonedDateTimeFormat(override val actualFormat: CachedFormatStructure<IncompleteDateTimeOffsetZone>) :
    AbstractDateTimeFormat<UnresolvedZonedDateTime, IncompleteDateTimeOffsetZone>() {
    override fun intermediateFromValue(value: UnresolvedZonedDateTime): IncompleteDateTimeOffsetZone =
        IncompleteDateTimeOffsetZone().apply {
            date.populateFrom(value.rawLocalDateTime.date)
            time.populateFrom(value.rawLocalDateTime.time)
            value.preferredOffset?.let { offset.populateFrom(it) }
            timeZoneId = value.timeZone.id
        }

    override fun valueFromIntermediate(intermediate: IncompleteDateTimeOffsetZone): UnresolvedZonedDateTime =
        intermediate.toZonedDateTime()

    override val emptyIntermediate get() = emptyDateTimeOffsetZoneContents

    class Builder(override val actualBuilder: AppendableFormatStructure<IncompleteDateTimeOffsetZone>) :
        AbstractDateTimeFormatBuilder<IncompleteDateTimeOffsetZone, Builder>, AbstractWithDateTimeBuilder,
        AbstractWithOffsetBuilder, DateTimeFormatBuilder.WithZonedDateTime {
        override fun addFormatStructureForDateTime(structure: FormatStructure<DateTimeFieldContainer>) {
            actualBuilder.add(structure)
        }

        override fun addFormatStructureForOffset(structure: FormatStructure<UtcOffsetFieldContainer>) {
            actualBuilder.add(structure)
        }

        override fun timeZoneId() =
            actualBuilder.add(BasicFormatStructure(TimeZoneIdDirective()))

        @Suppress("NO_ELSE_IN_WHEN")
        override fun zonedDateTime(format: DateTimeFormat<UnresolvedZonedDateTime>) = when (format) {
            is DateTimeComponentsFormat -> actualBuilder.add(format.actualFormat)
        }

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }
}

private val emptyDateTimeOffsetZoneContents = IncompleteDateTimeOffsetZone()
