/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*

public sealed interface DateFormatBuilderFields: FormatBuilder {
    public fun appendYear(minDigits: Int = 1, outputPlusOnExceededPadding: Boolean = false)
    public fun appendMonthNumber(minLength: Int = 1)
    public fun appendMonthName(names: List<String>)
    public fun appendDayOfMonth(minLength: Int = 1)
    public fun appendDayOfWeek(names: List<String>)
}

public class LocalDateFormat private constructor(private val actualFormat: StringFormat<DateFieldContainer>)
    : Format<LocalDate> by LocalDateFormatImpl(actualFormat) {
    public companion object {
        public fun build(block: DateFormatBuilderFields.() -> Unit): LocalDateFormat {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return LocalDateFormat(builder.build())
        }

        public val ISO: LocalDateFormat = build { appendIsoDate() }
    }

    internal class Builder(override val actualBuilder: AppendableFormatStructure<DateFieldContainer>) :
        AbstractFormatBuilder<DateFieldContainer, Builder>, DateFormatBuilderFields {
        override fun appendYear(minDigits: Int, outputPlusOnExceededPadding: Boolean) =
            actualBuilder.add(BasicFormatStructure(YearDirective(minDigits, outputPlusOnExceededPadding)))

        override fun appendMonthNumber(minLength: Int) =
            actualBuilder.add(BasicFormatStructure(MonthDirective(minLength)))

        override fun appendMonthName(names: List<String>) =
            actualBuilder.add(BasicFormatStructure(MonthNameDirective(names)))

        override fun appendDayOfMonth(minLength: Int) = actualBuilder.add(BasicFormatStructure(DayDirective(minLength)))
        override fun appendDayOfWeek(names: List<String>) =
            actualBuilder.add(BasicFormatStructure(DayOfWeekDirective(names)))

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }

    override fun toString(): String = actualFormat.builderString()

}

internal fun DateFormatBuilderFields.appendIsoDate() {
    appendYear(4, outputPlusOnExceededPadding = true)
    appendLiteral('-')
    appendMonthNumber(2)
    appendLiteral('-')
    appendDayOfMonth(2)
}

public fun LocalDate.format(format: LocalDateFormat): String = format.format(this)

public fun LocalDate.Companion.parse(input: String, format: LocalDateFormat): LocalDate = format.parse(input)

internal fun LocalDate.toIncompleteLocalDate(): IncompleteLocalDate =
    IncompleteLocalDate(year, monthNumber, dayOfMonth, dayOfWeek.isoDayNumber)

internal fun <T> getParsedField(field: T?, name: String): T {
    if (field == null) {
        throw DateTimeFormatException("Can not create a $name from the given input: the field $name is missing")
    }
    return field
}

internal interface DateFieldContainer {
    var year: Int?
    var monthNumber: Int?
    var dayOfMonth: Int?
    var isoDayOfWeek: Int?
}

internal object DateFields {
    val year = SignedFieldSpec(DateFieldContainer::year, maxAbsoluteValue = null)
    val month = UnsignedFieldSpec(DateFieldContainer::monthNumber, minValue = 1, maxValue = 12)
    val dayOfMonth = UnsignedFieldSpec(DateFieldContainer::dayOfMonth, minValue = 1, maxValue = 31)
    val isoDayOfWeek = UnsignedFieldSpec(DateFieldContainer::isoDayOfWeek, minValue = 1, maxValue = 7)
}

/**
 * A [kotlinx.datetime.LocalDate], but potentially incomplete and inconsistent.
 */
internal class IncompleteLocalDate(
    override var year: Int? = null,
    override var monthNumber: Int? = null,
    override var dayOfMonth: Int? = null,
    override var isoDayOfWeek: Int? = null
) : DateFieldContainer, Copyable<IncompleteLocalDate> {
    fun toLocalDate(): LocalDate {
        val date = LocalDate(
            getParsedField(year, "year"),
            getParsedField(monthNumber, "monthNumber"),
            getParsedField(dayOfMonth, "dayOfMonth")
        )
        isoDayOfWeek?.let {
            if (it != date.dayOfWeek.isoDayNumber) {
                throw DateTimeFormatException(
                    "Can not create a LocalDate from the given input: " +
                        "the day of week is ${DayOfWeek(it)} but the date is $date, which is a ${date.dayOfWeek}"
                )
            }
        }
        return date
    }

    override fun copy(): IncompleteLocalDate = IncompleteLocalDate(year, monthNumber, dayOfMonth, isoDayOfWeek)

    override fun equals(other: Any?): Boolean =
        other is IncompleteLocalDate && year == other.year && monthNumber == other.monthNumber &&
            dayOfMonth == other.dayOfMonth && isoDayOfWeek == other.isoDayOfWeek

    override fun hashCode(): Int =
        year.hashCode() * 31 + monthNumber.hashCode() * 31 + dayOfMonth.hashCode() * 31 + isoDayOfWeek.hashCode() * 31

    override fun toString(): String =
        "${year ?: "??"}-${monthNumber ?: "??"}-${dayOfMonth ?: "??"} (day of week is ${isoDayOfWeek ?: "??"})"
}

internal class YearDirective(digits: Int, outputPlusOnExceededPadding: Boolean) :
    SignedIntFieldFormatDirective<DateFieldContainer>(
        DateFields.year,
        minDigits = digits,
        maxDigits = null,
        outputPlusOnExceededPadding = outputPlusOnExceededPadding,
    ) {
    override val builderRepresentation: String = when {
        digits == 1 && !outputPlusOnExceededPadding -> "${DateFormatBuilderFields::appendYear.name}()"
        !outputPlusOnExceededPadding -> "${DateFormatBuilderFields::appendYear.name}($digits)"
        digits == 1 -> "${DateFormatBuilderFields::appendYear.name}(outputPlusOnExceededPadding = true)"
        else -> "${DateFormatBuilderFields::appendYear.name}($digits, $outputPlusOnExceededPadding)"
    }
}

internal class MonthDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.month, minDigits) {
    override val builderRepresentation: String = when (minDigits) {
        1 -> "${DateFormatBuilderFields::appendMonthNumber.name}()"
        else -> "${DateFormatBuilderFields::appendMonthNumber.name}($minDigits)"
    }
}

internal class MonthNameDirective(names: List<String>) :
    NamedUnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.month, names) {
    override val builderRepresentation: String =
        "${DateFormatBuilderFields::appendMonthName.name}(${names.repr(String::repr)})"
}

internal class DayDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.dayOfMonth, minDigits) {

    override val builderRepresentation: String = when (minDigits) {
        1 -> "${DateFormatBuilderFields::appendDayOfMonth.name}()"
        else -> "${DateFormatBuilderFields::appendDayOfMonth.name}($minDigits)"
    }
}

internal class DayOfWeekDirective(names: List<String>) :
    NamedUnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.isoDayOfWeek, names) {

    override val builderRepresentation: String =
        "${DateFormatBuilderFields::appendDayOfWeek.name}(${names.repr(String::repr)})"
}

private class LocalDateFormatImpl(actualFormat: StringFormat<IncompleteLocalDate>)
    : AbstractFormat<LocalDate, IncompleteLocalDate>(actualFormat)
{
    override fun intermediateFromValue(value: LocalDate): IncompleteLocalDate = value.toIncompleteLocalDate()

    override fun valueFromIntermediate(intermediate: IncompleteLocalDate): LocalDate = intermediate.toLocalDate()

    override fun newIntermediate(): IncompleteLocalDate = IncompleteLocalDate()
}
