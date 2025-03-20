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
 *
 * Instances of this class are typically used as arguments to [DateTimeFormatBuilder.WithYearMonth.monthName].
 *
 * Predefined instances are available as [ENGLISH_FULL] and [ENGLISH_ABBREVIATED].
 * You can also create custom instances using the constructor.
 *
 * An [IllegalArgumentException] will be thrown if some month name is empty or there are duplicate names.
 *
 * @sample kotlinx.datetime.test.samples.format.YearMonthFormatSamples.MonthNamesSamples.usage
 * @sample kotlinx.datetime.test.samples.format.YearMonthFormatSamples.MonthNamesSamples.constructionFromList
 */
public class MonthNames(
    /**
     * A list of month names in order from January to December.
     *
     * @sample kotlinx.datetime.test.samples.format.YearMonthFormatSamples.MonthNamesSamples.names
     */
    public val names: List<String>
) {
    init {
        require(names.size == 12) { "Month names must contain exactly 12 elements" }
        names.indices.forEach { ix ->
            require(names[ix].isNotEmpty()) { "A month name can not be empty" }
            for (ix2 in 0 until ix) {
                require(names[ix] != names[ix2]) {
                    "Month names must be unique, but '${names[ix]}' was repeated"
                }
            }
        }
    }

    /**
     * Create a [MonthNames] using the month names in order from January to December.
     *
     * @sample kotlinx.datetime.test.samples.format.YearMonthFormatSamples.MonthNamesSamples.constructionFromStrings
     */
    public constructor(
        january: String, february: String, march: String, april: String, may: String, june: String,
        july: String, august: String, september: String, october: String, november: String, december: String
    ) :
            this(listOf(january, february, march, april, may, june, july, august, september, october, november, december))

    public companion object {
        /**
         * English month names from 'January' to 'December'.
         *
         * @sample kotlinx.datetime.test.samples.format.YearMonthFormatSamples.MonthNamesSamples.englishFull
         */
        public val ENGLISH_FULL: MonthNames = MonthNames(
            listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
        )

        /**
         * Shortened English month names from 'Jan' to 'Dec'.
         *
         * @sample kotlinx.datetime.test.samples.format.YearMonthFormatSamples.MonthNamesSamples.englishAbbreviated
         */
        public val ENGLISH_ABBREVIATED: MonthNames = MonthNames(
            listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
        )
    }

    /** @suppress */
    override fun toString(): String =
        names.joinToString(", ", "MonthNames(", ")", transform = String::toString)

    /** @suppress */
    override fun equals(other: Any?): Boolean = other is MonthNames && names == other.names

    /** @suppress */
    override fun hashCode(): Int = names.hashCode()
}

private fun MonthNames.toKotlinCode(): String = when (this.names) {
    MonthNames.ENGLISH_FULL.names -> "MonthNames.${MonthNames::ENGLISH_FULL.name}"
    MonthNames.ENGLISH_ABBREVIATED.names -> "MonthNames.${MonthNames::ENGLISH_ABBREVIATED.name}"
    else -> names.joinToString(", ", "MonthNames(", ")", transform = String::toKotlinCode)
}

internal fun <T> requireParsedField(field: T?, name: String): T {
    if (field == null) {
        throw DateTimeFormatException("Can not create a $name from the given input: the field $name is missing")
    }
    return field
}

internal interface YearMonthFieldContainer {
    var year: Int?
    var monthNumber: Int?
}

private object YearMonthFields {
    val year = GenericFieldSpec(PropertyAccessor(YearMonthFieldContainer::year))
    val month = UnsignedFieldSpec(PropertyAccessor(YearMonthFieldContainer::monthNumber), minValue = 1, maxValue = 12)
}

internal class IncompleteYearMonth(
    override var year: Int? = null,
    override var monthNumber: Int? = null,
) : YearMonthFieldContainer, Copyable<IncompleteYearMonth> {
    fun toYearMonth(): YearMonth {
        val year = requireParsedField(year, "year")
        val month = requireParsedField(monthNumber, "monthNumber")
        return YearMonth(year, month)
    }

    fun populateFrom(yearMonth: YearMonth) {
        year = yearMonth.year
        monthNumber = yearMonth.month.number
    }

    override fun copy(): IncompleteYearMonth = IncompleteYearMonth(year, monthNumber)

    override fun equals(other: Any?): Boolean =
        other is IncompleteYearMonth && year == other.year && monthNumber == other.monthNumber

    override fun hashCode(): Int = year.hashCode() * 31 + monthNumber.hashCode()

    override fun toString(): String = "${year ?: "??"}-${monthNumber ?: "??"}"
}

private class YearDirective(private val padding: Padding, private val isYearOfEra: Boolean = false) :
    SignedIntFieldFormatDirective<YearMonthFieldContainer>(
        YearMonthFields.year,
        minDigits = padding.minDigits(4),
        maxDigits = null,
        spacePadding = padding.spaces(4),
        outputPlusOnExceededWidth = 4,
    ) {
    override val builderRepresentation: String
        get() = when (padding) {
            Padding.ZERO -> "${DateTimeFormatBuilder.WithYearMonth::year.name}()"
            else -> "${DateTimeFormatBuilder.WithYearMonth::year.name}(${padding.toKotlinCode()})"
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
    ReducedIntFieldDirective<YearMonthFieldContainer>(
        YearMonthFields.year,
        digits = 2,
        base = base,
    ) {
    override val builderRepresentation: String
        get() =
            "${DateTimeFormatBuilder.WithYearMonth::yearTwoDigits.name}($base)".let {
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
 * A special directive for year-of-era that behaves equivalently to [DateTimeFormatBuilder.WithYearMonth.year].
 * This is the result of calling [byUnicodePattern] on a pattern that uses the ubiquitous "y" symbol.
 * We need a separate directive so that, when using [DateTimeFormat.formatAsKotlinBuilderDsl], we can print an
 * additional comment and explain that the behavior was not preserved exactly.
 */
internal fun DateTimeFormatBuilder.WithYearMonth.yearOfEra(padding: Padding) {
    @Suppress("NO_ELSE_IN_WHEN")
    when (this) {
        is AbstractWithYearMonthBuilder -> addFormatStructureForYearMonth(
            BasicFormatStructure(YearDirective(padding, isYearOfEra = true))
        )
    }
}

/**
 * A special directive for year-of-era that behaves equivalently to [DateTimeFormatBuilder.WithYearMonth.year].
 * This is the result of calling [byUnicodePattern] on a pattern that uses the ubiquitous "y" symbol.
 * We need a separate directive so that, when using [DateTimeFormat.formatAsKotlinBuilderDsl], we can print an
 * additional comment and explain that the behavior was not preserved exactly.
 */
internal fun DateTimeFormatBuilder.WithYearMonth.yearOfEraTwoDigits(baseYear: Int) {
    @Suppress("NO_ELSE_IN_WHEN")
    when (this) {
        is AbstractWithYearMonthBuilder -> addFormatStructureForYearMonth(
            BasicFormatStructure(ReducedYearDirective(baseYear, isYearOfEra = true))
        )
    }
}

private class MonthDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<YearMonthFieldContainer>(
        YearMonthFields.month,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2),
    ) {
    override val builderRepresentation: String
        get() = when (padding) {
            Padding.ZERO -> "${DateTimeFormatBuilder.WithYearMonth::monthNumber.name}()"
            else -> "${DateTimeFormatBuilder.WithYearMonth::monthNumber.name}(${padding.toKotlinCode()})"
        }

    override fun equals(other: Any?): Boolean = other is MonthDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

private class MonthNameDirective(private val names: MonthNames) :
    NamedUnsignedIntFieldFormatDirective<YearMonthFieldContainer>(YearMonthFields.month, names.names, "monthName") {
    override val builderRepresentation: String
        get() =
            "${DateTimeFormatBuilder.WithYearMonth::monthName.name}(${names.toKotlinCode()})"

    override fun equals(other: Any?): Boolean = other is MonthNameDirective && names.names == other.names.names
    override fun hashCode(): Int = names.names.hashCode()
}

internal class YearMonthFormat(override val actualFormat: CachedFormatStructure<YearMonthFieldContainer>) :
    AbstractDateTimeFormat<YearMonth, IncompleteYearMonth>() {
    override fun intermediateFromValue(value: YearMonth): IncompleteYearMonth =
        IncompleteYearMonth().apply { populateFrom(value) }

    override fun valueFromIntermediate(intermediate: IncompleteYearMonth): YearMonth = intermediate.toYearMonth()

    override val emptyIntermediate get() = emptyIncompleteYearMonth

    companion object {
        fun build(block: DateTimeFormatBuilder.WithYearMonth.() -> Unit): DateTimeFormat<YearMonth> {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return YearMonthFormat(builder.build())
        }
    }

    internal class Builder(override val actualBuilder: AppendableFormatStructure<YearMonthFieldContainer>) :
        AbstractDateTimeFormatBuilder<YearMonthFieldContainer, Builder>, AbstractWithYearMonthBuilder {

        override fun addFormatStructureForYearMonth(structure: FormatStructure<YearMonthFieldContainer>) =
            actualBuilder.add(structure)

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }
}

internal interface AbstractWithYearMonthBuilder : DateTimeFormatBuilder.WithYearMonth {
    fun addFormatStructureForYearMonth(structure: FormatStructure<YearMonthFieldContainer>)

    override fun year(padding: Padding) =
        addFormatStructureForYearMonth(BasicFormatStructure(YearDirective(padding)))

    override fun yearTwoDigits(baseYear: Int) =
        addFormatStructureForYearMonth(BasicFormatStructure(ReducedYearDirective(baseYear)))

    override fun monthNumber(padding: Padding) =
        addFormatStructureForYearMonth(BasicFormatStructure(MonthDirective(padding)))

    override fun monthName(names: MonthNames) =
        addFormatStructureForYearMonth(BasicFormatStructure(MonthNameDirective(names)))

    @Suppress("NO_ELSE_IN_WHEN")
    override fun yearMonth(format: DateTimeFormat<YearMonth>) = when (format) {
        is YearMonthFormat -> addFormatStructureForYearMonth(format.actualFormat)
    }
}

private val emptyIncompleteYearMonth = IncompleteYearMonth()

// these are constants so that the formats are not recreated every time they are used
internal val ISO_YEAR_MONTH by lazy {
    YearMonthFormat.build {
        year(); char('-'); monthNumber()
    }
}
