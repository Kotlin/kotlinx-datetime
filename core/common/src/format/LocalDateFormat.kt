/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.parser.Copyable

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

    /**
     * Create a [MonthNames] using the month names in order from January to December.
     */
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

internal fun MonthNames.toKotlinCode(): String = when (this.names) {
    MonthNames.ENGLISH_FULL.names -> "MonthNames.${DayOfWeekNames.Companion::ENGLISH_FULL.name}"
    MonthNames.ENGLISH_ABBREVIATED.names -> "MonthNames.${DayOfWeekNames.Companion::ENGLISH_ABBREVIATED.name}"
    else -> names.joinToString(", ", "MonthNames(", ")", transform = String::toKotlinCode)
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

    /**
     * A constructor that takes the day of week names, in order from Monday to Sunday.
     */
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

internal fun DayOfWeekNames.toKotlinCode(): String = when (this.names) {
    DayOfWeekNames.ENGLISH_FULL.names -> "DayOfWeekNames.${DayOfWeekNames.Companion::ENGLISH_FULL.name}"
    DayOfWeekNames.ENGLISH_ABBREVIATED.names -> "DayOfWeekNames.${DayOfWeekNames.Companion::ENGLISH_ABBREVIATED.name}"
    else -> names.joinToString(", ", "DayOfWeekNames(", ")", transform = String::toKotlinCode)
}

internal fun <T> requireParsedField(field: T?, name: String): T {
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

private object DateFields {
    val year = GenericFieldSpec(PropertyAccessor(DateFieldContainer::year))
    val month = UnsignedFieldSpec(PropertyAccessor(DateFieldContainer::monthNumber), minValue = 1, maxValue = 12)
    val dayOfMonth = UnsignedFieldSpec(PropertyAccessor(DateFieldContainer::dayOfMonth), minValue = 1, maxValue = 31)
    val isoDayOfWeek = UnsignedFieldSpec(PropertyAccessor(DateFieldContainer::isoDayOfWeek), minValue = 1, maxValue = 7)
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
            requireParsedField(year, "year"),
            requireParsedField(monthNumber, "monthNumber"),
            requireParsedField(dayOfMonth, "dayOfMonth")
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

    fun populateFrom(date: LocalDate) {
        year = date.year
        monthNumber = date.monthNumber
        dayOfMonth = date.dayOfMonth
        isoDayOfWeek = date.dayOfWeek.isoDayNumber
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

private class YearDirective(private val padding: Padding, private val isYearOfEra: Boolean = false) :
    SignedIntFieldFormatDirective<DateFieldContainer>(
        DateFields.year,
        minDigits = padding.minDigits(4),
        maxDigits = null,
        spacePadding = padding.spaces(4),
        outputPlusOnExceededWidth = 4,
    ) {
    override val builderRepresentation: String
        get() = when (padding) {
            Padding.ZERO -> "${DateTimeFormatBuilder.WithDate::year.name}()"
            else -> "${DateTimeFormatBuilder.WithDate::year.name}(${padding.toKotlinCode()})"
        }.let {
            if (isYearOfEra) {
                it + YEAR_OF_ERA_COMMENT
            } else it
        }

    override fun equals(other: Any?): Boolean =
        other is YearDirective && padding == other.padding && isYearOfEra == other.isYearOfEra

    override fun hashCode(): Int = padding.hashCode() * 31 + isYearOfEra.hashCode()
}

private class ReducedYearDirective(val base: Int, private val isYearOfEra: Boolean = false) :
    ReducedIntFieldDirective<DateFieldContainer>(
        DateFields.year,
        digits = 2,
        base = base,
    ) {
    override val builderRepresentation: String
        get() =
            "${DateTimeFormatBuilder.WithDate::yearTwoDigits.name}($base)".let {
                if (isYearOfEra) {
                    it + YEAR_OF_ERA_COMMENT
                } else it
            }

    override fun equals(other: Any?): Boolean =
        other is ReducedYearDirective && base == other.base && isYearOfEra == other.isYearOfEra

    override fun hashCode(): Int = base.hashCode() * 31 + isYearOfEra.hashCode()
}

private const val YEAR_OF_ERA_COMMENT =
    " /** TODO: the original format had an `y` directive, so the behavior is different on years earlier than 1 AD. See the [kotlinx.datetime.format.byUnicodePattern] documentation for details. */"

/**
 * A special directive for year-of-era that behaves equivalently to [DateTimeFormatBuilder.WithDate.year].
 * This is the result of calling [byUnicodePattern] on a pattern that uses the ubiquitous "y" symbol.
 * We need a separate directive so that, when using [DateTimeFormat.formatAsKotlinBuilderDsl], we can print an
 * additional comment and explain that the behavior was not preserved exactly.
 */
internal fun DateTimeFormatBuilder.WithDate.yearOfEra(padding: Padding) {
    @Suppress("NO_ELSE_IN_WHEN")
    when (this) {
        is AbstractWithDateBuilder -> addFormatStructureForDate(
            BasicFormatStructure(YearDirective(padding, isYearOfEra = true))
        )
    }
}

/**
 * A special directive for year-of-era that behaves equivalently to [DateTimeFormatBuilder.WithDate.year].
 * This is the result of calling [byUnicodePattern] on a pattern that uses the ubiquitous "y" symbol.
 * We need a separate directive so that, when using [DateTimeFormat.formatAsKotlinBuilderDsl], we can print an
 * additional comment and explain that the behavior was not preserved exactly.
 */
internal fun DateTimeFormatBuilder.WithDate.yearOfEraTwoDigits(baseYear: Int) {
    @Suppress("NO_ELSE_IN_WHEN")
    when (this) {
        is AbstractWithDateBuilder -> addFormatStructureForDate(
            BasicFormatStructure(ReducedYearDirective(baseYear, isYearOfEra = true))
        )
    }
}

private class MonthDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<DateFieldContainer>(
        DateFields.month,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2),
    ) {
    override val builderRepresentation: String
        get() = when (padding) {
            Padding.ZERO -> "${DateTimeFormatBuilder.WithDate::monthNumber.name}()"
            else -> "${DateTimeFormatBuilder.WithDate::monthNumber.name}(${padding.toKotlinCode()})"
        }

    override fun equals(other: Any?): Boolean = other is MonthDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

private class MonthNameDirective(private val names: MonthNames) :
    NamedUnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.month, names.names, "monthName") {
    override val builderRepresentation: String
        get() =
            "${DateTimeFormatBuilder.WithDate::monthName.name}(${names.toKotlinCode()})"

    override fun equals(other: Any?): Boolean = other is MonthNameDirective && names.names == other.names.names
    override fun hashCode(): Int = names.names.hashCode()
}

private class DayDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<DateFieldContainer>(
        DateFields.dayOfMonth,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2),
    ) {
    override val builderRepresentation: String
        get() = when (padding) {
            Padding.ZERO -> "${DateTimeFormatBuilder.WithDate::dayOfMonth.name}()"
            else -> "${DateTimeFormatBuilder.WithDate::dayOfMonth.name}(${padding.toKotlinCode()})"
        }

    override fun equals(other: Any?): Boolean = other is DayDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

private class DayOfWeekDirective(private val names: DayOfWeekNames) :
    NamedUnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.isoDayOfWeek, names.names, "dayOfWeekName") {

    override val builderRepresentation: String
        get() =
            "${DateTimeFormatBuilder.WithDate::dayOfWeek.name}(${names.toKotlinCode()})"

    override fun equals(other: Any?): Boolean = other is DayOfWeekDirective && names.names == other.names.names
    override fun hashCode(): Int = names.names.hashCode()
}

internal class LocalDateFormat(override val actualFormat: CachedFormatStructure<DateFieldContainer>) :
    AbstractDateTimeFormat<LocalDate, IncompleteLocalDate>() {
    override fun intermediateFromValue(value: LocalDate): IncompleteLocalDate =
        IncompleteLocalDate().apply { populateFrom(value) }

    override fun valueFromIntermediate(intermediate: IncompleteLocalDate): LocalDate = intermediate.toLocalDate()

    override val emptyIntermediate get() = emptyIncompleteLocalDate

    companion object {
        fun build(block: DateTimeFormatBuilder.WithDate.() -> Unit): DateTimeFormat<LocalDate> {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return LocalDateFormat(builder.build())
        }
    }

    internal class Builder(override val actualBuilder: AppendableFormatStructure<DateFieldContainer>) :
        AbstractDateTimeFormatBuilder<DateFieldContainer, Builder>, AbstractWithDateBuilder {

        override fun addFormatStructureForDate(structure: FormatStructure<DateFieldContainer>) =
            actualBuilder.add(structure)

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }
}

internal interface AbstractWithDateBuilder : DateTimeFormatBuilder.WithDate {
    fun addFormatStructureForDate(structure: FormatStructure<DateFieldContainer>)

    override fun year(padding: Padding) =
        addFormatStructureForDate(BasicFormatStructure(YearDirective(padding)))

    override fun yearTwoDigits(baseYear: Int) =
        addFormatStructureForDate(BasicFormatStructure(ReducedYearDirective(baseYear)))

    override fun monthNumber(padding: Padding) =
        addFormatStructureForDate(BasicFormatStructure(MonthDirective(padding)))

    override fun monthName(names: MonthNames) =
        addFormatStructureForDate(BasicFormatStructure(MonthNameDirective(names)))

    override fun dayOfMonth(padding: Padding) = addFormatStructureForDate(BasicFormatStructure(DayDirective(padding)))
    override fun dayOfWeek(names: DayOfWeekNames) =
        addFormatStructureForDate(BasicFormatStructure(DayOfWeekDirective(names)))

    @Suppress("NO_ELSE_IN_WHEN")
    override fun date(format: DateTimeFormat<LocalDate>) = when (format) {
        is LocalDateFormat -> addFormatStructureForDate(format.actualFormat)
    }
}

// these are constants so that the formats are not recreated every time they are used
internal val ISO_DATE by lazy {
    LocalDateFormat.build { year(); char('-'); monthNumber(); char('-'); dayOfMonth() }
}
internal val ISO_DATE_BASIC by lazy {
    LocalDateFormat.build { year(); monthNumber(); dayOfMonth() }
}

private val emptyIncompleteLocalDate = IncompleteLocalDate()
