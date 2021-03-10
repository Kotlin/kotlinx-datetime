@file:JsModule("@js-joda/core")
@file:JsNonModule
@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE", "INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS", "EXTERNAL_DELEGATION")
package kotlinx.datetime.internal.JSJoda

import kotlin.js.*
import kotlin.js.Json
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

external open class TemporalAccessor {
    open fun get(field: TemporalField): Number
    open fun query(query: TemporalQuery): Any
    open fun range(field: TemporalField): ValueRange
}

external open class Temporal : TemporalAccessor

external open class Clock {
    open fun instant(): Instant
    open fun millis(): Number
    open fun zone(): ZoneId
    open fun withZone(zone: ZoneId): Clock
    open fun equals(other: Any): Boolean

    companion object {
        fun fixed(fixedInstant: Instant, zoneOffset: ZoneOffset): Clock
        fun system(zone: ZoneId): Clock
        fun systemDefaultZone(): Clock
        fun systemUTC(): Clock
    }
}

external open class DayOfWeek : Temporal {
    open fun adjustInto(temporal: TemporalAdjuster): DayOfWeek /* this */
    open fun compareTo(other: DayOfWeek): Number
    open fun equals(other: Any): Boolean
    open fun getDisplayName(style: TextStyle, locale: Locale): String
    open fun getLong(field: TemporalField): Number
    open fun isSupported(field: TemporalField): Boolean
    open fun minus(days: Number): DayOfWeek
    open fun name(): String
    open fun ordinal(): Number
    open fun plus(days: Number): DayOfWeek
    override fun toString(): String
    open fun value(): Number

    companion object {
        var MONDAY: DayOfWeek
        var TUESDAY: DayOfWeek
        var WEDNESDAY: DayOfWeek
        var THURSDAY: DayOfWeek
        var FRIDAY: DayOfWeek
        var SATURDAY: DayOfWeek
        var SUNDAY: DayOfWeek
        fun from(temporal: TemporalAccessor): DayOfWeek
        fun of(dayOfWeek: Number): DayOfWeek
        fun valueOf(name: String): DayOfWeek
        fun values(): Array<DayOfWeek>
    }
}

external open class TemporalAmount {
    open fun <T : Temporal> addTo(temporal: T): T
    open fun get(unit: TemporalUnit): Number
    open fun units(): Array<TemporalUnit>
    open fun <T : Temporal> subtractFrom(temporal: T): T
}

external open class Duration : TemporalAmount {
    open fun abs(): Duration
    override fun <T : Temporal> addTo(temporal: T): T
    open fun compareTo(otherDuration: Duration): Number
    open fun dividedBy(divisor: Number): Duration
    open fun equals(other: Any): Boolean
    override fun get(unit: TemporalUnit): Number
    open fun isNegative(): Boolean
    open fun isZero(): Boolean
    open fun minus(durationOrNumber: Duration, unit: ChronoUnit): Duration
    open fun minus(durationOrNumber: Number, unit: ChronoUnit): Duration
    open fun minusAmountUnit(amountToSubtract: Number, unit: TemporalUnit): Duration
    open fun minusDays(daysToSubtract: Number): Duration
    open fun minusDuration(duration: Duration): Duration
    open fun minusHours(hoursToSubtract: Number): Duration
    open fun minusMillis(millisToSubtract: Number): Duration
    open fun minusMinutes(minutesToSubtract: Number): Duration
    open fun minusNanos(nanosToSubtract: Number): Duration
    open fun minusSeconds(secondsToSubtract: Number): Duration
    open fun multipliedBy(multiplicand: Number): Duration
    open fun nano(): Number
    open fun negated(): Duration
    open fun plus(durationOrNumber: Duration, unitOrNumber: TemporalUnit): Duration
    open fun plus(durationOrNumber: Duration, unitOrNumber: Number): Duration
    open fun plus(durationOrNumber: Number, unitOrNumber: TemporalUnit): Duration
    open fun plus(durationOrNumber: Number, unitOrNumber: Number): Duration
    open fun plusAmountUnit(amountToAdd: Number, unit: TemporalUnit): Duration
    open fun plusDays(daysToAdd: Number): Duration
    open fun plusDuration(duration: Duration): Duration
    open fun plusHours(hoursToAdd: Number): Duration
    open fun plusMillis(millisToAdd: Number): Duration
    open fun plusMinutes(minutesToAdd: Number): Duration
    open fun plusNanos(nanosToAdd: Number): Duration
    open fun plusSeconds(secondsToAdd: Number): Duration
    open fun plusSecondsNanos(secondsToAdd: Number, nanosToAdd: Number): Duration
    open fun seconds(): Number
    override fun <T : Temporal> subtractFrom(temporal: T): T
    open fun toDays(): Number
    open fun toHours(): Number
    open fun toJSON(): String
    open fun toMillis(): Number
    open fun toMinutes(): Number
    open fun toNanos(): Number
    override fun toString(): String
    override fun units(): Array<TemporalUnit>
    open fun withNanos(nanoOfSecond: Number): Duration
    open fun withSeconds(seconds: Number): Duration

    companion object {
        var ZERO: Duration
        fun between(startInclusive: Temporal, endExclusive: Temporal): Duration
        fun from(amount: TemporalAmount): Duration
        fun of(amount: Number, unit: TemporalUnit): Duration
        fun ofDays(days: Number): Duration
        fun ofHours(hours: Number): Duration
        fun ofMillis(millis: Number): Duration
        fun ofMinutes(minutes: Number): Duration
        fun ofNanos(nanos: Number): Duration
        fun ofSeconds(seconds: Number): Duration
        fun parse(text: String): Duration
    }
}

external open class Instant : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atZone(zone: ZoneId): ZonedDateTime
    open fun compareTo(otherInstant: Instant): Number
    open fun epochSecond(): Number
    open fun equals(other: Any): Boolean
    override fun get(field: TemporalField): Number
    open fun getLong(field: TemporalField): Number
    override fun hashCode(): Number
    open fun isAfter(otherInstant: Instant): Boolean
    open fun isBefore(otherInstant: Instant): Boolean
    open fun isSupported(fieldOrUnit: TemporalField): Boolean
    open fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    open fun minus(amount: TemporalAmount): Instant
    open fun minus(amountToSubtract: Number, unit: TemporalUnit): Instant
    open fun minusMillis(millisToSubtract: Number): Instant
    open fun minusNanos(nanosToSubtract: Number): Instant
    open fun minusSeconds(secondsToSubtract: Number): Instant
    open fun nano(): Number
    open fun plus(amount: TemporalAmount): Instant
    open fun plus(amountToAdd: Number, unit: TemporalUnit): Instant
    open fun plusMillis(millisToAdd: Number): Instant
    open fun plusNanos(nanosToAdd: Number): Instant
    open fun plusSeconds(secondsToAdd: Number): Instant
    override fun query(query: TemporalQuery): Any
    override fun range(field: TemporalField): ValueRange
    open fun toEpochMilli(): Number
    open fun toJSON(): String
    override fun toString(): String
    open fun truncatedTo(unit: TemporalUnit): Instant
    open fun until(endExclusive: Temporal, unit: TemporalUnit): Number
    open fun with(adjuster: TemporalAdjuster): Instant
    open fun with(field: TemporalField, newValue: Number): Instant
    open fun withTemporalAdjuster(adjuster: TemporalAdjuster): Instant

    companion object {
        var EPOCH: Instant
        var MIN: Instant
        var MAX: Instant
        var MIN_SECONDS: Instant
        var MAX_SECONDS: Instant
        fun from(temporal: TemporalAccessor): Instant
        fun now(clock: Clock? = definedExternally): Instant
        fun ofEpochMilli(epochMilli: Number): Instant
        fun ofEpochSecond(epochSecond: Number, nanoAdjustment: Number? = definedExternally): Instant
        fun parse(text: String): Instant
    }
}

external open class ResolverStyle {
    companion object {
        var STRICT: ResolverStyle
        var SMART: ResolverStyle
        var LENIENT: ResolverStyle
    }
}

external open class SignStyle {
    companion object {
        var NORMAL: SignStyle
        var NEVER: SignStyle
        var ALWAYS: SignStyle
        var EXCEEDS_PAD: SignStyle
        var NOT_NEGATIVE: SignStyle
    }
}

external open class DateTimeFormatter {
    open fun chronology(): Any
    open fun decimalStyle(): Any
    open fun format(temporal: TemporalAccessor): String
    open fun locale(): Any
    open fun parse(text: String, type: TemporalQuery): TemporalAccessor
    open fun parse1(text: String): TemporalAccessor
    open fun parse2(text: Any, type: Any): Any
    open fun parseUnresolved(text: Any, position: Any): Any
    override fun toString(): String
    open fun withChronology(chrono: Any): Any
    open fun withLocale(locale: Locale): DateTimeFormatter
    open fun withResolverStyle(resolverStyle: ResolverStyle): DateTimeFormatter

    companion object {
        var ISO_LOCAL_DATE: DateTimeFormatter
        var ISO_LOCAL_TIME: DateTimeFormatter
        var ISO_LOCAL_DATE_TIME: DateTimeFormatter
        var ISO_INSTANT: DateTimeFormatter
        var ISO_OFFSET_DATE_TIME: DateTimeFormatter
        var ISO_ZONED_DATE_TIME: DateTimeFormatter
        fun ofPattern(pattern: String): DateTimeFormatter
        fun parsedExcessDays(): TemporalQuery
        fun parsedLeapSecond(): Boolean
    }
}

external open class DateTimeFormatterBuilder {
    open fun append(formatter: DateTimeFormatter): DateTimeFormatterBuilder
    open fun appendFraction(field: TemporalField, minWidth: Number, maxWidth: Number, decimalPoint: Boolean): DateTimeFormatterBuilder
    open fun appendInstant(fractionalDigits: Number): DateTimeFormatterBuilder
    open fun appendLiteral(literal: Any): DateTimeFormatterBuilder
    open fun appendOffset(pattern: String, noOffsetText: String): DateTimeFormatterBuilder
    open fun appendOffsetId(): DateTimeFormatterBuilder
    open fun appendPattern(pattern: String): DateTimeFormatterBuilder
    open fun appendValue(field: TemporalField, width: Number? = definedExternally, maxWidth: Number? = definedExternally, signStyle: SignStyle? = definedExternally): DateTimeFormatterBuilder
    open fun appendValueReduced(field: TemporalField, width: Number, maxWidth: Number, base: ChronoLocalDate): DateTimeFormatterBuilder
    open fun appendValueReduced(field: TemporalField, width: Number, maxWidth: Number, base: Number): DateTimeFormatterBuilder
    open fun appendZoneId(): DateTimeFormatterBuilder
    open fun optionalEnd(): DateTimeFormatterBuilder
    open fun optionalStart(): DateTimeFormatterBuilder
    open fun padNext(): DateTimeFormatterBuilder
    open fun parseCaseInsensitive(): DateTimeFormatterBuilder
    open fun parseCaseSensitive(): DateTimeFormatterBuilder
    open fun parseLenient(): DateTimeFormatterBuilder
    open fun parseStrict(): DateTimeFormatterBuilder
    open fun toFormatter(resolverStyle: ResolverStyle): DateTimeFormatter
}

external open class LocalTime : Temporal {
    open fun adjustInto(temporal: TemporalAdjuster): Temporal
    open fun atDate(date: LocalDate): LocalDateTime
    open fun compareTo(other: LocalTime): Number
    open fun equals(other: Any): Boolean
    open fun format(formatter: DateTimeFormatter): String
    open fun get(field: ChronoField): Number
    open fun getLong(field: ChronoField): Number
    override fun hashCode(): Number
    open fun hour(): Number
    open fun isAfter(other: LocalTime): Boolean
    open fun isBefore(other: LocalTime): Boolean
    open fun isSupported(fieldOrUnit: ChronoField): Boolean
    open fun isSupported(fieldOrUnit: ChronoUnit): Boolean
    open fun minus(amount: TemporalAmount): LocalTime
    open fun minus(amountToSubtract: Number, unit: ChronoUnit): LocalTime
    open fun minusHours(hoursToSubtract: Number): LocalTime
    open fun minusMinutes(minutesToSubtract: Number): LocalTime
    open fun minusNanos(nanosToSubtract: Number): LocalTime
    open fun minusSeconds(secondsToSubtract: Number): LocalTime
    open fun minute(): Number
    open fun nano(): Number
    open fun plus(amount: TemporalAmount): LocalTime
    open fun plus(amountToAdd: Number, unit: TemporalUnit): LocalTime
    open fun plusHours(hoursToAdd: Number): LocalTime
    open fun plusMinutes(minutesToAdd: Number): LocalTime
    open fun plusNanos(nanosToAdd: Number): LocalTime
    open fun plusSeconds(secondstoAdd: Number): LocalTime
    override fun query(query: TemporalQuery): Any
    open fun range(field: ChronoField): ValueRange
    open fun second(): Number
    open fun toJSON(): String
    open fun toNanoOfDay(): Number
    open fun toSecondOfDay(): Number
    override fun toString(): String
    open fun truncatedTo(unit: ChronoUnit): LocalTime
    open fun until(endExclusive: TemporalAccessor, unit: TemporalUnit): Number
    open fun with(adjuster: TemporalAdjuster): LocalTime
    open fun with(field: TemporalField, newValue: Number): LocalTime
    open fun withHour(hour: Number): LocalTime
    open fun withMinute(minute: Number): LocalTime
    open fun withNano(nanoOfSecond: Number): LocalTime
    open fun withSecond(second: Number): LocalTime
    open fun withTemporalAdjuster(adjuster: TemporalAdjuster): LocalTime

    companion object {
        var MIN: LocalTime
        var MAX: LocalTime
        var MIDNIGHT: LocalTime
        var NOON: LocalTime
        var HOURS_PER_DAY: Number
        var MINUTES_PER_HOUR: Number
        var MINUTES_PER_DAY: Number
        var SECONDS_PER_MINUTE: Number
        var SECONDS_PER_HOUR: Number
        var SECONDS_PER_DAY: Number
        var MILLIS_PER_DAY: Number
        var MICROS_PER_DAY: Number
        var NANOS_PER_SECOND: Number
        var NANOS_PER_MINUTE: Number
        var NANOS_PER_HOUR: Number
        var NANOS_PER_DAY: Number
        fun from(temporal: TemporalAccessor): LocalTime
        fun now(clockOrZone: Clock? = definedExternally): LocalTime
        fun now(clockOrZone: ZoneId? = definedExternally): LocalTime
        fun of(hour: Number? = definedExternally, minute: Number? = definedExternally, second: Number? = definedExternally, nanoOfSecond: Number? = definedExternally): LocalTime
        fun ofInstant(instant: Instant, zone: ZoneId? = definedExternally): LocalTime
        fun ofNanoOfDay(nanoOfDay: Number): LocalTime
        fun ofSecondOfDay(secondOfDay: Number? = definedExternally, nanoOfSecond: Number? = definedExternally): LocalTime
        fun parse(text: String, formatter: DateTimeFormatter? = definedExternally): LocalTime
        fun now(): LocalTime
    }
}

external open class MathUtil {
    companion object {
        fun compareNumbers(a: Number, b: Number): Number
        fun floorDiv(x: Number, y: Number): Number
        fun floorMod(x: Number, y: Number): Number
        fun intDiv(x: Number, y: Number): Number
        fun intMod(x: Number, y: Number): Number
        fun parseInt(value: Number): Number
        fun roundDown(r: Number): Number
        fun safeAdd(x: Number, y: Number): Number
        fun safeMultiply(x: Number, y: Number): Number
        fun safeSubtract(x: Number, y: Number): Number
        fun safeToInt(value: Number): Number
        fun safeZero(value: Number): Number
        fun verifyInt(value: Number)
    }
}

external open class Month : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun compareTo(other: Month): Number
    open fun equals(other: Any): Boolean
    open fun firstDayOfYear(leapYear: Boolean): Number
    open fun firstMonthOfQuarter(): Month
    override fun get(field: TemporalField): Number
    open fun getDisplayName(style: TextStyle, locale: Locale): String
    open fun getLong(field: TemporalField): Number
    open fun isSupported(field: TemporalField): Boolean
    open fun length(leapYear: Boolean): Number
    open fun maxLength(): Number
    open fun minLength(): Number
    open fun minus(months: Number): Month
    open fun name(): String
    open fun ordinal(): Number
    open fun plus(months: Number): Month
    override fun query(query: TemporalQuery): Any
    override fun toString(): String
    open fun value(): Number

    companion object {
        var JANUARY: Month
        var FEBRUARY: Month
        var MARCH: Month
        var APRIL: Month
        var MAY: Month
        var JUNE: Month
        var JULY: Month
        var AUGUST: Month
        var SEPTEMBER: Month
        var OCTOBER: Month
        var NOVEMBER: Month
        var DECEMBER: Month
        fun from(temporal: TemporalAccessor): Month
        fun of(month: Number): Month
        fun valueOf(name: String): Month
        fun values(): Array<Month>
    }
}

external open class MonthDay : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atYear(year: Number): LocalDate
    open fun compareTo(other: MonthDay): Number
    open fun dayOfMonth(): Number
    open fun equals(other: Any): Boolean
    open fun format(formatter: DateTimeFormatter): String
    override fun get(field: TemporalField): Number
    open fun getLong(field: TemporalField): Number
    open fun isAfter(other: MonthDay): Boolean
    open fun isBefore(other: MonthDay): Boolean
    open fun isSupported(field: TemporalField): Boolean
    open fun isValidYear(year: Number): Boolean
    open fun month(): Month
    open fun monthValue(): Number
    override fun query(query: TemporalQuery): Any
    override fun range(field: TemporalField): ValueRange
    override fun toString(): String
    open fun with(month: Month): MonthDay
    open fun withDayOfMonth(dayOfMonth: Number): MonthDay
    open fun withMonth(month: Number): MonthDay

    companion object {
        fun from(temporal: TemporalAccessor): MonthDay
        fun now(arg1: ZoneId? = definedExternally): MonthDay
        fun now(arg1: Clock? = definedExternally): MonthDay
        fun of(monthOrNumber: Month, number: Number? = definedExternally): MonthDay
        fun of(monthOrNumber: Number, number: Number? = definedExternally): MonthDay
        fun ofMonthNumber(month: Month, dayOfMonth: Number): MonthDay
        fun ofNumberNumber(month: Number, dayOfMonth: Number): MonthDay
        fun parse(text: String, formatter: DateTimeFormatter? = definedExternally): MonthDay
        fun parseString(text: String): MonthDay
        fun parseStringFormatter(text: String, formatter: DateTimeFormatter): MonthDay
        fun now(): MonthDay
    }
}

external open class TemporalField {
    open fun isDateBased(): Boolean
    open fun isTimeBased(): Boolean
    open fun name(): String
}

external open class ChronoField : TemporalField {
    open fun baseUnit(): Number
    open fun checkValidIntValue(value: Number): Number
    open fun checkValidValue(value: Number): Any
    open fun displayName(): String
    open fun equals(other: Any): Boolean
    open fun getFrom(temporal: TemporalAccessor): Number
    override fun isDateBased(): Boolean
    override fun isTimeBased(): Boolean
    override fun name(): String
    open fun range(): ValueRange
    open fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange
    open fun rangeUnit(): Number
    override fun toString(): String

    companion object {
        var NANO_OF_SECOND: ChronoField
        var NANO_OF_DAY: ChronoField
        var MICRO_OF_SECOND: ChronoField
        var MICRO_OF_DAY: ChronoField
        var MILLI_OF_SECOND: ChronoField
        var MILLI_OF_DAY: ChronoField
        var SECOND_OF_MINUTE: ChronoField
        var SECOND_OF_DAY: ChronoField
        var MINUTE_OF_HOUR: ChronoField
        var MINUTE_OF_DAY: ChronoField
        var HOUR_OF_AMPM: ChronoField
        var CLOCK_HOUR_OF_AMPM: ChronoField
        var HOUR_OF_DAY: ChronoField
        var CLOCK_HOUR_OF_DAY: ChronoField
        var AMPM_OF_DAY: ChronoField
        var DAY_OF_WEEK: ChronoField
        var ALIGNED_DAY_OF_WEEK_IN_MONTH: ChronoField
        var ALIGNED_DAY_OF_WEEK_IN_YEAR: ChronoField
        var DAY_OF_MONTH: ChronoField
        var DAY_OF_YEAR: ChronoField
        var EPOCH_DAY: ChronoField
        var ALIGNED_WEEK_OF_MONTH: ChronoField
        var ALIGNED_WEEK_OF_YEAR: ChronoField
        var MONTH_OF_YEAR: ChronoField
        var PROLEPTIC_MONTH: ChronoField
        var YEAR_OF_ERA: ChronoField
        var YEAR: ChronoField
        var ERA: ChronoField
        var INSTANT_SECONDS: ChronoField
        var OFFSET_SECONDS: ChronoField
    }
}

external open class TemporalUnit {
    open fun <T : Temporal> addTo(temporal: T, amount: Number): T
    open fun between(temporal1: Temporal, temporal2: Temporal): Number
    open fun duration(): Duration
    open fun isDateBased(): Boolean
    open fun isDurationEstimated(): Boolean
    open fun isSupportedBy(temporal: Temporal): Boolean
    open fun isTimeBased(): Boolean
}

external open class ChronoUnit : TemporalUnit {
    override fun <T : Temporal> addTo(temporal: T, amount: Number): T
    override fun between(temporal1: Temporal, temporal2: Temporal): Number
    open fun compareTo(other: TemporalUnit): Number
    override fun duration(): Duration
    override fun isDateBased(): Boolean
    override fun isDurationEstimated(): Boolean
    override fun isSupportedBy(temporal: Temporal): Boolean
    override fun isTimeBased(): Boolean
    override fun toString(): String

    companion object {
        var NANOS: ChronoUnit
        var MICROS: ChronoUnit
        var MILLIS: ChronoUnit
        var SECONDS: ChronoUnit
        var MINUTES: ChronoUnit
        var HOURS: ChronoUnit
        var HALF_DAYS: ChronoUnit
        var DAYS: ChronoUnit
        var WEEKS: ChronoUnit
        var MONTHS: ChronoUnit
        var YEARS: ChronoUnit
        var DECADES: ChronoUnit
        var CENTURIES: ChronoUnit
        var MILLENNIA: ChronoUnit
        var ERAS: ChronoUnit
        var FOREVER: ChronoUnit
    }
}

external open class IsoFields {
    companion object {
        var DAY_OF_QUARTER: TemporalField
        var QUARTER_OF_YEAR: TemporalField
        var WEEK_OF_WEEK_BASED_YEAR: TemporalField
        var WEEK_BASED_YEAR: TemporalField
        var WEEK_BASED_YEARS: TemporalUnit
        var QUARTER_YEARS: TemporalUnit
    }
}

external open class ChronoLocalDate : Temporal {
    open fun adjustInto(temporal: TemporalAdjuster): ChronoLocalDate /* this */
    open fun format(formatter: DateTimeFormatter): String
    open fun isSupported(fieldOrUnit: TemporalField): Boolean
    open fun isSupported(fieldOrUnit: TemporalUnit): Boolean
}

external open class LocalDate : ChronoLocalDate {
    open fun atStartOfDay(): LocalDateTime
    open fun atStartOfDay(zone: ZoneId): ZonedDateTime
    open fun atStartOfDayWithZone(zone: ZoneId): ZonedDateTime
    open fun atTime(time: LocalTime): LocalDateTime
    open fun atTime(hour: Number, minute: Number, second: Number? = definedExternally, nanoOfSecond: Number? = definedExternally): LocalDateTime
    open fun chronology(): Chronology
    open fun compareTo(other: LocalDate): Number
    open fun dayOfMonth(): Number
    open fun dayOfWeek(): DayOfWeek
    open fun dayOfYear(): Number
    open fun equals(other: Any): Boolean
    override fun get(field: TemporalField): Number
    open fun getLong(field: TemporalField): Number
    override fun hashCode(): Number
    open fun isAfter(other: LocalDate): Boolean
    open fun isBefore(other: LocalDate): Boolean
    open fun isEqual(other: LocalDate): Boolean
    open fun isLeapYear(): Boolean
    open fun isoWeekOfWeekyear(): Number
    open fun isoWeekyear(): Number
    open fun lengthOfMonth(): Number
    open fun lengthOfYear(): Number
    open fun minus(amount: TemporalAmount): LocalDate
    open fun minus(amountToSubtract: Number, unit: TemporalUnit): LocalDate
    open fun minusDays(daysToSubtract: Number): LocalDate
    open fun minusMonths(monthsToSubtract: Number): LocalDate
    open fun minusWeeks(weeksToSubtract: Number): LocalDate
    open fun minusYears(yearsToSubtract: Number): LocalDate
    open fun month(): Month
    open fun monthValue(): Number
    open fun plus(amount: TemporalAmount): LocalDate
    open fun plus(amountToAdd: Number, unit: TemporalUnit): LocalDate
    open fun plusDays(daysToAdd: Number): LocalDate
    open fun plusMonths(monthsToAdd: Number): LocalDate
    open fun plusWeeks(weeksToAdd: Number): LocalDate
    open fun plusYears(yearsToAdd: Number): LocalDate
    override fun query(query: TemporalQuery): Any
    override fun range(field: TemporalField): ValueRange
    open fun toEpochDay(): Number
    open fun toJSON(): String
    override fun toString(): String
    open fun until(endDate: TemporalAccessor): Period
    open fun until(endExclusive: TemporalAccessor, unit: TemporalUnit): Number
    open fun with(fieldOrAdjuster: TemporalField, newValue: Number): LocalDate
    open fun with(adjuster: TemporalAdjuster): LocalDate
    open fun withDayOfMonth(dayOfMonth: Number): LocalDate
    open fun withDayOfYear(dayOfYear: Number): LocalDate
    open fun withFieldAndValue(field: TemporalField, newValue: Number): LocalDate
    open fun withMonth(month: Month): LocalDate
    open fun withMonth(month: Number): LocalDate
    open fun withTemporalAdjuster(adjuster: TemporalAdjuster): LocalDate
    open fun withYear(year: Number): LocalDate
    open fun year(): Number

    companion object {
        var MIN: LocalDate
        var MAX: LocalDate
        var EPOCH_0: LocalDate
        fun from(temporal: TemporalAccessor): LocalDate
        fun now(clockOrZone: Clock? = definedExternally): LocalDate
        fun now(clockOrZone: ZoneId? = definedExternally): LocalDate
        fun of(year: Number, month: Month, dayOfMonth: Number): LocalDate
        fun of(year: Number, month: Number, dayOfMonth: Number): LocalDate
        fun ofEpochDay(epochDay: Number): LocalDate
        fun ofInstant(instant: Instant, zoneId: ZoneId? = definedExternally): LocalDate
        fun ofYearDay(year: Number, dayOfYear: Number): LocalDate
        fun parse(text: String, formatter: DateTimeFormatter? = definedExternally): LocalDate
        fun now(): LocalDate
    }
}

external open class ChronoLocalDateTime : Temporal {
    open fun adjustInto(temporal: Any): Any
    open fun chronology(): Chronology
    open fun toEpochSecond(offset: ZoneOffset): Number
    open fun toInstant(offset: ZoneOffset): Instant
}

external open class LocalDateTime : ChronoLocalDateTime {
    open fun adjustInto(temporal: TemporalAdjuster): LocalDateTime
    open fun atZone(zone: ZoneId): ZonedDateTime
    open fun compareTo(other: LocalDateTime): Number
    open fun dayOfMonth(): Number
    open fun dayOfWeek(): DayOfWeek
    open fun dayOfYear(): Number
    open fun equals(other: Any): Boolean
    open fun format(formatter: DateTimeFormatter): String
    override fun get(field: TemporalField): Number
    open fun getLong(field: TemporalField): Number
    override fun hashCode(): Number
    open fun hour(): Number
    open fun isAfter(other: LocalDateTime): Boolean
    open fun isBefore(other: LocalDateTime): Boolean
    open fun isEqual(other: LocalDateTime): Boolean
    open fun isSupported(fieldOrUnit: TemporalField): Boolean
    open fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    open fun minus(amount: TemporalAmount): LocalDateTime
    open fun minus(amountToSubtract: Number, unit: TemporalUnit): LocalDateTime
    open fun minusDays(days: Number): LocalDateTime
    open fun minusHours(hours: Number): LocalDateTime
    open fun minusMinutes(minutes: Number): LocalDateTime
    open fun minusMonths(months: Number): LocalDateTime
    open fun minusNanos(nanos: Number): LocalDateTime
    open fun minusSeconds(seconds: Number): LocalDateTime
    open fun minusTemporalAmount(amount: TemporalAmount): LocalDateTime
    open fun minusWeeks(weeks: Number): LocalDateTime
    open fun minusYears(years: Number): LocalDateTime
    open fun minute(): Number
    open fun month(): Month
    open fun monthValue(): Number
    open fun nano(): Number
    open fun plus(amount: TemporalAmount): LocalDateTime
    open fun plus(amountToAdd: Number, unit: TemporalUnit): LocalDateTime
    open fun plusDays(days: Number): LocalDateTime
    open fun plusHours(hours: Number): LocalDateTime
    open fun plusMinutes(minutes: Number): LocalDateTime
    open fun plusMonths(months: Number): LocalDateTime
    open fun plusNanos(nanos: Number): LocalDateTime
    open fun plusSeconds(seconds: Number): LocalDateTime
    open fun plusTemporalAmount(amount: TemporalAmount): LocalDateTime
    open fun plusWeeks(weeks: Number): LocalDateTime
    open fun plusYears(years: Number): LocalDateTime
    override fun query(query: TemporalQuery): Any
    override fun range(field: TemporalField): ValueRange
    open fun second(): Number
    open fun toJSON(): String
    open fun toLocalDate(): LocalDate
    open fun toLocalTime(): LocalTime
    override fun toString(): String
    open fun truncatedTo(unit: TemporalUnit): LocalDateTime
    open fun until(endExclusive: Temporal, unit: TemporalUnit): Number
    open fun with(adjuster: TemporalAdjuster): LocalDateTime
    open fun with(field: TemporalField, newValue: Number): LocalDateTime
    open fun withDayOfMonth(dayOfMonth: Number): LocalDateTime
    open fun withDayOfYear(dayOfYear: Number): LocalDateTime
    open fun withHour(hour: Number): LocalDateTime
    open fun withMinute(minute: Number): LocalDateTime
    open fun withMonth(month: Number): LocalDateTime
    open fun withMonth(month: Month): LocalDateTime
    open fun withNano(nanoOfSecond: Number): LocalDateTime
    open fun withSecond(second: Number): LocalDateTime
    open fun withTemporalAdjuster(adjuster: TemporalAdjuster): LocalDateTime
    open fun withYear(year: Number): LocalDateTime
    open fun year(): Number

    companion object {
        var MIN: LocalDateTime
        var MAX: LocalDateTime
        fun from(temporal: TemporalAccessor): LocalDateTime
        fun now(clockOrZone: Clock? = definedExternally): LocalDateTime
        fun now(clockOrZone: ZoneId? = definedExternally): LocalDateTime
        fun of(date: LocalDate, time: LocalTime): LocalDateTime
        fun of(year: Number, month: Month, dayOfMonth: Number, hour: Number? = definedExternally, minute: Number? = definedExternally, second: Number? = definedExternally, nanoSecond: Number? = definedExternally): LocalDateTime
        fun of(year: Number, month: Number, dayOfMonth: Number, hour: Number? = definedExternally, minute: Number? = definedExternally, second: Number? = definedExternally, nanoSecond: Number? = definedExternally): LocalDateTime
        fun ofEpochSecond(epochSecond: Number, offset: ZoneOffset): LocalDateTime
        fun ofEpochSecond(epochSecond: Number, nanoOfSecond: Number, offset: ZoneOffset): LocalDateTime
        fun ofInstant(instant: Instant, zoneId: ZoneId? = definedExternally): LocalDateTime
        fun parse(text: String, formatter: DateTimeFormatter? = definedExternally): LocalDateTime
        fun now(): LocalDateTime
    }
}

external open class Period : TemporalAmount {
    override fun <T : Temporal> addTo(temporal: T): T
    open fun chronology(): IsoChronology
    open fun days(): Number
    open fun equals(other: Any): Boolean
    override fun get(unit: TemporalUnit): Number
    override fun hashCode(): Number
    open fun isNegative(): Boolean
    open fun isZero(): Boolean
    open fun minus(amountToSubtract: TemporalAmount): Period
    open fun minusDays(daysToSubtract: Number): Period
    open fun minusMonths(monthsToSubtract: Number): Period
    open fun minusYears(yearsToSubtract: Number): Period
    open fun months(): Number
    open fun multipliedBy(scalar: Number): Period
    open fun negated(): Period
    open fun normalized(): Period
    open fun plus(amountToAdd: TemporalAmount): Period
    open fun plusDays(daysToAdd: Number): Period
    open fun plusMonths(monthsToAdd: Number): Period
    open fun plusYears(yearsToAdd: Number): Period
    override fun <T : Temporal> subtractFrom(temporal: T): T
    open fun toJSON(): String
    override fun toString(): String
    open fun toTotalMonths(): Number
    override fun units(): Array<TemporalUnit>
    open fun withDays(days: Number): Period
    open fun withMonths(months: Number): Period
    open fun withYears(years: Number): Period
    open fun years(): Number

    companion object {
        var ZERO: Period
        fun between(startDate: LocalDate, endDate: LocalDate): Period
        fun create(years: Number, months: Number, days: Number): Duration
        fun from(amount: TemporalAmount): Period
        fun of(years: Number, months: Number, days: Number): Period
        fun ofDays(days: Number): Period
        fun ofMonths(months: Number): Period
        fun ofWeeks(weeks: Number): Period
        fun ofYears(years: Number): Period
        fun parse(text: String): Period
    }
}

external open class TemporalAdjuster {
    open fun adjustInto(temporal: Temporal): Temporal
}

external open class TemporalAdjusters {
    companion object {
        fun dayOfWeekInMonth(ordinal: Number, dayOfWeek: DayOfWeek): TemporalAdjuster
        fun firstDayOfMonth(): TemporalAdjuster
        fun firstDayOfNextMonth(): TemporalAdjuster
        fun firstDayOfNextYear(): TemporalAdjuster
        fun firstDayOfYear(): TemporalAdjuster
        fun firstInMonth(dayOfWeek: DayOfWeek): TemporalAdjuster
        fun lastDayOfMonth(): TemporalAdjuster
        fun lastDayOfYear(): TemporalAdjuster
        fun lastInMonth(dayOfWeek: DayOfWeek): TemporalAdjuster
        fun next(dayOfWeek: DayOfWeek): TemporalAdjuster
        fun nextOrSame(dayOfWeek: DayOfWeek): TemporalAdjuster
        fun previous(dayOfWeek: DayOfWeek): TemporalAdjuster
        fun previousOrSame(dayOfWeek: DayOfWeek): TemporalAdjuster
    }
}

external open class TemporalQueries {
    companion object {
        fun chronology(): TemporalQuery
        fun localDate(): TemporalQuery
        fun localTime(): TemporalQuery
        fun offset(): TemporalQuery
        fun precision(): TemporalQuery
        fun zone(): TemporalQuery
        fun zoneId(): TemporalQuery
    }
}

external open class TemporalQuery {
    open fun queryFrom(temporal: TemporalAccessor): Any
}

external open class ValueRange {
    open fun checkValidIntValue(value: Number, field: TemporalField): Number
    open fun checkValidValue(value: Number, field: TemporalField): Any
    open fun equals(other: Any): Boolean
    override fun hashCode(): Number
    open fun isFixed(): Boolean
    open fun isIntValue(): Boolean
    open fun isValidIntValue(value: Number): Boolean
    open fun isValidValue(value: Any): Boolean
    open fun largestMinimum(): Number
    open fun maximum(): Number
    open fun minimum(): Number
    open fun smallestMaximum(): Number
    override fun toString(): String

    companion object {
        fun of(min: Number, max: Number): ValueRange
        fun of(min: Number, maxSmallest: Number, maxLargest: Number): ValueRange
        fun of(minSmallest: Number, minLargest: Number, maxSmallest: Number, maxLargest: Number): ValueRange
    }
}

external open class Year : Temporal {
    open fun atDay(dayOfYear: Number): LocalDate
    open fun atMonth(month: Month): YearMonth
    open fun atMonth(month: Number): YearMonth
    open fun atMonthDay(monthDay: MonthDay): LocalDate
    open fun compareTo(other: Year): Number
    open fun equals(other: Any): Boolean
    open fun isAfter(other: Year): Boolean
    open fun isBefore(other: Year): Boolean
    open fun isLeap(): Boolean
    open fun isValidMonthDay(monthDay: MonthDay): Boolean
    open fun length(): Number
    open fun plus(amount: TemporalAmount): Year
    open fun plus(amountToAdd: Number, unit: TemporalUnit): Year
    open fun plusYears(yearsToAdd: Number): Year
    open fun minus(amount: TemporalAmount): Year
    open fun minus(amountToSubtract: Number, unit: TemporalUnit): Year
    open fun minusYears(yearsToSubtract: Number): Year
    open fun value(): Number
    open fun with(adjuster: TemporalAdjuster): Year
    open fun with(field: TemporalField, newValue: Number): Year

    companion object {
        var MIN_VALUE: Number
        var MAX_VALUE: Number
        fun from(temporal: TemporalAccessor): Year
        fun isLeap(year: Number): Boolean
        fun now(zoneIdOrClock: ZoneId? = definedExternally): Year
        fun now(zoneIdOrClock: Clock? = definedExternally): Year
        fun of(isoYear: Number): Year
        fun parse(text: String, formatter: DateTimeFormatter? = definedExternally): Year
        fun now(): Year
    }
}

external open class YearMonth : Temporal {
    open fun minus(amount: TemporalAmount): YearMonth
    open fun minus(amountToSubtract: Number, unit: TemporalUnit): YearMonth
    open fun minusYears(yearsToSubtract: Number): YearMonth
    open fun minusMonths(monthsToSubtract: Number): YearMonth
    open fun plus(amount: TemporalAmount): YearMonth
    open fun plus(amountToAdd: Number, unit: TemporalUnit): YearMonth
    open fun plusYears(yearsToAdd: Number): YearMonth
    open fun plusMonths(monthsToAdd: Number): YearMonth
    open fun with(adjuster: TemporalAdjuster): YearMonth
    open fun with(field: TemporalField, value: Number): YearMonth
    open fun withYearMonth(newYear: Number, newMonth: Number): YearMonth
    open fun withYear(year: Number): YearMonth
    open fun withMonth(month: Number): YearMonth
    open fun isSupported(fieldOrUnit: TemporalField): Boolean
    open fun isSupported(fieldOrUnit: ChronoUnit): Boolean
    open fun year(): Number
    open fun monthValue(): Number
    open fun month(): Month
    open fun isLeapYear(): Boolean
    open fun isValidDay(): Boolean
    open fun lengthOfMonth(): Number
    open fun lengthOfYear(): Number
    open fun atDay(dayOfMonth: Number): LocalDate
    open fun atEndOfMonth(): LocalDate
    open fun compareTo(other: YearMonth): Number
    open fun isAfter(other: YearMonth): Boolean
    open fun isBefore(other: YearMonth): Boolean
    open fun equals(other: Any): Boolean
    open fun toJSON(): String
    open fun format(formatter: DateTimeFormatter): String

    companion object {
        fun from(temporal: TemporalAccessor): YearMonth
        fun now(zoneIdOrClock: ZoneId? = definedExternally): YearMonth
        fun now(zoneIdOrClock: Clock? = definedExternally): YearMonth
        fun of(year: Number, monthOrNumber: Month): YearMonth
        fun of(year: Number, monthOrNumber: Number): YearMonth
        fun parse(text: String, formatter: DateTimeFormatter? = definedExternally): YearMonth
        fun now(): YearMonth
    }
}

external open class ZoneId {
    open fun equals(other: Any): Boolean
    override fun hashCode(): Number
    open fun id(): String
    open fun normalized(): ZoneId
    open fun rules(): ZoneRules
    override fun toString(): String

    companion object {
        var SYSTEM: ZoneId
        var UTC: ZoneId
        fun systemDefault(): ZoneId
        fun of(zoneId: String): ZoneId
        fun ofOffset(prefix: String, offset: ZoneOffset): ZoneId
        fun from(temporal: TemporalAccessor): ZoneId
        fun getAvailableZoneIds(): Array<String>
    }
}

external open class ZoneOffset : ZoneId {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun compareTo(other: ZoneOffset): Number
    override fun equals(other: Any): Boolean
    open fun get(field: TemporalField): Number
    open fun getLong(field: TemporalField): Number
    override fun hashCode(): Number
    override fun id(): String
    open fun query(query: TemporalQuery): Any
    override fun rules(): ZoneRules
    override fun toString(): String
    open fun totalSeconds(): Number

    companion object {
        var MAX_SECONDS: ZoneOffset
        var UTC: ZoneOffset
        var MIN: ZoneOffset
        var MAX: ZoneOffset
        fun of(offsetId: String): ZoneOffset
        fun ofHours(hours: Number): ZoneOffset
        fun ofHoursMinutes(hours: Number, minutes: Number): ZoneOffset
        fun ofHoursMinutesSeconds(hours: Number, minutes: Number, seconds: Number): ZoneOffset
        fun ofTotalMinutes(totalMinutes: Number): ZoneOffset
        fun ofTotalSeconds(totalSeconds: Number): ZoneOffset
    }
}

external open class ZoneRegion : ZoneId {
    override fun id(): String
    override fun rules(): ZoneRules

    companion object {
        fun ofId(zoneId: String): ZoneId
    }
}

external open class ZoneOffsetTransition {
    open fun instant(): Instant
    open fun toEpochSecond(): Number
    open fun dateTimeBefore(): LocalDateTime
    open fun dateTimeAfter(): LocalDateTime
    open fun offsetBefore(): ZoneOffset
    open fun offsetAfter(): ZoneOffset
    open fun duration(): Duration
    open fun durationSeconds(): Number
    open fun isGap(): Boolean
    open fun isOverlap(): Boolean
    open fun isValidOffset(offset: ZoneOffset): Boolean
    open fun validOffsets(): Array<ZoneOffset>
    open fun compareTo(transition: ZoneOffsetTransition): Number
    open fun equals(other: Any): Boolean
    override fun hashCode(): Number
    override fun toString(): String

    companion object {
        fun of(transition: LocalDateTime, offsetBefore: ZoneOffset, offsetAfter: ZoneOffset): ZoneOffsetTransition
    }
}

external interface ZoneOffsetTransitionRule

external open class ZoneRules {
    open fun isFixedOffset(): Boolean
    open fun offset(instantOrLocalDateTime: Instant): ZoneOffset
    open fun offset(instantOrLocalDateTime: LocalDateTime): ZoneOffset
    open fun toJSON(): String
    open fun offsetOfEpochMilli(epochMilli: Number): ZoneOffset
    open fun offsetOfInstant(instant: Instant): ZoneOffset
    open fun offsetOfLocalDateTime(localDateTime: LocalDateTime): ZoneOffset
    open fun validOffsets(localDateTime: LocalDateTime): Array<ZoneOffset>
    open fun transition(localDateTime: LocalDateTime): ZoneOffsetTransition
    open fun standardOffset(instant: Instant): ZoneOffset
    open fun daylightSavings(instant: Instant): Duration
    open fun isDaylightSavings(instant: Instant): Boolean
    open fun isValidOffset(localDateTime: LocalDateTime, offset: ZoneOffset): Boolean
    open fun nextTransition(instant: Instant): ZoneOffsetTransition
    open fun previousTransition(instant: Instant): ZoneOffsetTransition
    open fun transitions(): Array<ZoneOffsetTransition>
    open fun transitionRules(): Array<ZoneOffsetTransitionRule>
    override fun toString(): String

    companion object {
        fun of(offest: ZoneOffset): ZoneRules
    }
}

external open class ChronoZonedDateTime : Temporal {
    open fun compareTo(other: ChronoZonedDateTime): Number
    open fun equals(other: Any): Boolean
    open fun format(formatter: DateTimeFormatter): String
    open fun isAfter(other: ChronoZonedDateTime): Boolean
    open fun isBefore(other: ChronoZonedDateTime): Boolean
    open fun isEqual(other: ChronoZonedDateTime): Boolean
    open fun query(query: Any): Any
    open fun toEpochSecond(): Number
    open fun toInstant(): Instant
}

external open class ZonedDateTime : ChronoZonedDateTime {
    open fun dayOfMonth(): Number
    open fun dayOfWeek(): DayOfWeek
    open fun dayOfYear(): Number
    override fun equals(other: Any): Boolean
    override fun format(formatter: DateTimeFormatter): String
    override fun get(field: TemporalField): Number
    open fun getLong(field: TemporalField): Number
    override fun hashCode(): Number
    open fun hour(): Number
    open fun isSupported(fieldOrUnit: TemporalField): Boolean
    open fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    open fun minus(): Any
    open fun minus(amountToSubtract: Number, unit: TemporalUnit): ZonedDateTime
    open fun minusDays(days: Number): ZonedDateTime
    open fun minusHours(hours: Number): ZonedDateTime
    open fun minusMinutes(minutes: Number): ZonedDateTime
    open fun minusMonths(months: Number): ZonedDateTime
    open fun minusNanos(nanos: Number): ZonedDateTime
    open fun minusSeconds(seconds: Number): ZonedDateTime
    open fun minusTemporalAmount(amount: TemporalAmount): ZonedDateTime
    open fun minusWeeks(weeks: Number): ZonedDateTime
    open fun minusYears(years: Number): ZonedDateTime
    open fun minute(): Number
    open fun month(): Month
    open fun monthValue(): Number
    open fun nano(): Number
    open fun offset(): Any
    open fun plus(): Any
    open fun plus(amountToAdd: Number, unit: TemporalUnit): ZonedDateTime
    open fun plusDays(days: Number): Any
    open fun plusHours(hours: Number): ZonedDateTime
    open fun plusMinutes(minutes: Number): ZonedDateTime
    open fun plusMonths(months: Number): ZonedDateTime
    open fun plusNanos(nanos: Number): ZonedDateTime
    open fun plusSeconds(seconds: Number): ZonedDateTime
    open fun plusTemporalAmount(amount: TemporalAmount): ZonedDateTime
    open fun plusWeeks(weeks: Number): Any
    open fun plusYears(years: Number): ZonedDateTime
    override fun query(query: TemporalQuery): Any
    override fun range(field: TemporalField): ValueRange
    open fun second(): Number
    open fun toJSON(): String
    open fun toLocalDate(): LocalDate
    open fun toLocalDateTime(): LocalDateTime
    open fun toLocalTime(): LocalTime
    override fun toString(): String
    open fun truncatedTo(unit: TemporalUnit): ZonedDateTime
    open fun until(endExclusive: Temporal, unit: TemporalUnit): Number
    open fun with(): Any
    open fun with(field: TemporalField, newValue: Number): ZonedDateTime
    open fun withDayOfMonth(dayOfMonth: Number): ZonedDateTime
    open fun withDayOfYear(dayOfYear: Number): ZonedDateTime
    open fun withFixedOffsetZone(): ZonedDateTime
    open fun withHour(hour: Number): ZonedDateTime
    open fun withMinute(minute: Number): ZonedDateTime
    open fun withMonth(month: Number): ZonedDateTime
    open fun withNano(nanoOfSecond: Number): ZonedDateTime
    open fun withSecond(second: Number): ZonedDateTime
    open fun withTemporalAdjuster(adjuster: TemporalAdjuster): ZonedDateTime
    open fun withYear(year: Number): ZonedDateTime
    open fun withZoneSameInstant(zone: ZoneId): ZonedDateTime
    open fun withZoneSameLocal(zone: ZoneId): ZonedDateTime
    open fun year(): Number
    open fun zone(): ZoneId

    companion object {
        fun from(temporal: TemporalAccessor): ZonedDateTime
        fun now(clockOrZone: Clock? = definedExternally): ZonedDateTime
        fun now(clockOrZone: ZoneId? = definedExternally): ZonedDateTime
        fun of(): Any
        fun of(localDateTime: LocalDateTime, zone: ZoneId): ZonedDateTime
        fun of(date: LocalDate, time: LocalTime, zone: ZoneId): ZonedDateTime
        fun of(year: Number, month: Number, dayOfMonth: Number, hour: Number, minute: Number, second: Number, nanoOfSecond: Number, zone: ZoneId): ZonedDateTime
        fun ofInstant(): ZonedDateTime
        fun ofInstant(instant: Instant, zone: ZoneId): ZonedDateTime
        fun ofInstant(localDateTime: LocalDateTime, offset: ZoneOffset, zone: ZoneId): ZonedDateTime
        fun ofLocal(localDateTime: LocalDateTime, zone: ZoneId, preferredOffset: ZoneOffset?): ZonedDateTime
        fun ofStrict(localDateTime: LocalDateTime, offset: ZoneOffset, zone: ZoneId): ZonedDateTime
        fun parse(text: String, formatter: DateTimeFormatter? = definedExternally): ZonedDateTime
        fun now(): ZonedDateTime
    }
}

external open class TextStyle {
    open fun asNormal(): TextStyle
    open fun asStandalone(): TextStyle
    open fun isStandalone(): Boolean

    companion object {
        var FULL: TextStyle
        var FULL_STANDALONE: TextStyle
        var SHORT: TextStyle
        var SHORT_STANDALONE: TextStyle
        var NARROW: TextStyle
        var NARROW_STANDALONE: TextStyle
    }
}

external interface Locale

external open class IsoChronology {
    open fun resolveDate(fieldValues: Any, resolverStyle: Any): Any
    open fun equals(other: Any): Boolean
    override fun toString(): String

    companion object {
        fun isLeapYear(prolepticYear: Number): Boolean
    }
}

external fun nativeJs(date: Date, zone: ZoneId? = definedExternally): TemporalAccessor

external fun nativeJs(date: Any, zone: ZoneId? = definedExternally): TemporalAccessor

external interface `T$0` {
    var toDate: () -> Date
    var toEpochMilli: () -> Number
}

external fun convert(temporal: LocalDate, zone: ZoneId? = definedExternally): `T$0`

external fun convert(temporal: LocalDateTime, zone: ZoneId? = definedExternally): `T$0`

external fun convert(temporal: ZonedDateTime, zone: ZoneId? = definedExternally): `T$0`

external fun use(plugin: Function<*>): Any

external open class ZoneRulesProvider {
    companion object {
        fun getRules(zoneId: String): ZoneRules
        fun getAvailableZoneIds(): Array<String>
    }
}