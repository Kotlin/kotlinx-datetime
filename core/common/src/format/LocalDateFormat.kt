/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*

public sealed interface DateFormatBuilder: FormatBuilder {
    public fun appendYear(padding: Padding = Padding.ZERO)
    public fun appendYearTwoDigits(base: Int)
    public fun appendMonthNumber(padding: Padding = Padding.ZERO)
    public fun appendMonthName(names: MonthNames)
    public fun appendDayOfMonth(padding: Padding = Padding.ZERO)
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

    public constructor(
        january: String, february: String, march: String, april: String, may: String, june: String,
        july: String, august: String, september: String, october: String, november: String, december: String
    ) :
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

    public constructor(
        monday: String,
        tuesday: String,
        wednesday: String,
        thursday: String,
        friday: String,
        saturday: String,
        sunday: String
    ) :
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
    appendYear()
    appendLiteral('-')
    appendMonthNumber()
    appendLiteral('-')
    appendDayOfMonth()
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

internal class YearDirective(padding: Padding) :
    SignedIntFieldFormatDirective<DateFieldContainer>(
        DateFields.year,
        minDigits = padding.minDigits(4),
        maxDigits = null,
        spacePadding = padding.spaces(4),
        outputPlusOnExceededWidth = 4,
    ) {
    override val builderRepresentation: String = when (padding) {
        Padding.ZERO -> "${DateFormatBuilder::appendYear.name}()"
        else -> "${DateFormatBuilder::appendYear.name}($padding)"
    }
}

internal class ReducedYearDirective(val base: Int) :
    ReducedIntFieldDirective<DateFieldContainer>(
        DateFields.year,
        digits = 2,
        base = base,
    ) {
    override val builderRepresentation: String = "${DateFormatBuilder::appendYearTwoDigits.name}($base)"
}

internal class MonthDirective(padding: Padding) :
    UnsignedIntFieldFormatDirective<DateFieldContainer>(
        DateFields.month,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2),
    ) {
    override val builderRepresentation: String = when (padding) {
        Padding.ZERO -> "${DateFormatBuilder::appendMonthNumber.name}()"
        else -> "${DateFormatBuilder::appendMonthNumber.name}($padding)"
    }
}

internal class MonthNameDirective(names: List<String>) :
    NamedUnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.month, names) {
    override val builderRepresentation: String =
        "${DateFormatBuilder::appendMonthName.name}(${names.toKotlinCode(String::toKotlinCode)})"
}

internal class DayDirective(padding: Padding) :
    UnsignedIntFieldFormatDirective<DateFieldContainer>(
        DateFields.dayOfMonth,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2),
    ) {
    override val builderRepresentation: String = when (padding) {
        Padding.ZERO -> "${DateFormatBuilder::appendDayOfMonth.name}()"
        else -> "${DateFormatBuilder::appendDayOfMonth.name}($padding)"
    }
}

internal class DayOfWeekDirective(names: List<String>) :
    NamedUnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.isoDayOfWeek, names) {

    override val builderRepresentation: String =
        "${DateFormatBuilder::appendDayOfWeek.name}(${names.toKotlinCode(String::toKotlinCode)})"
}

internal class LocalDateFormat(val actualFormat: StringFormat<DateFieldContainer>) :
    AbstractFormat<LocalDate, IncompleteLocalDate>(actualFormat) {
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

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }

    override fun toString(): String = actualFormat.builderString()
}
