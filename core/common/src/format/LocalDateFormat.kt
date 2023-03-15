/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.parser.*

public interface DateFormatBuilderFields {
    public fun appendYear(minDigits: Int = 1, outputPlusOnExceededPadding: Boolean = false)
    public fun appendMonthNumber(minLength: Int = 1)
    public fun appendMonthName(names: List<String>)
    public fun appendDayOfMonth(minLength: Int = 1)
}

@DateTimeBuilder
public interface DateFormatBuilder : DateFormatBuilderFields, FormatBuilder<DateFormatBuilder>

public class LocalDateFormat private constructor(private val actualFormat: Format<DateFieldContainer>) {
    public companion object {
        public fun build(block: DateFormatBuilder.() -> Unit): LocalDateFormat {
            val builder = Builder(AppendableFormatStructure(DateFormatBuilderSpec))
            builder.block()
            return LocalDateFormat(builder.build())
        }

        public fun fromFormatString(formatString: String): LocalDateFormat = build { appendFormatString(formatString) }

        public val ISO: LocalDateFormat = build {
            appendYear(4, outputPlusOnExceededPadding = true)
            appendFormatString("'-'mm'-'dd")
        }

        internal val Cache = LruCache<String, LocalDateFormat>(16) { fromFormatString(it) }
    }

    public fun format(date: LocalDate): String =
        StringBuilder().also {
            actualFormat.formatter.format(date.toIncompleteLocalDate(), it)
        }.toString()

    public fun parse(input: String): LocalDate {
        val parser = Parser(::IncompleteLocalDate, IncompleteLocalDate::copy, actualFormat.parser)
        try {
            return parser.match(input).toLocalDate()
        } catch (e: ParseException) {
            throw DateTimeFormatException("Failed to parse date from '$input'", e)
        } catch (e: IllegalArgumentException) {
            throw DateTimeFormatException("Invalid date '$input'", e)
        }
    }

    private class Builder(override val actualBuilder: AppendableFormatStructure<DateFieldContainer>) :
        AbstractFormatBuilder<DateFieldContainer, DateFormatBuilder, Builder>, DateFormatBuilder {
        override fun appendYear(minDigits: Int, outputPlusOnExceededPadding: Boolean) =
            actualBuilder.add(BasicFormatStructure(YearDirective(minDigits, outputPlusOnExceededPadding)))

        override fun appendMonthNumber(minLength: Int) =
            actualBuilder.add(BasicFormatStructure(MonthDirective(minLength)))

        override fun appendMonthName(names: List<String>) =
            actualBuilder.add(BasicFormatStructure(MonthNameDirective(names)))

        override fun appendDayOfMonth(minLength: Int) = actualBuilder.add(BasicFormatStructure(DayDirective(minLength)))

        override fun createEmpty(): Builder = Builder(actualBuilder.createSibling())
        override fun castToGeneric(actualSelf: Builder): DateFormatBuilder = this
    }

}

public fun LocalDate.format(formatString: String): String =
    LocalDateFormat.Cache.get(formatString).format(this)

public fun LocalDate.format(format: LocalDateFormat): String = format.format(this)

public fun LocalDate.Companion.parse(input: String, formatString: String): LocalDate =
    LocalDateFormat.Cache.get(formatString).parse(input)

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

    override fun toString(): String =
        "${year ?: "??"}-${monthNumber ?: "??"}-${dayOfMonth ?: "??"} (day of week is ${isoDayOfWeek ?: "??"})"
}

internal class YearDirective(digits: Int, outputPlusOnExceededPadding: Boolean) :
    SignedIntFieldFormatDirective<DateFieldContainer>(
        DateFields.year,
        minDigits = digits,
        maxDigits = null,
        outputPlusOnExceededPadding = outputPlusOnExceededPadding,
    )

internal class MonthDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.month, minDigits)

internal class MonthNameDirective(names: List<String>) :
    NamedUnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.month, names)

internal class DayDirective(minDigits: Int) :
    UnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.dayOfMonth, minDigits)

internal object DateFormatBuilderSpec: BuilderSpec<DateFieldContainer>(
    mapOf(
        "ld" to DateFormatBuilderSpec
    ),
    mapOf(
        'y' to { length -> BasicFormatStructure(YearDirective(length, outputPlusOnExceededPadding = false)) },
        'm' to { length -> BasicFormatStructure(MonthDirective(length)) },
        'd' to { length -> BasicFormatStructure(DayDirective(length)) },
    )
) {
    const val name = "ld"
}
