@file:JsModule("@js-joda/core")
@file:kotlinx.datetime.JsNonModule
@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE", "INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
package kotlinx.datetime.internal.JSJoda

import kotlinx.datetime.InteropInterface
import kotlin.js.*
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

external open class TemporalField : InteropInterface {
    open fun isSupportedBy(temporal: TemporalAccessor): Boolean
    open fun isDateBased(): Boolean
    open fun isTimeBased(): Boolean
    open fun baseUnit(): TemporalUnit
    open fun rangeUnit(): TemporalUnit
    open fun range(): ValueRange
    open fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange
    open fun getFrom(temporal: TemporalAccessor): Int
    open fun <R : Temporal> adjustInto(temporal: R, newValue: Int): R
    open fun name(): String
    open fun displayName(): String
    open fun equals(other: InteropInterface): Boolean
}

external open class TemporalUnit : InteropInterface {
    open fun <T : Temporal> addTo(temporal: T, amount: Int): T
    open fun between(temporal1: Temporal, temporal2: Temporal): Int
    open fun duration(): Duration
    open fun isDateBased(): Boolean
    open fun isDurationEstimated(): Boolean
    open fun isSupportedBy(temporal: Temporal): Boolean
    open fun isTimeBased(): Boolean
}

external open class ValueRange : InteropInterface {
    open fun checkValidValue(value: Int, field: TemporalField): Int
    open fun checkValidIntValue(value: Int, field: TemporalField): Int
    open fun equals(other: InteropInterface): Boolean
    override fun hashCode(): Int
    open fun isFixed(): Boolean
    open fun isIntValue(): Boolean
    open fun isValidIntValue(value: Int): Boolean
    open fun isValidValue(value: Int): Boolean
    open fun largestMinimum(): Int
    open fun maximum(): Int
    open fun minimum(): Int
    open fun smallestMaximum(): Int
    override fun toString(): String

    companion object {
        fun of(min: Int, max: Int): ValueRange
        fun of(min: Int, maxSmallest: Int, maxLargest: Int): ValueRange
        fun of(minSmallest: Int, minLargest: Int, maxSmallest: Int, maxLargest: Int): ValueRange
    }
}

external open class TemporalAmount : InteropInterface {
    open fun <T : Temporal> addTo(temporal: T): T
    open fun get(unit: TemporalUnit): Int
//    open fun units(): JsArray<TemporalUnit>
    open fun <T : Temporal> subtractFrom(temporal: T): T
}

external open class TemporalAccessor : InteropInterface {
    open fun get(field: TemporalField): Int
    open fun <R : InteropInterface> query(query: TemporalQuery<R>): R?
    open fun range(field: TemporalField): ValueRange
    open fun getLong(field: TemporalField): Long
    open fun isSupported(field: TemporalField): Boolean
}

external open class Temporal : TemporalAccessor {
    override fun isSupported(field: TemporalField): Boolean
    open fun isSupported(unit: TemporalUnit): Boolean
    open fun minus(amountToSubtract: Int, unit: TemporalUnit): Temporal
    open fun minus(amount: TemporalAmount): Temporal
    open fun plus(amountToAdd: Int, unit: TemporalUnit): Temporal
    open fun plus(amount: TemporalAmount): Temporal
    open fun until(endTemporal: Temporal, unit: TemporalUnit): Int
    open fun with(adjuster: TemporalAdjuster): Temporal
    open fun with(field: TemporalField, newValue: Int): Temporal
}

external open class TemporalAdjuster : InteropInterface {
    open fun adjustInto(temporal: Temporal): Temporal
}

external open class TemporalQuery<R : InteropInterface> : InteropInterface {
    open fun queryFrom(temporal: TemporalAccessor): R
}

external open class ChronoField : TemporalField {
    override fun isSupportedBy(temporal: TemporalAccessor): Boolean
    override fun baseUnit(): TemporalUnit
    open fun checkValidValue(value: Int): Int
    open fun checkValidIntValue(value: Int): Int
    override fun displayName(): String
    override fun equals(other: InteropInterface): Boolean
    override fun getFrom(temporal: TemporalAccessor): Int
    override fun isDateBased(): Boolean
    override fun isTimeBased(): Boolean
    override fun name(): String
    override fun range(): ValueRange
    override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange
    override fun rangeUnit(): TemporalUnit
    override fun <R : Temporal> adjustInto(temporal: R, newValue: Int): R
    override fun toString(): String

    companion object {
        var ALIGNED_DAY_OF_WEEK_IN_MONTH: ChronoField
        var ALIGNED_DAY_OF_WEEK_IN_YEAR: ChronoField
        var ALIGNED_WEEK_OF_MONTH: ChronoField
        var ALIGNED_WEEK_OF_YEAR: ChronoField
        var AMPM_OF_DAY: ChronoField
        var CLOCK_HOUR_OF_AMPM: ChronoField
        var CLOCK_HOUR_OF_DAY: ChronoField
        var DAY_OF_MONTH: ChronoField
        var DAY_OF_WEEK: ChronoField
        var DAY_OF_YEAR: ChronoField
        var EPOCH_DAY: ChronoField
        var ERA: ChronoField
        var HOUR_OF_AMPM: ChronoField
        var HOUR_OF_DAY: ChronoField
        var INSTANT_SECONDS: ChronoField
        var MICRO_OF_DAY: ChronoField
        var MICRO_OF_SECOND: ChronoField
        var MILLI_OF_DAY: ChronoField
        var MILLI_OF_SECOND: ChronoField
        var MINUTE_OF_DAY: ChronoField
        var MINUTE_OF_HOUR: ChronoField
        var MONTH_OF_YEAR: ChronoField
        var NANO_OF_DAY: ChronoField
        var NANO_OF_SECOND: ChronoField
        var OFFSET_SECONDS: ChronoField
        var PROLEPTIC_MONTH: ChronoField
        var SECOND_OF_DAY: ChronoField
        var SECOND_OF_MINUTE: ChronoField
        var YEAR: ChronoField
        var YEAR_OF_ERA: ChronoField
    }
}

external open class ChronoUnit : TemporalUnit {
    override fun <T : Temporal> addTo(temporal: T, amount: Int): T
    override fun between(temporal1: Temporal, temporal2: Temporal): Int
    open fun compareTo(other: TemporalUnit): Int
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

external open class Clock : InteropInterface {
    open fun equals(other: InteropInterface): Boolean
    open fun instant(): Instant
    open fun millis(): Double
    open fun withZone(zone: ZoneId): Clock
    open fun zone(): ZoneId

    companion object {
        fun fixed(fixedInstant: Instant, zoneId: ZoneId): Clock
        fun offset(baseClock: Clock, offsetDuration: Duration): Clock
        fun system(zone: ZoneId): Clock
        fun systemDefaultZone(): Clock
        fun systemUTC(): Clock
    }
}

external open class Duration : TemporalAmount {
    open fun abs(): Duration
    override fun <T : Temporal> addTo(temporal: T): T
    open fun compareTo(otherDuration: Duration): Int
    open fun dividedBy(divisor: Int): Duration
    open fun equals(other: InteropInterface): Boolean
    override fun get(unit: TemporalUnit): Int
    open fun isNegative(): Boolean
    open fun isZero(): Boolean
    open fun minus(amount: Int, unit: TemporalUnit): Duration
    open fun minus(duration: Duration): Duration
    open fun minusDays(daysToSubtract: Int): Duration
    open fun minusHours(hoursToSubtract: Int): Duration
    open fun minusMillis(millisToSubtract: Double): Duration
    open fun minusMinutes(minutesToSubtract: Int): Duration
    open fun minusNanos(nanosToSubtract: Double): Duration
    open fun minusSeconds(secondsToSubtract: Int): Duration
    open fun multipliedBy(multiplicand: Int): Duration
    open fun nano(): Double
    open fun negated(): Duration
    open fun plus(amount: Int, unit: TemporalUnit): Duration
    open fun plus(duration: Duration): Duration
    open fun plusDays(daysToAdd: Int): Duration
    open fun plusHours(hoursToAdd: Int): Duration
    open fun plusMillis(millisToAdd: Double): Duration
    open fun plusMinutes(minutesToAdd: Int): Duration
    open fun plusNanos(nanosToAdd: Double): Duration
    open fun plusSeconds(secondsToAdd: Int): Duration
    open fun plusSecondsNanos(secondsToAdd: Int, nanosToAdd: Double): Duration
    open fun seconds(): Double
    override fun <T : Temporal> subtractFrom(temporal: T): T
    open fun toDays(): Int
    open fun toHours(): Int
    open fun toJSON(): String
    open fun toMillis(): Double
    open fun toMinutes(): Int
    open fun toNanos(): Double
    override fun toString(): String
//    override fun units(): JsArray<TemporalUnit>
    open fun withNanos(nanoOfSecond: Double): Duration
    open fun withSeconds(seconds: Int): Duration

    companion object {
        var ZERO: Duration
        fun between(startInclusive: Temporal, endExclusive: Temporal): Duration
        fun from(amount: TemporalAmount): Duration
        fun of(amount: Int, unit: TemporalUnit): Duration
        fun ofDays(days: Int): Duration
        fun ofHours(hours: Int): Duration
        fun ofMillis(millis: Double): Duration
        fun ofMinutes(minutes: Int): Duration
        fun ofNanos(nanos: Double): Duration
        fun ofSeconds(seconds: Int, nanoAdjustment: Int = definedExternally): Duration
        fun parse(text: String): Duration
    }
}

external open class Instant : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atZone(zone: ZoneId): ZonedDateTime
    open fun compareTo(otherInstant: Instant): Int
    open fun epochSecond(): Double
    open fun equals(other: InteropInterface): Boolean
    override fun getLong(field: TemporalField): Long
    override fun hashCode(): Int
    open fun isAfter(otherInstant: Instant): Boolean
    open fun isBefore(otherInstant: Instant): Boolean
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    override fun minus(amount: TemporalAmount): Instant
    override fun minus(amountToSubtract: Int, unit: TemporalUnit): Instant
    open fun minusMillis(millisToSubtract: Double): Instant
    open fun minusNanos(nanosToSubtract: Double): Instant
    open fun minusSeconds(secondsToSubtract: Int): Instant
    open fun nano(): Double
    override fun plus(amount: TemporalAmount): Instant
    override fun plus(amountToAdd: Int, unit: TemporalUnit): Instant
    open fun plusMillis(millisToAdd: Double): Instant
    open fun plusNanos(nanosToAdd: Double): Instant
    open fun plusSeconds(secondsToAdd: Int): Instant
    open fun toEpochMilli(): Double
    open fun toJSON(): String
    override fun toString(): String
    open fun truncatedTo(unit: TemporalUnit): Instant
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Double
    override fun with(adjuster: TemporalAdjuster): Instant
    override fun with(field: TemporalField, newValue: Int): Instant

    companion object {
        var EPOCH: Instant
        var MIN: Instant
        var MAX: Instant
        var MIN_SECONDS: Instant
        var MAX_SECONDS: Instant
        var FROM: TemporalQuery<Instant>
        fun from(temporal: TemporalAccessor): Instant
        fun now(clock: Clock = definedExternally): Instant
        fun ofEpochMilli(epochMilli: Double): Instant
        fun ofEpochSecond(epochSecond: Double, nanoAdjustment: Int = definedExternally): Instant
        fun parse(text: String): Instant
    }
}

external open class LocalDate : ChronoLocalDate {
    open fun atStartOfDay(): LocalDateTime
    open fun atStartOfDay(zone: ZoneId): ZonedDateTime
    open fun atTime(hour: Int, minute: Int, second: Int = definedExternally, nanoOfSecond: Double = definedExternally): LocalDateTime
    open fun atTime(hour: Int, minute: Int): LocalDateTime
    open fun atTime(hour: Int, minute: Int, second: Int = definedExternally): LocalDateTime
    open fun atTime(time: LocalTime): LocalDateTime
    open fun chronology(): Chronology
    open fun compareTo(other: LocalDate): Int
    open fun dayOfMonth(): Int
    open fun dayOfWeek(): DayOfWeek
    open fun dayOfYear(): Int
    open fun equals(other: InteropInterface): Boolean
    override fun getLong(field: TemporalField): Long
    override fun hashCode(): Int
    open fun isAfter(other: LocalDate): Boolean
    open fun isBefore(other: LocalDate): Boolean
    open fun isEqual(other: LocalDate): Boolean
    open fun isLeapYear(): Boolean
    open fun isoWeekOfWeekyear(): Int
    open fun isoWeekyear(): Int
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    open fun lengthOfMonth(): Int
    open fun lengthOfYear(): Int
    override fun minus(amount: TemporalAmount): LocalDate
    override fun minus(amountToSubtract: Int, unit: TemporalUnit): LocalDate
    open fun minusDays(daysToSubtract: Int): LocalDate
    open fun minusMonths(monthsToSubtract: Int): LocalDate
    open fun minusWeeks(weeksToSubtract: Int): LocalDate
    open fun minusYears(yearsToSubtract: Int): LocalDate
    open fun month(): Month
    open fun monthValue(): Int
    override fun plus(amount: TemporalAmount): LocalDate
    override fun plus(amountToAdd: Int, unit: TemporalUnit): LocalDate
    open fun plusDays(daysToAdd: Int): LocalDate
    open fun plusDays(daysToAdd: Double): LocalDate
    open fun plusMonths(monthsToAdd: Int): LocalDate
    open fun plusMonths(monthsToAdd: Double): LocalDate
    open fun plusWeeks(weeksToAdd: Int): LocalDate
    open fun plusYears(yearsToAdd: Int): LocalDate
    open fun toEpochDay(): Double
    open fun toJSON(): String
    override fun toString(): String
    open fun until(endDate: TemporalAccessor): Period
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Double
    override fun with(adjuster: TemporalAdjuster): LocalDate
    override fun with(field: TemporalField, newValue: Int): LocalDate
    open fun withDayOfMonth(dayOfMonth: Int): LocalDate
    open fun withDayOfYear(dayOfYear: Int): LocalDate
    open fun withMonth(month: Month): LocalDate
    open fun withMonth(month: Int): LocalDate
    open fun withYear(year: Int): LocalDate
    open fun year(): Int

    companion object {
        var MIN: LocalDate
        var MAX: LocalDate
        var EPOCH_0: LocalDate
        var FROM: TemporalQuery<LocalDate>
        fun from(temporal: TemporalAccessor): LocalDate
        fun now(clockOrZone: Clock = definedExternally): LocalDate
        fun now(clockOrZone: ZoneId = definedExternally): LocalDate
        fun of(year: Int, month: Month, dayOfMonth: Int): LocalDate
        fun of(year: Int, month: Int, dayOfMonth: Int): LocalDate
        fun ofEpochDay(epochDay: Int): LocalDate
        fun ofInstant(instant: Instant, zoneId: ZoneId = definedExternally): LocalDate
        fun ofYearDay(year: Int, dayOfYear: Int): LocalDate
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): LocalDate
    }
}

external open class LocalDateTime : ChronoLocalDateTime {
    open fun atOffset(offset: ZoneOffset): OffsetDateTime
    open fun atZone(zone: ZoneId): ZonedDateTime
    open fun compareTo(other: LocalDateTime): Int
    open fun dayOfMonth(): Int
    open fun dayOfWeek(): DayOfWeek
    open fun dayOfYear(): Int
    open fun equals(other: InteropInterface): Boolean
    open fun format(formatter: DateTimeFormatter): String
    override fun getLong(field: TemporalField): Long
    override fun hashCode(): Int
    open fun hour(): Int
    open fun isAfter(other: LocalDateTime): Boolean
    open fun isBefore(other: LocalDateTime): Boolean
    open fun isEqual(other: LocalDateTime): Boolean
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    override fun minus(amount: TemporalAmount): LocalDateTime
    override fun minus(amountToSubtract: Int, unit: TemporalUnit): LocalDateTime
    open fun minusDays(days: Int): LocalDateTime
    open fun minusHours(hours: Int): LocalDateTime
    open fun minusMinutes(minutes: Int): LocalDateTime
    open fun minusMonths(months: Int): LocalDateTime
    open fun minusNanos(nanos: Double): LocalDateTime
    open fun minusSeconds(seconds: Int): LocalDateTime
    open fun minusWeeks(weeks: Int): LocalDateTime
    open fun minusYears(years: Int): LocalDateTime
    open fun minute(): Int
    open fun month(): Month
    open fun monthValue(): Int
    open fun nano(): Double
    override fun plus(amount: TemporalAmount): LocalDateTime
    override fun plus(amountToAdd: Int, unit: TemporalUnit): LocalDateTime
    open fun plusDays(days: Int): LocalDateTime
    open fun plusHours(hours: Int): LocalDateTime
    open fun plusMinutes(minutes: Int): LocalDateTime
    open fun plusMonths(months: Int): LocalDateTime
    open fun plusNanos(nanos: Double): LocalDateTime
    open fun plusSeconds(seconds: Int): LocalDateTime
    open fun plusWeeks(weeks: Int): LocalDateTime
    open fun plusYears(years: Int): LocalDateTime
    open fun second(): Int
    open fun toJSON(): String
    open fun toLocalDate(): LocalDate
    open fun toLocalTime(): LocalTime
    override fun toString(): String
    open fun truncatedTo(unit: TemporalUnit): LocalDateTime
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Double
    override fun with(adjuster: TemporalAdjuster): LocalDateTime
    override fun with(field: TemporalField, newValue: Int): LocalDateTime
    open fun withDayOfMonth(dayOfMonth: Int): LocalDateTime
    open fun withDayOfYear(dayOfYear: Int): LocalDateTime
    open fun withHour(hour: Int): LocalDateTime
    open fun withMinute(minute: Int): LocalDateTime
    open fun withMonth(month: Int): LocalDateTime
    open fun withMonth(month: Month): LocalDateTime
    open fun withNano(nanoOfSecond: Int): LocalDateTime
    open fun withSecond(second: Int): LocalDateTime
    open fun withYear(year: Int): LocalDateTime
    open fun year(): Int

    companion object {
        var MIN: LocalDateTime
        var MAX: LocalDateTime
        var FROM: TemporalQuery<LocalDateTime>
        fun from(temporal: TemporalAccessor): LocalDateTime
        fun now(clockOrZone: Clock = definedExternally): LocalDateTime
        fun now(clockOrZone: ZoneId = definedExternally): LocalDateTime
        fun of(date: LocalDate, time: LocalTime): LocalDateTime
        fun of(year: Int, month: Month, dayOfMonth: Int, hour: Int = definedExternally, minute: Int = definedExternally, second: Int = definedExternally, nanoSecond: Int = definedExternally): LocalDateTime
        fun of(year: Int, month: Int, dayOfMonth: Int, hour: Int = definedExternally, minute: Int = definedExternally, second: Int = definedExternally, nanoSecond: Int = definedExternally): LocalDateTime
        fun ofEpochSecond(epochSecond: Double, nanoOfSecond: Int, offset: ZoneOffset): LocalDateTime
        fun ofEpochSecond(epochSecond: Double, offset: ZoneOffset): LocalDateTime
        fun ofInstant(instant: Instant, zoneId: ZoneId = definedExternally): LocalDateTime
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): LocalDateTime
    }
}

external open class LocalTime : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atDate(date: LocalDate): LocalDateTime
    open fun compareTo(other: LocalTime): Int
    open fun equals(other: InteropInterface): Boolean
    open fun format(formatter: DateTimeFormatter): String
    open fun getLong(field: ChronoField): Long
    override fun getLong(field: TemporalField): Long
    override fun hashCode(): Int
    open fun hour(): Int
    open fun isAfter(other: LocalTime): Boolean
    open fun isBefore(other: LocalTime): Boolean
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    override fun minus(amount: TemporalAmount): LocalTime
    override fun minus(amountToSubtract: Int, unit: TemporalUnit): LocalTime
    open fun minusHours(hoursToSubtract: Int): LocalTime
    open fun minusMinutes(minutesToSubtract: Int): LocalTime
    open fun minusNanos(nanosToSubtract: Double): LocalTime
    open fun minusSeconds(secondsToSubtract: Int): LocalTime
    open fun minute(): Int
    open fun nano(): Double
    override fun plus(amount: TemporalAmount): LocalTime
    override fun plus(amountToAdd: Int, unit: TemporalUnit): LocalTime
    open fun plusHours(hoursToAdd: Int): LocalTime
    open fun plusMinutes(minutesToAdd: Int): LocalTime
    open fun plusNanos(nanosToAdd: Double): LocalTime
    open fun plusSeconds(secondstoAdd: Int): LocalTime
    open fun second(): Int
    open fun toJSON(): String
    open fun toNanoOfDay(): Double
    open fun toSecondOfDay(): Int
    override fun toString(): String
    open fun truncatedTo(unit: ChronoUnit): LocalTime
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Double
    override fun with(adjuster: TemporalAdjuster): LocalTime
    override fun with(field: TemporalField, newValue: Int): LocalTime
    open fun withHour(hour: Int): LocalTime
    open fun withMinute(minute: Int): LocalTime
    open fun withNano(nanoOfSecond: Int): LocalTime
    open fun withSecond(second: Int): LocalTime

    companion object {
        var MIN: LocalTime
        var MAX: LocalTime
        var MIDNIGHT: LocalTime
        var NOON: LocalTime
        var HOURS_PER_DAY: Int
        var MINUTES_PER_HOUR: Int
        var MINUTES_PER_DAY: Int
        var SECONDS_PER_MINUTE: Int
        var SECONDS_PER_HOUR: Int
        var SECONDS_PER_DAY: Int
        var MILLIS_PER_DAY: Double
        var MICROS_PER_DAY: Double
        var NANOS_PER_SECOND: Double
        var NANOS_PER_MINUTE: Double
        var NANOS_PER_HOUR: Double
        var NANOS_PER_DAY: Double
        var FROM: TemporalQuery<LocalTime>
        fun from(temporal: TemporalAccessor): LocalTime
        fun now(clockOrZone: Clock = definedExternally): LocalTime
        fun now(clockOrZone: ZoneId = definedExternally): LocalTime
        fun of(hour: Int = definedExternally, minute: Int = definedExternally, second: Int = definedExternally, nanoOfSecond: Int = definedExternally): LocalTime
        fun ofInstant(instant: Instant, zone: ZoneId = definedExternally): LocalTime
        fun ofNanoOfDay(nanoOfDay: Double): LocalTime
        fun ofSecondOfDay(secondOfDay: Int = definedExternally, nanoOfSecond: Int = definedExternally): LocalTime
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): LocalTime
    }
}

external open class MonthDay : TemporalAccessor {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atYear(year: Int): LocalDate
    open fun compareTo(other: MonthDay): Int
    open fun dayOfMonth(): Int
    open fun equals(other: InteropInterface): Boolean
    open fun format(formatter: DateTimeFormatter): String
    override fun getLong(field: TemporalField): Long
    open fun isAfter(other: MonthDay): Boolean
    open fun isBefore(other: MonthDay): Boolean
    override fun isSupported(field: TemporalField): Boolean
    open fun isValidYear(year: Int): Boolean
    open fun month(): Month
    open fun monthValue(): Int
    open fun toJSON(): String
    override fun toString(): String
    open fun with(month: Month): MonthDay
    open fun withDayOfMonth(dayOfMonth: Int): MonthDay
    open fun withMonth(month: Int): MonthDay

    companion object {
        var FROM: TemporalQuery<MonthDay>
        fun from(temporal: TemporalAccessor): MonthDay
        fun now(zoneIdOrClock: ZoneId = definedExternally): MonthDay
        fun now(zoneIdOrClock: Clock = definedExternally): MonthDay
        fun of(month: Month, dayOfMonth: Int): MonthDay
        fun of(month: Int, dayOfMonth: Int): MonthDay
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): MonthDay
    }
}

external open class Period : TemporalAmount {
    override fun <T : Temporal> addTo(temporal: T): T
    open fun chronology(): IsoChronology
    open fun days(): Int
    open fun equals(other: InteropInterface): Boolean
    override fun get(unit: TemporalUnit): Int
    override fun hashCode(): Int
    open fun isNegative(): Boolean
    open fun isZero(): Boolean
    open fun minus(amountToSubtract: TemporalAmount): Period
    open fun minusDays(daysToSubtract: Int): Period
    open fun minusMonths(monthsToSubtract: Int): Period
    open fun minusYears(yearsToSubtract: Int): Period
    open fun months(): Int
    open fun multipliedBy(scalar: Int): Period
    open fun negated(): Period
    open fun normalized(): Period
    open fun plus(amountToAdd: TemporalAmount): Period
    open fun plusDays(daysToAdd: Int): Period
    open fun plusMonths(monthsToAdd: Int): Period
    open fun plusYears(yearsToAdd: Int): Period
    override fun <T : Temporal> subtractFrom(temporal: T): T
    open fun toJSON(): String
    override fun toString(): String
    open fun toTotalMonths(): Int
//    override fun units(): JsArray<TemporalUnit>
    open fun withDays(days: Int): Period
    open fun withMonths(months: Int): Period
    open fun withYears(years: Int): Period
    open fun years(): Int

    companion object {
        var ZERO: Period
        fun between(startDate: LocalDate, endDate: LocalDate): Period
        fun from(amount: TemporalAmount): Period
        fun of(years: Int, months: Int, days: Int): Period
        fun ofDays(days: Int): Period
        fun ofMonths(months: Int): Period
        fun ofWeeks(weeks: Int): Period
        fun ofYears(years: Int): Period
        fun parse(text: String): Period
    }
}

external open class Year : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atDay(dayOfYear: Int): LocalDate
    open fun atMonth(month: Month): YearMonth
    open fun atMonth(month: Int): YearMonth
    open fun atMonthDay(monthDay: MonthDay): LocalDate
    open fun compareTo(other: Year): Int
    open fun equals(other: InteropInterface): Boolean
    override fun getLong(field: TemporalField): Long
    open fun isAfter(other: Year): Boolean
    open fun isBefore(other: Year): Boolean
    open fun isLeap(): Boolean
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    open fun isValidMonthDay(monthDay: MonthDay): Boolean
    open fun length(): Int
    override fun minus(amount: TemporalAmount): Year
    override fun minus(amountToSubtract: Int, unit: TemporalUnit): Year
    open fun minusYears(yearsToSubtract: Int): Year
    override fun plus(amount: TemporalAmount): Year
    override fun plus(amountToAdd: Int, unit: TemporalUnit): Year
    open fun plusYears(yearsToAdd: Int): Year
    open fun toJSON(): String
    override fun toString(): String
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Double
    open fun value(): Int
    override fun with(adjuster: TemporalAdjuster): Year
    override fun with(field: TemporalField, newValue: Int): Year

    companion object {
        var MIN_VALUE: Int
        var MAX_VALUE: Int
        var FROM: TemporalQuery<Year>
        fun from(temporal: TemporalAccessor): Year
        fun isLeap(year: Int): Boolean
        fun now(zoneIdOrClock: ZoneId = definedExternally): Year
        fun now(zoneIdOrClock: Clock = definedExternally): Year
        fun of(isoYear: Int): Year
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): Year
    }
}

external open class YearMonth : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atDay(dayOfMonth: Int): LocalDate
    open fun atEndOfMonth(): LocalDate
    open fun compareTo(other: YearMonth): Int
    open fun equals(other: InteropInterface): Boolean
    open fun format(formatter: DateTimeFormatter): String
    override fun getLong(field: TemporalField): Long
    open fun isAfter(other: YearMonth): Boolean
    open fun isBefore(other: YearMonth): Boolean
    open fun isLeapYear(): Boolean
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    open fun isValidDay(): Boolean
    open fun lengthOfMonth(): Int
    open fun lengthOfYear(): Int
    override fun minus(amount: TemporalAmount): YearMonth
    override fun minus(amountToSubtract: Int, unit: TemporalUnit): YearMonth
    open fun minusMonths(monthsToSubtract: Int): YearMonth
    open fun minusYears(yearsToSubtract: Int): YearMonth
    open fun month(): Month
    open fun monthValue(): Int
    override fun plus(amount: TemporalAmount): YearMonth
    override fun plus(amountToAdd: Int, unit: TemporalUnit): YearMonth
    open fun plusMonths(monthsToAdd: Int): YearMonth
    open fun plusYears(yearsToAdd: Int): YearMonth
    open fun toJSON(): String
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Double
    override fun with(adjuster: TemporalAdjuster): YearMonth
    override fun with(field: TemporalField, newValue: Int): YearMonth
    open fun withMonth(month: Int): YearMonth
    open fun withYear(year: Int): YearMonth
    open fun year(): Int

    companion object {
        var FROM: TemporalQuery<YearMonth>
        fun from(temporal: TemporalAccessor): YearMonth
        fun now(zoneIdOrClock: ZoneId = definedExternally): YearMonth
        fun now(zoneIdOrClock: Clock = definedExternally): YearMonth
        fun of(year: Int, monthOrInt: Month): YearMonth
        fun of(year: Int, monthOrInt: Int): YearMonth
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): YearMonth
    }
}

external open class OffsetDateTime : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atZoneSameInstant(zone: ZoneId): ZonedDateTime
    open fun atZoneSimilarLocal(zone: ZoneId): ZonedDateTime
    open fun compareTo(other: OffsetDateTime): Int
    open fun equals(obj: InteropInterface): Boolean
    open fun format(formatter: DateTimeFormatter): String
    override fun get(field: TemporalField): Int
    open fun dayOfMonth(): Int
    open fun dayOfWeek(): DayOfWeek
    open fun dayOfYear(): Int
    open fun hour(): Int
    override fun getLong(field: TemporalField): Long
    open fun minute(): Int
    open fun month(): Month
    open fun monthValue(): Int
    open fun nano(): Double
    open fun offset(): ZoneOffset
    open fun second(): Int
    open fun year(): Int
    override fun hashCode(): Int
    open fun isAfter(other: OffsetDateTime): Boolean
    open fun isBefore(other: OffsetDateTime): Boolean
    open fun isEqual(other: OffsetDateTime): Boolean
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    override fun minus(amountToSubtract: Int, unit: TemporalUnit): OffsetDateTime
    override fun minus(amountToSubtract: TemporalAmount): OffsetDateTime
    open fun minusDays(days: Int): OffsetDateTime
    open fun minusHours(hours: Int): OffsetDateTime
    open fun minusMinutes(minutes: Int): OffsetDateTime
    open fun minusMonths(months: Int): OffsetDateTime
    open fun minusNanos(nanos: Double): OffsetDateTime
    open fun minusSeconds(seconds: Int): OffsetDateTime
    open fun minusWeeks(weeks: Int): OffsetDateTime
    open fun minusYears(years: Int): OffsetDateTime
    override fun plus(amountToAdd: Int, unit: TemporalUnit): OffsetDateTime
    override fun plus(amountToAdd: TemporalAmount): OffsetDateTime
    open fun plusDays(days: Int): OffsetDateTime
    open fun plusHours(hours: Int): OffsetDateTime
    open fun plusMinutes(minutes: Int): OffsetDateTime
    open fun plusMonths(months: Int): OffsetDateTime
    open fun plusNanos(nanos: Double): OffsetDateTime
    open fun plusSeconds(seconds: Int): OffsetDateTime
    open fun plusWeeks(weeks: Int): OffsetDateTime
    open fun plusYears(years: Int): OffsetDateTime
    override fun <R : InteropInterface> query(query: TemporalQuery<R>): R?
    override fun range(field: TemporalField): ValueRange
    open fun toEpochSecond(): Double
    open fun toJSON(): String
    open fun toInstant(): Instant
    open fun toLocalDate(): LocalDate
    open fun toLocalDateTime(): LocalDateTime
    open fun toLocalTime(): LocalTime
    open fun toOffsetTime(): OffsetTime
    override fun toString(): String
    open fun truncatedTo(unit: TemporalUnit): OffsetDateTime
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Double
    override fun with(adjuster: TemporalAdjuster): OffsetDateTime
    override fun with(field: TemporalField, newValue: Int): OffsetDateTime
    open fun withDayOfMonth(dayOfMonth: Int): OffsetDateTime
    open fun withDayOfYear(dayOfYear: Int): OffsetDateTime
    open fun withHour(hour: Int): OffsetDateTime
    open fun withMinute(minute: Int): OffsetDateTime
    open fun withMonth(month: Int): OffsetDateTime
    open fun withNano(nanoOfSecond: Int): OffsetDateTime
    open fun withOffsetSameInstant(offset: ZoneOffset): OffsetDateTime
    open fun withOffsetSameLocal(offset: ZoneOffset): OffsetDateTime
    open fun withSecond(second: Int): OffsetDateTime
    open fun withYear(year: Int): OffsetDateTime

    companion object {
        var MIN: OffsetDateTime
        var MAX: OffsetDateTime
        var FROM: TemporalQuery<OffsetDateTime>
        fun from(temporal: TemporalAccessor): OffsetDateTime
        fun now(clockOrZone: Clock = definedExternally): OffsetDateTime
        fun now(clockOrZone: ZoneId = definedExternally): OffsetDateTime
        fun of(dateTime: LocalDateTime, offset: ZoneOffset): OffsetDateTime
        fun of(date: LocalDate, time: LocalTime, offset: ZoneOffset): OffsetDateTime
        fun of(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, nanoOfSecond: Int, offset: ZoneOffset): OffsetDateTime
        fun ofInstant(instant: Instant, zone: ZoneId): OffsetDateTime
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): OffsetDateTime
    }
}

external open class OffsetTime : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atDate(date: LocalDate): OffsetDateTime
    open fun compareTo(other: OffsetTime): Int
    open fun equals(other: InteropInterface): Boolean
    open fun format(formatter: DateTimeFormatter): String
    override fun get(field: TemporalField): Int
    open fun hour(): Int
    override fun getLong(field: TemporalField): Long
    open fun minute(): Int
    open fun nano(): Double
    open fun offset(): ZoneOffset
    open fun second(): Int
    override fun hashCode(): Int
    open fun isAfter(other: OffsetTime): Boolean
    open fun isBefore(other: OffsetTime): Boolean
    open fun isEqual(other: OffsetTime): Boolean
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    override fun minus(amountToSubtract: Int, unit: TemporalUnit): OffsetTime
    override fun minus(amountToSubtract: TemporalAmount): OffsetTime
    open fun minusHours(hours: Int): OffsetTime
    open fun minusMinutes(minutes: Int): OffsetTime
    open fun minusNanos(nanos: Double): OffsetTime
    open fun minusSeconds(seconds: Int): OffsetTime
    override fun plus(amountToAdd: Int, unit: TemporalUnit): OffsetTime
    override fun plus(amountToAdd: TemporalAmount): OffsetTime
    open fun plusHours(hours: Int): OffsetTime
    open fun plusMinutes(minutes: Int): OffsetTime
    open fun plusNanos(nanos: Double): OffsetTime
    open fun plusSeconds(seconds: Int): OffsetTime
    override fun <R : InteropInterface> query(query: TemporalQuery<R>): R?
    override fun range(field: TemporalField): ValueRange
    open fun toEpochSecond(date: LocalDate): Double
    open fun toJSON(): String
    open fun toLocalTime(): LocalTime
    override fun toString(): String
    open fun truncatedTo(unit: TemporalUnit): OffsetTime
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Double
    override fun with(adjuster: TemporalAdjuster): OffsetTime
    override fun with(field: TemporalField, newValue: Int): OffsetTime
    open fun withHour(hour: Int): OffsetTime
    open fun withMinute(minute: Int): OffsetTime
    open fun withNano(nanoOfSecond: Int): OffsetTime
    open fun withOffsetSameInstant(offset: ZoneOffset): OffsetTime
    open fun withOffsetSameLocal(offset: ZoneOffset): OffsetTime
    open fun withSecond(second: Int): OffsetTime

    companion object {
        var MIN: OffsetTime
        var MAX: OffsetTime
        var FROM: TemporalQuery<OffsetTime>
        fun from(temporal: TemporalAccessor): OffsetTime
        fun now(clockOrZone: Clock = definedExternally): OffsetTime
        fun now(clockOrZone: ZoneId = definedExternally): OffsetTime
        fun of(time: LocalTime, offset: ZoneOffset): OffsetTime
        fun of(hour: Int, minute: Int, second: Int, nanoOfSecond: Int, offset: ZoneOffset): OffsetTime
        fun ofInstant(instant: Instant, zone: ZoneId): OffsetTime
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): OffsetTime
    }
}

external open class ZonedDateTime : ChronoZonedDateTime {
    open fun dayOfMonth(): Int
    open fun dayOfWeek(): DayOfWeek
    open fun dayOfYear(): Int
    override fun equals(other: InteropInterface): Boolean
    override fun format(formatter: DateTimeFormatter): String
    override fun getLong(field: TemporalField): Long
    override fun hashCode(): Int
    open fun hour(): Int
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    override fun minus(amount: TemporalAmount): ZonedDateTime
    override fun minus(amountToSubtract: Int, unit: TemporalUnit): ZonedDateTime
    open fun minusDays(days: Int): ZonedDateTime
    open fun minusHours(hours: Int): ZonedDateTime
    open fun minusMinutes(minutes: Int): ZonedDateTime
    open fun minusMonths(months: Int): ZonedDateTime
    open fun minusNanos(nanos: Double): ZonedDateTime
    open fun minusSeconds(seconds: Int): ZonedDateTime
    open fun minusWeeks(weeks: Int): ZonedDateTime
    open fun minusYears(years: Int): ZonedDateTime
    open fun minute(): Int
    open fun month(): Month
    open fun monthValue(): Int
    open fun nano(): Double
    open fun offset(): ZoneOffset
    override fun plus(amount: TemporalAmount): ZonedDateTime
    override fun plus(amountToAdd: Int, unit: TemporalUnit): ZonedDateTime
    open fun plusDays(days: Int): ZonedDateTime
    open fun plusDays(days: Double): ZonedDateTime
    open fun plusHours(hours: Int): ZonedDateTime
    open fun plusMinutes(minutes: Int): ZonedDateTime
    open fun plusMonths(months: Int): ZonedDateTime
    open fun plusMonths(months: Double): ZonedDateTime
    open fun plusNanos(nanos: Double): ZonedDateTime
    open fun plusSeconds(seconds: Int): ZonedDateTime
    open fun plusWeeks(weeks: Int): ZonedDateTime
    open fun plusYears(years: Int): ZonedDateTime
    override fun range(field: TemporalField): ValueRange
    open fun second(): Int
    open fun toJSON(): String
    open fun toLocalDate(): LocalDate
    open fun toLocalDateTime(): LocalDateTime
    open fun toLocalTime(): LocalTime
    open fun toOffsetDateTime(): OffsetDateTime
    override fun toString(): String
    open fun truncatedTo(unit: TemporalUnit): ZonedDateTime
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Double
    override fun with(adjuster: TemporalAdjuster): ZonedDateTime
    override fun with(field: TemporalField, newValue: Int): ZonedDateTime
    open fun withDayOfMonth(dayOfMonth: Int): ZonedDateTime
    open fun withDayOfYear(dayOfYear: Int): ZonedDateTime
    open fun withEarlierOffsetAtOverlap(): ZonedDateTime
    open fun withFixedOffsetZone(): ZonedDateTime
    open fun withHour(hour: Int): ZonedDateTime
    open fun withLaterOffsetAtOverlap(): ZonedDateTime
    open fun withMinute(minute: Int): ZonedDateTime
    open fun withMonth(month: Int): ZonedDateTime
    open fun withNano(nanoOfSecond: Int): ZonedDateTime
    open fun withSecond(second: Int): ZonedDateTime
    open fun withYear(year: Int): ZonedDateTime
    open fun withZoneSameInstant(zone: ZoneId): ZonedDateTime
    open fun withZoneSameLocal(zone: ZoneId): ZonedDateTime
    open fun year(): Int
    open fun zone(): ZoneId

    companion object {
        var FROM: TemporalQuery<ZonedDateTime>
        fun from(temporal: TemporalAccessor): ZonedDateTime
        fun now(clockOrZone: Clock = definedExternally): ZonedDateTime
        fun now(clockOrZone: ZoneId = definedExternally): ZonedDateTime
        fun of(localDateTime: LocalDateTime, zone: ZoneId): ZonedDateTime
        fun of(date: LocalDate, time: LocalTime, zone: ZoneId): ZonedDateTime
        fun of(year: Int, month: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanoOfSecond: Int, zone: ZoneId): ZonedDateTime
        fun ofInstant(instant: Instant, zone: ZoneId): ZonedDateTime
        fun ofInstant(localDateTime: LocalDateTime, offset: ZoneOffset, zone: ZoneId): ZonedDateTime
        fun ofLocal(localDateTime: LocalDateTime, zone: ZoneId, preferredOffset: ZoneOffset? = definedExternally): ZonedDateTime
        fun ofStrict(localDateTime: LocalDateTime, offset: ZoneOffset, zone: ZoneId): ZonedDateTime
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): ZonedDateTime
    }
}

external open class ZoneId : InteropInterface {
    open fun equals(other: InteropInterface): Boolean
    override fun hashCode(): Int
    open fun id(): String
    open fun normalized(): ZoneId
    open fun rules(): ZoneRules
    open fun toJSON(): String
    override fun toString(): String

    companion object {
        var SYSTEM: ZoneId
        var UTC: ZoneId
        fun systemDefault(): ZoneId
        fun of(zoneId: String): ZoneId
        fun ofOffset(prefix: String, offset: ZoneOffset): ZoneId
        fun from(temporal: TemporalAccessor): ZoneId
        fun getAvailableZoneIds(): InteropInterface
    }
}

external open class ZoneOffset : ZoneId {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun compareTo(other: ZoneOffset): Int
    override fun equals(other: InteropInterface): Boolean
    open fun get(field: TemporalField): Int
    open fun getLong(field: TemporalField): Long
    override fun hashCode(): Int
    override fun id(): String
    override fun rules(): ZoneRules
    override fun toString(): String
    open fun totalSeconds(): Int

    companion object {
        var MAX_SECONDS: ZoneOffset
        var UTC: ZoneOffset
        var MIN: ZoneOffset
        var MAX: ZoneOffset
        fun of(offsetId: String): ZoneOffset
        fun ofHours(hours: Int): ZoneOffset
        fun ofHoursMinutes(hours: Int, minutes: Int): ZoneOffset
        fun ofHoursMinutesSeconds(hours: Int, minutes: Int, seconds: Int): ZoneOffset
        fun ofTotalMinutes(totalMinutes: Int): ZoneOffset
        fun ofTotalSeconds(totalSeconds: Int): ZoneOffset
    }
}

external open class ZoneRegion : ZoneId {
    override fun id(): String
    override fun rules(): ZoneRules

    companion object {
        fun ofId(zoneId: String): ZoneId
    }
}

external open class DayOfWeek : TemporalAccessor {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun compareTo(other: DayOfWeek): Int
    open fun equals(other: InteropInterface): Boolean
    open fun displayName(style: TextStyle, locale: Locale): String
    override fun getLong(field: TemporalField): Long
    override fun isSupported(field: TemporalField): Boolean
    open fun minus(days: Int): DayOfWeek
    open fun name(): String
    open fun ordinal(): Int
    open fun plus(days: Int): DayOfWeek
    open fun toJSON(): String
    override fun toString(): String
    open fun value(): Int

    companion object {
        var MONDAY: DayOfWeek
        var TUESDAY: DayOfWeek
        var WEDNESDAY: DayOfWeek
        var THURSDAY: DayOfWeek
        var FRIDAY: DayOfWeek
        var SATURDAY: DayOfWeek
        var SUNDAY: DayOfWeek
        var FROM: TemporalQuery<DayOfWeek>
        fun from(temporal: TemporalAccessor): DayOfWeek
        fun of(dayOfWeek: Int): DayOfWeek
        fun valueOf(name: String): DayOfWeek
//        fun values(): JsArray<DayOfWeek>
    }
}

external open class Month : TemporalAccessor {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun compareTo(other: Month): Int
    open fun equals(other: InteropInterface): Boolean
    open fun firstDayOfYear(leapYear: Boolean): Int
    open fun firstMonthOfQuarter(): Month
    open fun displayName(style: TextStyle, locale: Locale): String
    override fun getLong(field: TemporalField): Long
    override fun isSupported(field: TemporalField): Boolean
    open fun length(leapYear: Boolean): Int
    open fun maxLength(): Int
    open fun minLength(): Int
    open fun minus(months: Int): Month
    open fun name(): String
    open fun ordinal(): Int
    open fun plus(months: Int): Month
    open fun toJSON(): String
    override fun toString(): String
    open fun value(): Int

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
        fun of(month: Int): Month
        fun valueOf(name: String): Month
//        fun values(): JsArray<Month>
    }
}

external open class DateTimeFormatter : InteropInterface {
    open fun chronology(): Chronology?
    open fun decimalStyle(): DecimalStyle
    open fun format(temporal: TemporalAccessor): String
    open fun locale(): InteropInterface
    open fun parse(text: String): TemporalAccessor
    open fun <T : InteropInterface> parse(text: String, query: TemporalQuery<T>): T
    open fun parseUnresolved(text: String, position: ParsePosition): TemporalAccessor
    override fun toString(): String
    open fun withChronology(chrono: Chronology): DateTimeFormatter
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
        fun parsedExcessDays(): TemporalQuery<Period>
//        fun parsedLeapSecond(): TemporalQuery<JsBoolean>
    }
}

external open class DateTimeFormatterBuilder : InteropInterface {
    open fun append(formatter: DateTimeFormatter): DateTimeFormatterBuilder
    open fun appendFraction(field: TemporalField, minWidth: Int, maxWidth: Int, decimalPoint: Boolean): DateTimeFormatterBuilder
    open fun appendInstant(fractionalDigits: Int): DateTimeFormatterBuilder
    open fun appendLiteral(literal: InteropInterface): DateTimeFormatterBuilder
    open fun appendOffset(pattern: String, noOffsetText: String): DateTimeFormatterBuilder
    open fun appendOffsetId(): DateTimeFormatterBuilder
    open fun appendPattern(pattern: String): DateTimeFormatterBuilder
    open fun appendValue(field: TemporalField, width: Int = definedExternally, maxWidth: Int = definedExternally, signStyle: SignStyle = definedExternally): DateTimeFormatterBuilder
    open fun appendValueReduced(field: TemporalField, width: Int, maxWidth: Int, base: ChronoLocalDate): DateTimeFormatterBuilder
    open fun appendValueReduced(field: TemporalField, width: Int, maxWidth: Int, base: Int): DateTimeFormatterBuilder
    open fun appendZoneId(): DateTimeFormatterBuilder
    open fun optionalEnd(): DateTimeFormatterBuilder
    open fun optionalStart(): DateTimeFormatterBuilder
    open fun padNext(): DateTimeFormatterBuilder
    open fun parseCaseInsensitive(): DateTimeFormatterBuilder
    open fun parseCaseSensitive(): DateTimeFormatterBuilder
    open fun parseLenient(): DateTimeFormatterBuilder
    open fun parseStrict(): DateTimeFormatterBuilder
    open fun toFormatter(resolverStyle: ResolverStyle = definedExternally): DateTimeFormatter
}

external open class DecimalStyle : InteropInterface {
    open fun decimalSeparator(): String
    open fun equals(other: InteropInterface): Boolean
    override fun hashCode(): InteropInterface
    open fun negativeSign(): String
    open fun positiveSign(): String
    override fun toString(): String
    open fun zeroDigit(): String
}

external open class ResolverStyle : InteropInterface {
    open fun equals(other: InteropInterface): Boolean
    open fun toJSON(): String
    override fun toString(): String

    companion object {
        var STRICT: ResolverStyle
        var SMART: ResolverStyle
        var LENIENT: ResolverStyle
    }
}

external open class SignStyle : InteropInterface {
    open fun equals(other: InteropInterface): Boolean
    open fun toJSON(): String
    override fun toString(): String

    companion object {
        var NORMAL: SignStyle
        var NEVER: SignStyle
        var ALWAYS: SignStyle
        var EXCEEDS_PAD: SignStyle
        var NOT_NEGATIVE: SignStyle
    }
}

external open class TextStyle : InteropInterface {
    open fun asNormal(): TextStyle
    open fun asStandalone(): TextStyle
    open fun isStandalone(): Boolean
    open fun equals(other: InteropInterface): Boolean
    open fun toJSON(): String
    override fun toString(): String

    companion object {
        var FULL: TextStyle
        var FULL_STANDALONE: TextStyle
        var SHORT: TextStyle
        var SHORT_STANDALONE: TextStyle
        var NARROW: TextStyle
        var NARROW_STANDALONE: TextStyle
    }
}

external open class ParsePosition(index: Int) : InteropInterface {
    open fun getIndex(): Int
    open fun setIndex(index: Int)
    open fun getErrorIndex(): Int
    open fun setErrorIndex(errorIndex: Int)
}

external open class ZoneOffsetTransition : InteropInterface {
    open fun compareTo(transition: ZoneOffsetTransition): Int
    open fun dateTimeAfter(): LocalDateTime
    open fun dateTimeBefore(): LocalDateTime
    open fun duration(): Duration
    open fun durationSeconds(): Int
    open fun equals(other: InteropInterface): Boolean
    override fun hashCode(): Int
    open fun instant(): Instant
    open fun isGap(): Boolean
    open fun isOverlap(): Boolean
    open fun isValidOffset(offset: ZoneOffset): Boolean
    open fun offsetAfter(): ZoneOffset
    open fun offsetBefore(): ZoneOffset
    open fun toEpochSecond(): Double
    override fun toString(): String
//    open fun validOffsets(): JsArray<ZoneOffset>

    companion object {
        fun of(transition: LocalDateTime, offsetBefore: ZoneOffset, offsetAfter: ZoneOffset): ZoneOffsetTransition
    }
}

external interface ZoneOffsetTransitionRule : InteropInterface

external open class ZoneRules : InteropInterface {
    open fun offset(instant: Instant): ZoneOffset
    open fun offset(localDateTime: LocalDateTime): ZoneOffset
    open fun toJSON(): String
    open fun daylightSavings(instant: Instant): Duration
    open fun isDaylightSavings(instant: Instant): Boolean
    open fun isFixedOffset(): Boolean
    open fun isValidOffset(localDateTime: LocalDateTime, offset: ZoneOffset): Boolean
    open fun nextTransition(instant: Instant): ZoneOffsetTransition
    open fun offsetOfEpochMilli(epochMilli: Double): ZoneOffset
    open fun offsetOfInstant(instant: Instant): ZoneOffset
    open fun offsetOfLocalDateTime(localDateTime: LocalDateTime): ZoneOffset
    open fun previousTransition(instant: Instant): ZoneOffsetTransition
    open fun standardOffset(instant: Instant): ZoneOffset
    override fun toString(): String
    open fun transition(localDateTime: LocalDateTime): ZoneOffsetTransition
//    open fun transitionRules(): JsArray<ZoneOffsetTransitionRule>
//    open fun transitions(): JsArray<ZoneOffsetTransition>
//    open fun validOffsets(localDateTime: LocalDateTime): JsArray<ZoneOffset>

    companion object {
        fun of(offest: ZoneOffset): ZoneRules
    }
}

external open class ZoneRulesProvider : InteropInterface {
    companion object {
        fun getRules(zoneId: String): ZoneRules
//        fun getAvailableZoneIds(): JsArray<JsString>
    }
}

external open class IsoChronology : InteropInterface {
    open fun equals(other: InteropInterface): Boolean
    open fun resolveDate(fieldValues: InteropInterface, resolverStyle: InteropInterface): InteropInterface
    override fun toString(): String

    companion object {
        fun isLeapYear(prolepticYear: Int): Boolean
    }
}

external open class ChronoLocalDate : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun format(formatter: DateTimeFormatter): String
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
}

external open class ChronoLocalDateTime : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun chronology(): Chronology
    open fun toEpochSecond(offset: ZoneOffset): Double
    open fun toInstant(offset: ZoneOffset): Instant
}

external open class ChronoZonedDateTime : Temporal {
    open fun compareTo(other: ChronoZonedDateTime): Int
    open fun equals(other: InteropInterface): Boolean
    open fun format(formatter: DateTimeFormatter): String
    open fun isAfter(other: ChronoZonedDateTime): Boolean
    open fun isBefore(other: ChronoZonedDateTime): Boolean
    open fun isEqual(other: ChronoZonedDateTime): Boolean
    open fun toEpochSecond(): Double
    open fun toInstant(): Instant
}

external interface Locale : InteropInterface

external fun nativeJs(date: kotlinx.datetime.Date, zone: ZoneId = definedExternally): TemporalAccessor

external fun nativeJs(date: kotlinx.datetime.Date): TemporalAccessor

external fun nativeJs(date: InteropInterface, zone: ZoneId = definedExternally): TemporalAccessor

external fun nativeJs(date: InteropInterface): TemporalAccessor

external interface `T$0` : InteropInterface {
    var toDate: () -> kotlinx.datetime.Date
    var toEpochMilli: () -> Double
}

external fun convert(temporal: LocalDate, zone: ZoneId = definedExternally): `T$0`

external fun convert(temporal: LocalDate): `T$0`

external fun convert(temporal: LocalDateTime, zone: ZoneId = definedExternally): `T$0`

external fun convert(temporal: LocalDateTime): `T$0`

external fun convert(temporal: ZonedDateTime, zone: ZoneId = definedExternally): `T$0`

external fun convert(temporal: ZonedDateTime): `T$0`

external fun use(plugin: () -> InteropInterface): InteropInterface