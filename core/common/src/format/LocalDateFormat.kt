/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.parser.Copyable
import kotlinx.datetime.number

/**
 * A description of how the names of weekdays are formatted.
 *
 * Instances of this class are typically used as arguments to [DateTimeFormatBuilder.WithDate.dayOfWeek].
 *
 * Predefined instances are available as [ENGLISH_FULL] and [ENGLISH_ABBREVIATED].
 * You can also create custom instances using the constructor.
 *
 * An [IllegalArgumentException] will be thrown if some day-of-week name is empty or there are duplicate names.
 *
 * @sample kotlinx.datetime.test.samples.format.LocalDateFormatSamples.DayOfWeekNamesSamples.usage
 * @sample kotlinx.datetime.test.samples.format.LocalDateFormatSamples.DayOfWeekNamesSamples.constructionFromList
 */
public class DayOfWeekNames(
    /**
     * A list of the names of weekdays in order from Monday to Sunday.
     *
     * @sample kotlinx.datetime.test.samples.format.LocalDateFormatSamples.DayOfWeekNamesSamples.names
     */
    public val names: List<String>
) {
    init {
        require(names.size == 7) { "Day of week names must contain exactly 7 elements" }
        names.indices.forEach { ix ->
            require(names[ix].isNotEmpty()) { "A day-of-week name can not be empty" }
            for (ix2 in 0 until ix) {
                require(names[ix] != names[ix2]) {
                    "Day-of-week names must be unique, but '${names[ix]}' was repeated"
                }
            }
        }
    }

    /**
     * A constructor that takes the names of weekdays in order from Monday to Sunday.
     *
     * @sample kotlinx.datetime.test.samples.format.LocalDateFormatSamples.DayOfWeekNamesSamples.constructionFromStrings
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
         * English names of weekdays from 'Monday' to 'Sunday'.
         *
         * @sample kotlinx.datetime.test.samples.format.LocalDateFormatSamples.DayOfWeekNamesSamples.englishFull
         */
        public val ENGLISH_FULL: DayOfWeekNames = DayOfWeekNames(
            listOf(
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
            )
        )

        /**
         * Shortened English names of weekdays from 'Mon' to 'Sun'.
         *
         * @sample kotlinx.datetime.test.samples.format.LocalDateFormatSamples.DayOfWeekNamesSamples.englishAbbreviated
         */
        public val ENGLISH_ABBREVIATED: DayOfWeekNames = DayOfWeekNames(
            listOf(
                "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
            )
        )
    }

    /** @suppress */
    override fun toString(): String =
        names.joinToString(", ", "DayOfWeekNames(", ")", transform = String::toString)

    /** @suppress */
    override fun equals(other: Any?): Boolean = other is DayOfWeekNames && names == other.names

    /** @suppress */
    override fun hashCode(): Int = names.hashCode()
}

private fun DayOfWeekNames.toKotlinCode(): String = when (this.names) {
    DayOfWeekNames.ENGLISH_FULL.names -> "DayOfWeekNames.${DayOfWeekNames.Companion::ENGLISH_FULL.name}"
    DayOfWeekNames.ENGLISH_ABBREVIATED.names -> "DayOfWeekNames.${DayOfWeekNames.Companion::ENGLISH_ABBREVIATED.name}"
    else -> names.joinToString(", ", "DayOfWeekNames(", ")", transform = String::toKotlinCode)
}

internal interface DateFieldContainer: YearMonthFieldContainer {
    var day: Int?
    var dayOfWeek: Int?
    var dayOfYear: Int?
}

private object DateFields {
    val day = UnsignedFieldSpec(PropertyAccessor(DateFieldContainer::day), minValue = 1, maxValue = 31)
    val isoDayOfWeek = UnsignedFieldSpec(PropertyAccessor(DateFieldContainer::dayOfWeek), minValue = 1, maxValue = 7)
    val dayOfYear = UnsignedFieldSpec(PropertyAccessor(DateFieldContainer::dayOfYear), minValue = 1, maxValue = 366)
}

/**
 * A [kotlinx.datetime.LocalDate], but potentially incomplete and inconsistent.
 */
internal class IncompleteLocalDate(
    val yearMonth: IncompleteYearMonth = IncompleteYearMonth(),
    override var day: Int? = null,
    override var dayOfWeek: Int? = null,
    override var dayOfYear: Int? = null,
) : YearMonthFieldContainer by yearMonth, DateFieldContainer, Copyable<IncompleteLocalDate> {
    fun toLocalDate(): LocalDate {
        val year = requireParsedField(year, "year")
        val date = when (val dayOfYear = dayOfYear) {
            null -> LocalDate(
                year,
                requireParsedField(monthNumber, "monthNumber"),
                requireParsedField(day, "day")
            )
            else -> LocalDate(year, 1, 1).plus(dayOfYear - 1, DateTimeUnit.DAY).also {
                if (it.year != year) {
                    throw DateTimeFormatException(
                        "Can not create a LocalDate from the given input: " +
                                "the day of year is $dayOfYear, which is not a valid day of year for the year $year"
                    )
                }
                if (monthNumber != null && it.month.number != monthNumber) {
                    throw DateTimeFormatException(
                        "Can not create a LocalDate from the given input: " +
                                "the day of year is $dayOfYear, which is ${it.month}, " +
                                "but $monthNumber was specified as the month number"
                    )
                }
                if (day != null && it.day != day) {
                    throw DateTimeFormatException(
                        "Can not create a LocalDate from the given input: " +
                            "the day of year is $dayOfYear, which is the day ${it.day} of ${it.month}, " +
                                "but $day was specified as the day of month"
                    )
                }
            }
        }
        dayOfWeek?.let {
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
        monthNumber = date.month.number
        day = date.day
        dayOfWeek = date.dayOfWeek.isoDayNumber
        dayOfYear = date.dayOfYear
    }

    override fun copy(): IncompleteLocalDate =
        IncompleteLocalDate(yearMonth.copy(), day, dayOfWeek, dayOfYear)

    override fun equals(other: Any?): Boolean =
        other is IncompleteLocalDate && yearMonth == other.yearMonth &&
            day == other.day && dayOfWeek == other.dayOfWeek && dayOfYear == other.dayOfYear

    override fun hashCode(): Int = yearMonth.hashCode() * 29791 +
            day.hashCode() * 961 +
            dayOfWeek.hashCode() * 31 +
            dayOfYear.hashCode()

    override fun toString(): String = when {
        dayOfYear == null ->
            "$yearMonth-${day ?: "??"} (day of week is ${dayOfWeek ?: "??"})"
        day == null && monthNumber == null ->
            "(${yearMonth.year ?: "??"})-$dayOfYear (day of week is ${dayOfWeek ?: "??"})"
        else -> "$yearMonth-${day ?: "??"} (day of week is ${dayOfWeek ?: "??"}, day of year is $dayOfYear)"
    }
}

private class DayDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<DateFieldContainer>(
        DateFields.day,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2),
    ) {
    override val builderRepresentation: String
        get() = when (padding) {
            Padding.ZERO -> "${DateTimeFormatBuilder.WithDate::day.name}()"
            else -> "${DateTimeFormatBuilder.WithDate::day.name}(${padding.toKotlinCode()})"
        }

    override fun equals(other: Any?): Boolean = other is DayDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

private class DayOfYearDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<DateFieldContainer>(
        DateFields.dayOfYear,
        minDigits = padding.minDigits(3),
        spacePadding = padding.spaces(3),
    ) {
    override val builderRepresentation: String
        get() = when (padding) {
            Padding.ZERO -> "${DateTimeFormatBuilder.WithDate::dayOfYear.name}()"
            else -> "${DateTimeFormatBuilder.WithDate::dayOfYear.name}(${padding.toKotlinCode()})"
        }

    override fun equals(other: Any?): Boolean = other is DayOfYearDirective && padding == other.padding
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

internal interface AbstractWithDateBuilder : AbstractWithYearMonthBuilder, DateTimeFormatBuilder.WithDate {
    fun addFormatStructureForDate(structure: FormatStructure<DateFieldContainer>)

    override fun addFormatStructureForYearMonth(structure: FormatStructure<YearMonthFieldContainer>) {
        addFormatStructureForDate(structure)
    }

    override fun day(padding: Padding) =
        addFormatStructureForDate(BasicFormatStructure(DayDirective(padding)))

    override fun dayOfWeek(names: DayOfWeekNames) =
        addFormatStructureForDate(BasicFormatStructure(DayOfWeekDirective(names)))

    override fun dayOfYear(padding: Padding) =
        addFormatStructureForDate(BasicFormatStructure(DayOfYearDirective(padding)))

    @Suppress("NO_ELSE_IN_WHEN")
    override fun date(format: DateTimeFormat<LocalDate>) = when (format) {
        is LocalDateFormat -> addFormatStructureForDate(format.actualFormat)
    }
}

// these are constants so that the formats are not recreated every time they are used
internal val ISO_DATE by lazy {
    LocalDateFormat.build { year(); char('-'); monthNumber(); char('-'); day() }
}
internal val ISO_DATE_BASIC by lazy {
    LocalDateFormat.build { year(); monthNumber(); day() }
}

private val emptyIncompleteLocalDate = IncompleteLocalDate()
