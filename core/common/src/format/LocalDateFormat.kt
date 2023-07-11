/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*

public sealed interface DateFormatBuilder: FormatBuilder {
    public fun appendYear(minDigits: Int = 1, outputPlusOnExceededPadding: Boolean = false)
    public fun appendMonthNumber(minLength: Int = 1)
    public fun appendMonthName(names: MonthNames)
    public fun appendDayOfMonth(minLength: Int = 1)
    public fun appendDayOfWeek(names: DayOfWeekNames)
}

/**
 * A description of how month names are formatted.
 */
public class MonthNames(
    /**
     * A list of month names, in order from January to December.
     */
    public val names: List<String>
) {
    init {
        require(names.size == 12) { "Month names must contain exactly 12 elements" }
    }

    public constructor(january: String, february: String, march: String, april: String, may: String, june: String,
                       july: String, august: String, september: String, october: String, november: String, december: String) :
        this(listOf(january, february, march, april, may, june, july, august, september, october, november, december))

    public companion object {
        /**
         * English month names, 'January' to 'December'.
         */
        public val ENGLISH_FULL: MonthNames = MonthNames(
            listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
        )

        /**
         * Shortened English month names, 'Jan' to 'Dec'.
         */
        public val ENGLISH_ABBREVIATED: MonthNames = MonthNames(
            listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
        )
    }
}

/**
 * A description of how day of week names are formatted.
 */
public class DayOfWeekNames(
    /**
     * A list of day of week names, in order from Monday to Sunday.
     */
    public val names: List<String>
) {
    init {
        require(names.size == 7) { "Day of week names must contain exactly 7 elements" }
    }

    public constructor(monday: String, tuesday: String, wednesday: String, thursday: String, friday: String, saturday: String, sunday: String) :
        this(listOf(monday, tuesday, wednesday, thursday, friday, saturday, sunday))

    public companion object {
        /**
         * English day of week names, 'Monday' to 'Sunday'.
         */
        public val ENGLISH_FULL: DayOfWeekNames = DayOfWeekNames(
            listOf(
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
            )
        )

        /**
         * Shortened English day of week names, 'Mon' to 'Sun'.
         */
        public val ENGLISH_ABBREVIATED: DayOfWeekNames = DayOfWeekNames(
            listOf(
                "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
            )
        )
    }
}

internal fun DateFormatBuilder.appendIsoDate() {
    appendYear(4, outputPlusOnExceededPadding = true)
    appendLiteral('-')
    appendMonthNumber(2)
    appendLiteral('-')
    appendDayOfMonth(2)
}

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
        digits == 1 && !outputPlusOnExceededPadding -> "${DateFormatBuilder::appendYear.name}()"
        !outputPlusOnExceededPadding -> "${DateFormatBuilder::appendYear.name}($digits)"
        digits == 1 -> "${DateFormatBuilder::appendYear.name}(outputPlusOnExceededPadding = true)"
        else -> "${DateFormatBuilder::appendYear.name}($digits, $outputPlusOnExceededPadding)"
    }
}

internal class MonthDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.month, minDigits) {
    override val builderRepresentation: String = when (minDigits) {
        1 -> "${DateFormatBuilder::appendMonthNumber.name}()"
        else -> "${DateFormatBuilder::appendMonthNumber.name}($minDigits)"
    }
}

internal class MonthNameDirective(names: List<String>) :
    NamedUnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.month, names) {
    override val builderRepresentation: String =
        "${DateFormatBuilder::appendMonthName.name}(${names.repr(String::repr)})"
}

internal class DayDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.dayOfMonth, minDigits) {

    override val builderRepresentation: String = when (minDigits) {
        1 -> "${DateFormatBuilder::appendDayOfMonth.name}()"
        else -> "${DateFormatBuilder::appendDayOfMonth.name}($minDigits)"
    }
}

internal class DayOfWeekDirective(names: List<String>) :
    NamedUnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.isoDayOfWeek, names) {

    override val builderRepresentation: String =
        "${DateFormatBuilder::appendDayOfWeek.name}(${names.repr(String::repr)})"
}

internal class LocalDateFormat(private val actualFormat: StringFormat<IncompleteLocalDate>)
    : AbstractFormat<LocalDate, IncompleteLocalDate>(actualFormat)
{
    override fun intermediateFromValue(value: LocalDate): IncompleteLocalDate = value.toIncompleteLocalDate()

    override fun valueFromIntermediate(intermediate: IncompleteLocalDate): LocalDate = intermediate.toLocalDate()

    override fun newIntermediate(): IncompleteLocalDate = IncompleteLocalDate()

    companion object {
        fun build(block: DateFormatBuilder.() -> Unit): Format<LocalDate> {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return LocalDateFormat(builder.build())
        }

        val ISO: Format<LocalDate> = build { appendIsoDate() }
    }

    internal class Builder(override val actualBuilder: AppendableFormatStructure<DateFieldContainer>) :
        AbstractFormatBuilder<DateFieldContainer, Builder>, DateFormatBuilder {
        override fun appendYear(minDigits: Int, outputPlusOnExceededPadding: Boolean) =
            actualBuilder.add(BasicFormatStructure(YearDirective(minDigits, outputPlusOnExceededPadding)))

        override fun appendMonthNumber(minLength: Int) =
            actualBuilder.add(BasicFormatStructure(MonthDirective(minLength)))

        override fun appendMonthName(names: MonthNames) =
            actualBuilder.add(BasicFormatStructure(MonthNameDirective(names.names)))

        override fun appendDayOfMonth(minLength: Int) = actualBuilder.add(BasicFormatStructure(DayDirective(minLength)))
        override fun appendDayOfWeek(names: DayOfWeekNames) =
            actualBuilder.add(BasicFormatStructure(DayOfWeekDirective(names.names)))

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }

    override fun toString(): String = actualFormat.builderString()
}
