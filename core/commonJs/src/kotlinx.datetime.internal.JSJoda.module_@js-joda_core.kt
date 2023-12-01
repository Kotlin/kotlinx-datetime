@file:JsModule("@js-joda/core")
@file:JsNonModule
@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE", "INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
package kotlinx.datetime.internal.JSJoda

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

external open class TemporalField {
    open fun isSupportedBy(temporal: TemporalAccessor): Boolean
    open fun isDateBased(): Boolean
    open fun isTimeBased(): Boolean
    open fun baseUnit(): TemporalUnit
    open fun rangeUnit(): TemporalUnit
    open fun range(): ValueRange
    open fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange
    open fun getFrom(temporal: TemporalAccessor): Number
    open fun <R : Temporal> adjustInto(temporal: R, newValue: Number): R
    open fun name(): String
    open fun displayName(): String
    open fun equals(other: Any): Boolean
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

external open class ValueRange {
    open fun checkValidValue(value: Number, field: TemporalField): Number
    open fun checkValidIntValue(value: Number, field: TemporalField): Number
    open fun equals(other: Any): Boolean
    override fun hashCode(): Number
    open fun isFixed(): Boolean
    open fun isIntValue(): Boolean
    open fun isValidIntValue(value: Number): Boolean
    open fun isValidValue(value: Number): Boolean
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

external open class TemporalAmount {
    open fun <T : Temporal> addTo(temporal: T): T
    open fun get(unit: TemporalUnit): Number
    open fun units(): Array<TemporalUnit>
    open fun <T : Temporal> subtractFrom(temporal: T): T
}

external open class TemporalAccessor {
    open fun get(field: TemporalField): Number
    open fun <R> query(query: TemporalQuery<R>): R?
    open fun range(field: TemporalField): ValueRange
    open fun getLong(field: TemporalField): Number
    open fun isSupported(field: TemporalField): Boolean
}

external open class Temporal : TemporalAccessor {
    override fun isSupported(field: TemporalField): Boolean
    open fun isSupported(unit: TemporalUnit): Boolean
    open fun minus(amountToSubtract: Number, unit: TemporalUnit): Temporal
    open fun minus(amount: TemporalAmount): Temporal
    open fun plus(amountToAdd: Number, unit: TemporalUnit): Temporal
    open fun plus(amount: TemporalAmount): Temporal
    open fun until(endTemporal: Temporal, unit: TemporalUnit): Number
    open fun with(adjuster: TemporalAdjuster): Temporal
    open fun with(field: TemporalField, newValue: Number): Temporal
}

external open class TemporalAdjuster {
    open fun adjustInto(temporal: Temporal): Temporal
}

external open class TemporalQuery<R> {
    open fun queryFrom(temporal: TemporalAccessor): R
}

external open class ChronoField : TemporalField {
    override fun isSupportedBy(temporal: TemporalAccessor): Boolean
    override fun baseUnit(): TemporalUnit
    open fun checkValidValue(value: Number): Number
    open fun checkValidIntValue(value: Number): Number
    override fun displayName(): String
    override fun equals(other: Any): Boolean
    override fun getFrom(temporal: TemporalAccessor): Number
    override fun isDateBased(): Boolean
    override fun isTimeBased(): Boolean
    override fun name(): String
    override fun range(): ValueRange
    override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange
    override fun rangeUnit(): TemporalUnit
    override fun <R : Temporal> adjustInto(temporal: R, newValue: Number): R
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

external open class Clock {
    open fun equals(other: Any): Boolean
    open fun instant(): Instant
    open fun millis(): Number
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
    open fun compareTo(otherDuration: Duration): Number
    open fun dividedBy(divisor: Number): Duration
    open fun equals(other: Any): Boolean
    override fun get(unit: TemporalUnit): Number
    open fun isNegative(): Boolean
    open fun isZero(): Boolean
    open fun minus(amount: Number, unit: TemporalUnit): Duration
    open fun minus(duration: Duration): Duration
    open fun minusDays(daysToSubtract: Number): Duration
    open fun minusHours(hoursToSubtract: Number): Duration
    open fun minusMillis(millisToSubtract: Number): Duration
    open fun minusMinutes(minutesToSubtract: Number): Duration
    open fun minusNanos(nanosToSubtract: Number): Duration
    open fun minusSeconds(secondsToSubtract: Number): Duration
    open fun multipliedBy(multiplicand: Number): Duration
    open fun nano(): Number
    open fun negated(): Duration
    open fun plus(amount: Number, unit: TemporalUnit): Duration
    open fun plus(duration: Duration): Duration
    open fun plusDays(daysToAdd: Number): Duration
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
        fun ofSeconds(seconds: Number, nanoAdjustment: Number = definedExternally): Duration
        fun parse(text: String): Duration
    }
}

external open class Instant : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atZone(zone: ZoneId): ZonedDateTime
    open fun compareTo(otherInstant: Instant): Number
    open fun epochSecond(): Number
    open fun equals(other: Any): Boolean
    override fun getLong(field: TemporalField): Number
    override fun hashCode(): Number
    open fun isAfter(otherInstant: Instant): Boolean
    open fun isBefore(otherInstant: Instant): Boolean
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    override fun minus(amount: TemporalAmount): Instant
    override fun minus(amountToSubtract: Number, unit: TemporalUnit): Instant
    open fun minusMillis(millisToSubtract: Number): Instant
    open fun minusNanos(nanosToSubtract: Number): Instant
    open fun minusSeconds(secondsToSubtract: Number): Instant
    open fun nano(): Number
    override fun plus(amount: TemporalAmount): Instant
    override fun plus(amountToAdd: Number, unit: TemporalUnit): Instant
    open fun plusMillis(millisToAdd: Number): Instant
    open fun plusNanos(nanosToAdd: Number): Instant
    open fun plusSeconds(secondsToAdd: Number): Instant
    open fun toEpochMilli(): Number
    open fun toJSON(): String
    override fun toString(): String
    open fun truncatedTo(unit: TemporalUnit): Instant
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Number
    override fun with(adjuster: TemporalAdjuster): Instant
    override fun with(field: TemporalField, newValue: Number): Instant

    companion object {
        var EPOCH: Instant
        var MIN: Instant
        var MAX: Instant
        var MIN_SECONDS: Instant
        var MAX_SECONDS: Instant
        var FROM: TemporalQuery<Instant>
        fun from(temporal: TemporalAccessor): Instant
        fun now(clock: Clock = definedExternally): Instant
        fun ofEpochMilli(epochMilli: Number): Instant
        fun ofEpochSecond(epochSecond: Number, nanoAdjustment: Number = definedExternally): Instant
        fun parse(text: String): Instant
    }
}

external open class LocalDate : ChronoLocalDate {
    open fun atStartOfDay(): LocalDateTime
    open fun atStartOfDay(zone: ZoneId): ZonedDateTime
    open fun atTime(hour: Number, minute: Number, second: Number = definedExternally, nanoOfSecond: Number = definedExternally): LocalDateTime
    open fun atTime(hour: Number, minute: Number): LocalDateTime
    open fun atTime(hour: Number, minute: Number, second: Number = definedExternally): LocalDateTime
    open fun atTime(time: LocalTime): LocalDateTime
    open fun chronology(): Chronology
    open fun compareTo(other: LocalDate): Number
    open fun dayOfMonth(): Number
    open fun dayOfWeek(): DayOfWeek
    open fun dayOfYear(): Number
    open fun equals(other: Any): Boolean
    override fun getLong(field: TemporalField): Number
    override fun hashCode(): Number
    open fun isAfter(other: LocalDate): Boolean
    open fun isBefore(other: LocalDate): Boolean
    open fun isEqual(other: LocalDate): Boolean
    open fun isLeapYear(): Boolean
    open fun isoWeekOfWeekyear(): Number
    open fun isoWeekyear(): Number
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    open fun lengthOfMonth(): Number
    open fun lengthOfYear(): Number
    override fun minus(amount: TemporalAmount): LocalDate
    override fun minus(amountToSubtract: Number, unit: TemporalUnit): LocalDate
    open fun minusDays(daysToSubtract: Number): LocalDate
    open fun minusMonths(monthsToSubtract: Number): LocalDate
    open fun minusWeeks(weeksToSubtract: Number): LocalDate
    open fun minusYears(yearsToSubtract: Number): LocalDate
    open fun month(): Month
    open fun monthValue(): Number
    override fun plus(amount: TemporalAmount): LocalDate
    override fun plus(amountToAdd: Number, unit: TemporalUnit): LocalDate
    open fun plusDays(daysToAdd: Number): LocalDate
    open fun plusMonths(monthsToAdd: Number): LocalDate
    open fun plusWeeks(weeksToAdd: Number): LocalDate
    open fun plusYears(yearsToAdd: Number): LocalDate
    open fun toEpochDay(): Number
    open fun toJSON(): String
    override fun toString(): String
    open fun until(endDate: TemporalAccessor): Period
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Number
    override fun with(adjuster: TemporalAdjuster): LocalDate
    override fun with(field: TemporalField, newValue: Number): LocalDate
    open fun withDayOfMonth(dayOfMonth: Number): LocalDate
    open fun withDayOfYear(dayOfYear: Number): LocalDate
    open fun withMonth(month: Month): LocalDate
    open fun withMonth(month: Number): LocalDate
    open fun withYear(year: Number): LocalDate
    open fun year(): Number

    companion object {
        var MIN: LocalDate
        var MAX: LocalDate
        var EPOCH_0: LocalDate
        var FROM: TemporalQuery<LocalDate>
        fun from(temporal: TemporalAccessor): LocalDate
        fun now(clockOrZone: Clock = definedExternally): LocalDate
        fun now(clockOrZone: ZoneId = definedExternally): LocalDate
        fun of(year: Number, month: Month, dayOfMonth: Number): LocalDate
        fun of(year: Number, month: Number, dayOfMonth: Number): LocalDate
        fun ofEpochDay(epochDay: Number): LocalDate
        fun ofInstant(instant: Instant, zoneId: ZoneId = definedExternally): LocalDate
        fun ofYearDay(year: Number, dayOfYear: Number): LocalDate
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): LocalDate
    }
}

external open class LocalDateTime : ChronoLocalDateTime {
    open fun atOffset(offset: ZoneOffset): OffsetDateTime
    open fun atZone(zone: ZoneId): ZonedDateTime
    open fun compareTo(other: LocalDateTime): Number
    open fun dayOfMonth(): Number
    open fun dayOfWeek(): DayOfWeek
    open fun dayOfYear(): Number
    open fun equals(other: Any): Boolean
    open fun format(formatter: DateTimeFormatter): String
    override fun getLong(field: TemporalField): Number
    override fun hashCode(): Number
    open fun hour(): Number
    open fun isAfter(other: LocalDateTime): Boolean
    open fun isBefore(other: LocalDateTime): Boolean
    open fun isEqual(other: LocalDateTime): Boolean
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    override fun minus(amount: TemporalAmount): LocalDateTime
    override fun minus(amountToSubtract: Number, unit: TemporalUnit): LocalDateTime
    open fun minusDays(days: Number): LocalDateTime
    open fun minusHours(hours: Number): LocalDateTime
    open fun minusMinutes(minutes: Number): LocalDateTime
    open fun minusMonths(months: Number): LocalDateTime
    open fun minusNanos(nanos: Number): LocalDateTime
    open fun minusSeconds(seconds: Number): LocalDateTime
    open fun minusWeeks(weeks: Number): LocalDateTime
    open fun minusYears(years: Number): LocalDateTime
    open fun minute(): Number
    open fun month(): Month
    open fun monthValue(): Number
    open fun nano(): Number
    override fun plus(amount: TemporalAmount): LocalDateTime
    override fun plus(amountToAdd: Number, unit: TemporalUnit): LocalDateTime
    open fun plusDays(days: Number): LocalDateTime
    open fun plusHours(hours: Number): LocalDateTime
    open fun plusMinutes(minutes: Number): LocalDateTime
    open fun plusMonths(months: Number): LocalDateTime
    open fun plusNanos(nanos: Number): LocalDateTime
    open fun plusSeconds(seconds: Number): LocalDateTime
    open fun plusWeeks(weeks: Number): LocalDateTime
    open fun plusYears(years: Number): LocalDateTime
    open fun second(): Number
    open fun toJSON(): String
    open fun toLocalDate(): LocalDate
    open fun toLocalTime(): LocalTime
    override fun toString(): String
    open fun truncatedTo(unit: TemporalUnit): LocalDateTime
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Number
    override fun with(adjuster: TemporalAdjuster): LocalDateTime
    override fun with(field: TemporalField, newValue: Number): LocalDateTime
    open fun withDayOfMonth(dayOfMonth: Number): LocalDateTime
    open fun withDayOfYear(dayOfYear: Number): LocalDateTime
    open fun withHour(hour: Number): LocalDateTime
    open fun withMinute(minute: Number): LocalDateTime
    open fun withMonth(month: Number): LocalDateTime
    open fun withMonth(month: Month): LocalDateTime
    open fun withNano(nanoOfSecond: Number): LocalDateTime
    open fun withSecond(second: Number): LocalDateTime
    open fun withYear(year: Number): LocalDateTime
    open fun year(): Number

    companion object {
        var MIN: LocalDateTime
        var MAX: LocalDateTime
        var FROM: TemporalQuery<LocalDateTime>
        fun from(temporal: TemporalAccessor): LocalDateTime
        fun now(clockOrZone: Clock = definedExternally): LocalDateTime
        fun now(clockOrZone: ZoneId = definedExternally): LocalDateTime
        fun of(date: LocalDate, time: LocalTime): LocalDateTime
        fun of(year: Number, month: Month, dayOfMonth: Number, hour: Number = definedExternally, minute: Number = definedExternally, second: Number = definedExternally, nanoSecond: Number = definedExternally): LocalDateTime
        fun of(year: Number, month: Number, dayOfMonth: Number, hour: Number = definedExternally, minute: Number = definedExternally, second: Number = definedExternally, nanoSecond: Number = definedExternally): LocalDateTime
        fun ofEpochSecond(epochSecond: Number, nanoOfSecond: Number, offset: ZoneOffset): LocalDateTime
        fun ofEpochSecond(epochSecond: Number, offset: ZoneOffset): LocalDateTime
        fun ofInstant(instant: Instant, zoneId: ZoneId = definedExternally): LocalDateTime
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): LocalDateTime
    }
}

external open class LocalTime : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atDate(date: LocalDate): LocalDateTime
    open fun compareTo(other: LocalTime): Number
    open fun equals(other: Any): Boolean
    open fun format(formatter: DateTimeFormatter): String
    open fun getLong(field: ChronoField): Number
    override fun getLong(field: TemporalField): Number
    override fun hashCode(): Number
    open fun hour(): Number
    open fun isAfter(other: LocalTime): Boolean
    open fun isBefore(other: LocalTime): Boolean
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    override fun minus(amount: TemporalAmount): LocalTime
    override fun minus(amountToSubtract: Number, unit: TemporalUnit): LocalTime
    open fun minusHours(hoursToSubtract: Number): LocalTime
    open fun minusMinutes(minutesToSubtract: Number): LocalTime
    open fun minusNanos(nanosToSubtract: Number): LocalTime
    open fun minusSeconds(secondsToSubtract: Number): LocalTime
    open fun minute(): Number
    open fun nano(): Number
    override fun plus(amount: TemporalAmount): LocalTime
    override fun plus(amountToAdd: Number, unit: TemporalUnit): LocalTime
    open fun plusHours(hoursToAdd: Number): LocalTime
    open fun plusMinutes(minutesToAdd: Number): LocalTime
    open fun plusNanos(nanosToAdd: Number): LocalTime
    open fun plusSeconds(secondstoAdd: Number): LocalTime
    open fun second(): Number
    open fun toJSON(): String
    open fun toNanoOfDay(): Number
    open fun toSecondOfDay(): Number
    override fun toString(): String
    open fun truncatedTo(unit: ChronoUnit): LocalTime
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Number
    override fun with(adjuster: TemporalAdjuster): LocalTime
    override fun with(field: TemporalField, newValue: Number): LocalTime
    open fun withHour(hour: Number): LocalTime
    open fun withMinute(minute: Number): LocalTime
    open fun withNano(nanoOfSecond: Number): LocalTime
    open fun withSecond(second: Number): LocalTime

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
        var FROM: TemporalQuery<LocalTime>
        fun from(temporal: TemporalAccessor): LocalTime
        fun now(clockOrZone: Clock = definedExternally): LocalTime
        fun now(clockOrZone: ZoneId = definedExternally): LocalTime
        fun of(hour: Number = definedExternally, minute: Number = definedExternally, second: Number = definedExternally, nanoOfSecond: Number = definedExternally): LocalTime
        fun ofInstant(instant: Instant, zone: ZoneId = definedExternally): LocalTime
        fun ofNanoOfDay(nanoOfDay: Number): LocalTime
        fun ofSecondOfDay(secondOfDay: Number = definedExternally, nanoOfSecond: Number = definedExternally): LocalTime
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): LocalTime
    }
}

external open class MonthDay : TemporalAccessor {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atYear(year: Number): LocalDate
    open fun compareTo(other: MonthDay): Number
    open fun dayOfMonth(): Number
    open fun equals(other: Any): Boolean
    open fun format(formatter: DateTimeFormatter): String
    override fun getLong(field: TemporalField): Number
    open fun isAfter(other: MonthDay): Boolean
    open fun isBefore(other: MonthDay): Boolean
    override fun isSupported(field: TemporalField): Boolean
    open fun isValidYear(year: Number): Boolean
    open fun month(): Month
    open fun monthValue(): Number
    open fun toJSON(): String
    override fun toString(): String
    open fun with(month: Month): MonthDay
    open fun withDayOfMonth(dayOfMonth: Number): MonthDay
    open fun withMonth(month: Number): MonthDay

    companion object {
        var FROM: TemporalQuery<MonthDay>
        fun from(temporal: TemporalAccessor): MonthDay
        fun now(zoneIdOrClock: ZoneId = definedExternally): MonthDay
        fun now(zoneIdOrClock: Clock = definedExternally): MonthDay
        fun of(month: Month, dayOfMonth: Number): MonthDay
        fun of(month: Number, dayOfMonth: Number): MonthDay
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): MonthDay
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
        fun from(amount: TemporalAmount): Period
        fun of(years: Number, months: Number, days: Number): Period
        fun ofDays(days: Number): Period
        fun ofMonths(months: Number): Period
        fun ofWeeks(weeks: Number): Period
        fun ofYears(years: Number): Period
        fun parse(text: String): Period
    }
}

external open class Year : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atDay(dayOfYear: Number): LocalDate
    open fun atMonth(month: Month): YearMonth
    open fun atMonth(month: Number): YearMonth
    open fun atMonthDay(monthDay: MonthDay): LocalDate
    open fun compareTo(other: Year): Number
    open fun equals(other: Any): Boolean
    override fun getLong(field: TemporalField): Number
    open fun isAfter(other: Year): Boolean
    open fun isBefore(other: Year): Boolean
    open fun isLeap(): Boolean
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    open fun isValidMonthDay(monthDay: MonthDay): Boolean
    open fun length(): Number
    override fun minus(amount: TemporalAmount): Year
    override fun minus(amountToSubtract: Number, unit: TemporalUnit): Year
    open fun minusYears(yearsToSubtract: Number): Year
    override fun plus(amount: TemporalAmount): Year
    override fun plus(amountToAdd: Number, unit: TemporalUnit): Year
    open fun plusYears(yearsToAdd: Number): Year
    open fun toJSON(): String
    override fun toString(): String
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Number
    open fun value(): Number
    override fun with(adjuster: TemporalAdjuster): Year
    override fun with(field: TemporalField, newValue: Number): Year

    companion object {
        var MIN_VALUE: Number
        var MAX_VALUE: Number
        var FROM: TemporalQuery<Year>
        fun from(temporal: TemporalAccessor): Year
        fun isLeap(year: Number): Boolean
        fun now(zoneIdOrClock: ZoneId = definedExternally): Year
        fun now(zoneIdOrClock: Clock = definedExternally): Year
        fun of(isoYear: Number): Year
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): Year
    }
}

external open class YearMonth : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atDay(dayOfMonth: Number): LocalDate
    open fun atEndOfMonth(): LocalDate
    open fun compareTo(other: YearMonth): Number
    open fun equals(other: Any): Boolean
    open fun format(formatter: DateTimeFormatter): String
    override fun getLong(field: TemporalField): Number
    open fun isAfter(other: YearMonth): Boolean
    open fun isBefore(other: YearMonth): Boolean
    open fun isLeapYear(): Boolean
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    open fun isValidDay(): Boolean
    open fun lengthOfMonth(): Number
    open fun lengthOfYear(): Number
    override fun minus(amount: TemporalAmount): YearMonth
    override fun minus(amountToSubtract: Number, unit: TemporalUnit): YearMonth
    open fun minusMonths(monthsToSubtract: Number): YearMonth
    open fun minusYears(yearsToSubtract: Number): YearMonth
    open fun month(): Month
    open fun monthValue(): Number
    override fun plus(amount: TemporalAmount): YearMonth
    override fun plus(amountToAdd: Number, unit: TemporalUnit): YearMonth
    open fun plusMonths(monthsToAdd: Number): YearMonth
    open fun plusYears(yearsToAdd: Number): YearMonth
    open fun toJSON(): String
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Number
    override fun with(adjuster: TemporalAdjuster): YearMonth
    override fun with(field: TemporalField, newValue: Number): YearMonth
    open fun withMonth(month: Number): YearMonth
    open fun withYear(year: Number): YearMonth
    open fun year(): Number

    companion object {
        var FROM: TemporalQuery<YearMonth>
        fun from(temporal: TemporalAccessor): YearMonth
        fun now(zoneIdOrClock: ZoneId = definedExternally): YearMonth
        fun now(zoneIdOrClock: Clock = definedExternally): YearMonth
        fun of(year: Number, monthOrNumber: Month): YearMonth
        fun of(year: Number, monthOrNumber: Number): YearMonth
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): YearMonth
    }
}

external open class OffsetDateTime : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atZoneSameInstant(zone: ZoneId): ZonedDateTime
    open fun atZoneSimilarLocal(zone: ZoneId): ZonedDateTime
    open fun compareTo(other: OffsetDateTime): Number
    open fun equals(obj: Any): Boolean
    open fun format(formatter: DateTimeFormatter): String
    override fun get(field: TemporalField): Number
    open fun dayOfMonth(): Number
    open fun dayOfWeek(): DayOfWeek
    open fun dayOfYear(): Number
    open fun hour(): Number
    override fun getLong(field: TemporalField): Number
    open fun minute(): Number
    open fun month(): Month
    open fun monthValue(): Number
    open fun nano(): Number
    open fun offset(): ZoneOffset
    open fun second(): Number
    open fun year(): Number
    override fun hashCode(): Number
    open fun isAfter(other: OffsetDateTime): Boolean
    open fun isBefore(other: OffsetDateTime): Boolean
    open fun isEqual(other: OffsetDateTime): Boolean
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    override fun minus(amountToSubtract: Number, unit: TemporalUnit): OffsetDateTime
    override fun minus(amountToSubtract: TemporalAmount): OffsetDateTime
    open fun minusDays(days: Number): OffsetDateTime
    open fun minusHours(hours: Number): OffsetDateTime
    open fun minusMinutes(minutes: Number): OffsetDateTime
    open fun minusMonths(months: Number): OffsetDateTime
    open fun minusNanos(nanos: Number): OffsetDateTime
    open fun minusSeconds(seconds: Number): OffsetDateTime
    open fun minusWeeks(weeks: Number): OffsetDateTime
    open fun minusYears(years: Number): OffsetDateTime
    override fun plus(amountToAdd: Number, unit: TemporalUnit): OffsetDateTime
    override fun plus(amountToAdd: TemporalAmount): OffsetDateTime
    open fun plusDays(days: Number): OffsetDateTime
    open fun plusHours(hours: Number): OffsetDateTime
    open fun plusMinutes(minutes: Number): OffsetDateTime
    open fun plusMonths(months: Number): OffsetDateTime
    open fun plusNanos(nanos: Number): OffsetDateTime
    open fun plusSeconds(seconds: Number): OffsetDateTime
    open fun plusWeeks(weeks: Number): OffsetDateTime
    open fun plusYears(years: Number): OffsetDateTime
    override fun <R> query(query: TemporalQuery<R>): R?
    override fun range(field: TemporalField): ValueRange
    open fun toEpochSecond(): Number
    open fun toJSON(): String
    open fun toInstant(): Instant
    open fun toLocalDate(): LocalDate
    open fun toLocalDateTime(): LocalDateTime
    open fun toLocalTime(): LocalTime
    open fun toOffsetTime(): OffsetTime
    override fun toString(): String
    open fun truncatedTo(unit: TemporalUnit): OffsetDateTime
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Number
    override fun with(adjuster: TemporalAdjuster): OffsetDateTime
    override fun with(field: TemporalField, newValue: Number): OffsetDateTime
    open fun withDayOfMonth(dayOfMonth: Number): OffsetDateTime
    open fun withDayOfYear(dayOfYear: Number): OffsetDateTime
    open fun withHour(hour: Number): OffsetDateTime
    open fun withMinute(minute: Number): OffsetDateTime
    open fun withMonth(month: Number): OffsetDateTime
    open fun withNano(nanoOfSecond: Number): OffsetDateTime
    open fun withOffsetSameInstant(offset: ZoneOffset): OffsetDateTime
    open fun withOffsetSameLocal(offset: ZoneOffset): OffsetDateTime
    open fun withSecond(second: Number): OffsetDateTime
    open fun withYear(year: Number): OffsetDateTime

    companion object {
        var MIN: OffsetDateTime
        var MAX: OffsetDateTime
        var FROM: TemporalQuery<OffsetDateTime>
        fun from(temporal: TemporalAccessor): OffsetDateTime
        fun now(clockOrZone: Clock = definedExternally): OffsetDateTime
        fun now(clockOrZone: ZoneId = definedExternally): OffsetDateTime
        fun of(dateTime: LocalDateTime, offset: ZoneOffset): OffsetDateTime
        fun of(date: LocalDate, time: LocalTime, offset: ZoneOffset): OffsetDateTime
        fun of(year: Number, month: Number, day: Number, hour: Number, minute: Number, second: Number, nanoOfSecond: Number, offset: ZoneOffset): OffsetDateTime
        fun ofInstant(instant: Instant, zone: ZoneId): OffsetDateTime
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): OffsetDateTime
    }
}

external open class OffsetTime : Temporal {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun atDate(date: LocalDate): OffsetDateTime
    open fun compareTo(other: OffsetTime): Number
    open fun equals(other: Any): Boolean
    open fun format(formatter: DateTimeFormatter): String
    override fun get(field: TemporalField): Number
    open fun hour(): Number
    override fun getLong(field: TemporalField): Number
    open fun minute(): Number
    open fun nano(): Number
    open fun offset(): ZoneOffset
    open fun second(): Number
    override fun hashCode(): Number
    open fun isAfter(other: OffsetTime): Boolean
    open fun isBefore(other: OffsetTime): Boolean
    open fun isEqual(other: OffsetTime): Boolean
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    override fun minus(amountToSubtract: Number, unit: TemporalUnit): OffsetTime
    override fun minus(amountToSubtract: TemporalAmount): OffsetTime
    open fun minusHours(hours: Number): OffsetTime
    open fun minusMinutes(minutes: Number): OffsetTime
    open fun minusNanos(nanos: Number): OffsetTime
    open fun minusSeconds(seconds: Number): OffsetTime
    override fun plus(amountToAdd: Number, unit: TemporalUnit): OffsetTime
    override fun plus(amountToAdd: TemporalAmount): OffsetTime
    open fun plusHours(hours: Number): OffsetTime
    open fun plusMinutes(minutes: Number): OffsetTime
    open fun plusNanos(nanos: Number): OffsetTime
    open fun plusSeconds(seconds: Number): OffsetTime
    override fun <R> query(query: TemporalQuery<R>): R?
    override fun range(field: TemporalField): ValueRange
    open fun toEpochSecond(date: LocalDate): Number
    open fun toJSON(): String
    open fun toLocalTime(): LocalTime
    override fun toString(): String
    open fun truncatedTo(unit: TemporalUnit): OffsetTime
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Number
    override fun with(adjuster: TemporalAdjuster): OffsetTime
    override fun with(field: TemporalField, newValue: Number): OffsetTime
    open fun withHour(hour: Number): OffsetTime
    open fun withMinute(minute: Number): OffsetTime
    open fun withNano(nanoOfSecond: Number): OffsetTime
    open fun withOffsetSameInstant(offset: ZoneOffset): OffsetTime
    open fun withOffsetSameLocal(offset: ZoneOffset): OffsetTime
    open fun withSecond(second: Number): OffsetTime

    companion object {
        var MIN: OffsetTime
        var MAX: OffsetTime
        var FROM: TemporalQuery<OffsetTime>
        fun from(temporal: TemporalAccessor): OffsetTime
        fun now(clockOrZone: Clock = definedExternally): OffsetTime
        fun now(clockOrZone: ZoneId = definedExternally): OffsetTime
        fun of(time: LocalTime, offset: ZoneOffset): OffsetTime
        fun of(hour: Number, minute: Number, second: Number, nanoOfSecond: Number, offset: ZoneOffset): OffsetTime
        fun ofInstant(instant: Instant, zone: ZoneId): OffsetTime
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): OffsetTime
    }
}

external open class ZonedDateTime : ChronoZonedDateTime {
    open fun dayOfMonth(): Number
    open fun dayOfWeek(): DayOfWeek
    open fun dayOfYear(): Number
    override fun equals(other: Any): Boolean
    override fun format(formatter: DateTimeFormatter): String
    override fun getLong(field: TemporalField): Number
    override fun hashCode(): Number
    open fun hour(): Number
    override fun isSupported(fieldOrUnit: TemporalField): Boolean
    override fun isSupported(fieldOrUnit: TemporalUnit): Boolean
    override fun minus(amount: TemporalAmount): ZonedDateTime
    override fun minus(amountToSubtract: Number, unit: TemporalUnit): ZonedDateTime
    open fun minusDays(days: Number): ZonedDateTime
    open fun minusHours(hours: Number): ZonedDateTime
    open fun minusMinutes(minutes: Number): ZonedDateTime
    open fun minusMonths(months: Number): ZonedDateTime
    open fun minusNanos(nanos: Number): ZonedDateTime
    open fun minusSeconds(seconds: Number): ZonedDateTime
    open fun minusWeeks(weeks: Number): ZonedDateTime
    open fun minusYears(years: Number): ZonedDateTime
    open fun minute(): Number
    open fun month(): Month
    open fun monthValue(): Number
    open fun nano(): Number
    open fun offset(): ZoneOffset
    override fun plus(amount: TemporalAmount): ZonedDateTime
    override fun plus(amountToAdd: Number, unit: TemporalUnit): ZonedDateTime
    open fun plusDays(days: Number): ZonedDateTime
    open fun plusHours(hours: Number): ZonedDateTime
    open fun plusMinutes(minutes: Number): ZonedDateTime
    open fun plusMonths(months: Number): ZonedDateTime
    open fun plusNanos(nanos: Number): ZonedDateTime
    open fun plusSeconds(seconds: Number): ZonedDateTime
    open fun plusWeeks(weeks: Number): ZonedDateTime
    open fun plusYears(years: Number): ZonedDateTime
    override fun range(field: TemporalField): ValueRange
    open fun second(): Number
    open fun toJSON(): String
    open fun toLocalDate(): LocalDate
    open fun toLocalDateTime(): LocalDateTime
    open fun toLocalTime(): LocalTime
    open fun toOffsetDateTime(): OffsetDateTime
    override fun toString(): String
    open fun truncatedTo(unit: TemporalUnit): ZonedDateTime
    override fun until(endExclusive: Temporal, unit: TemporalUnit): Number
    override fun with(adjuster: TemporalAdjuster): ZonedDateTime
    override fun with(field: TemporalField, newValue: Number): ZonedDateTime
    open fun withDayOfMonth(dayOfMonth: Number): ZonedDateTime
    open fun withDayOfYear(dayOfYear: Number): ZonedDateTime
    open fun withEarlierOffsetAtOverlap(): ZonedDateTime
    open fun withFixedOffsetZone(): ZonedDateTime
    open fun withHour(hour: Number): ZonedDateTime
    open fun withLaterOffsetAtOverlap(): ZonedDateTime
    open fun withMinute(minute: Number): ZonedDateTime
    open fun withMonth(month: Number): ZonedDateTime
    open fun withNano(nanoOfSecond: Number): ZonedDateTime
    open fun withSecond(second: Number): ZonedDateTime
    open fun withYear(year: Number): ZonedDateTime
    open fun withZoneSameInstant(zone: ZoneId): ZonedDateTime
    open fun withZoneSameLocal(zone: ZoneId): ZonedDateTime
    open fun year(): Number
    open fun zone(): ZoneId

    companion object {
        var FROM: TemporalQuery<ZonedDateTime>
        fun from(temporal: TemporalAccessor): ZonedDateTime
        fun now(clockOrZone: Clock = definedExternally): ZonedDateTime
        fun now(clockOrZone: ZoneId = definedExternally): ZonedDateTime
        fun of(localDateTime: LocalDateTime, zone: ZoneId): ZonedDateTime
        fun of(date: LocalDate, time: LocalTime, zone: ZoneId): ZonedDateTime
        fun of(year: Number, month: Number, dayOfMonth: Number, hour: Number, minute: Number, second: Number, nanoOfSecond: Number, zone: ZoneId): ZonedDateTime
        fun ofInstant(instant: Instant, zone: ZoneId): ZonedDateTime
        fun ofInstant(localDateTime: LocalDateTime, offset: ZoneOffset, zone: ZoneId): ZonedDateTime
        fun ofLocal(localDateTime: LocalDateTime, zone: ZoneId, preferredOffset: ZoneOffset? = definedExternally): ZonedDateTime
        fun ofStrict(localDateTime: LocalDateTime, offset: ZoneOffset, zone: ZoneId): ZonedDateTime
        fun parse(text: String, formatter: DateTimeFormatter = definedExternally): ZonedDateTime
    }
}

external open class ZoneId {
    open fun equals(other: Any): Boolean
    override fun hashCode(): Number
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

external open class DayOfWeek : TemporalAccessor {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun compareTo(other: DayOfWeek): Number
    open fun equals(other: Any): Boolean
    open fun displayName(style: TextStyle, locale: Locale): String
    override fun getLong(field: TemporalField): Number
    override fun isSupported(field: TemporalField): Boolean
    open fun minus(days: Number): DayOfWeek
    open fun name(): String
    open fun ordinal(): Number
    open fun plus(days: Number): DayOfWeek
    open fun toJSON(): String
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
        var FROM: TemporalQuery<DayOfWeek>
        fun from(temporal: TemporalAccessor): DayOfWeek
        fun of(dayOfWeek: Number): DayOfWeek
        fun valueOf(name: String): DayOfWeek
        fun values(): Array<DayOfWeek>
    }
}

external open class Month : TemporalAccessor {
    open fun adjustInto(temporal: Temporal): Temporal
    open fun compareTo(other: Month): Number
    open fun equals(other: Any): Boolean
    open fun firstDayOfYear(leapYear: Boolean): Number
    open fun firstMonthOfQuarter(): Month
    open fun displayName(style: TextStyle, locale: Locale): String
    override fun getLong(field: TemporalField): Number
    override fun isSupported(field: TemporalField): Boolean
    open fun length(leapYear: Boolean): Number
    open fun maxLength(): Number
    open fun minLength(): Number
    open fun minus(months: Number): Month
    open fun name(): String
    open fun ordinal(): Number
    open fun plus(months: Number): Month
    open fun toJSON(): String
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

external open class DateTimeFormatter {
    open fun chronology(): Chronology?
    open fun decimalStyle(): DecimalStyle
    open fun format(temporal: TemporalAccessor): String
    open fun locale(): Any
    open fun parse(text: String): TemporalAccessor
    open fun <T> parse(text: String, query: TemporalQuery<T>): T
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
        fun parsedLeapSecond(): TemporalQuery<Boolean>
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
    open fun appendValue(field: TemporalField, width: Number = definedExternally, maxWidth: Number = definedExternally, signStyle: SignStyle = definedExternally): DateTimeFormatterBuilder
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
    open fun toFormatter(resolverStyle: ResolverStyle = definedExternally): DateTimeFormatter
}

external open class DecimalStyle {
    open fun decimalSeparator(): String
    open fun equals(other: Any): Boolean
    override fun hashCode(): Any
    open fun negativeSign(): String
    open fun positiveSign(): String
    override fun toString(): String
    open fun zeroDigit(): String
}

external open class ResolverStyle {
    open fun equals(other: Any): Boolean
    open fun toJSON(): String
    override fun toString(): String

    companion object {
        var STRICT: ResolverStyle
        var SMART: ResolverStyle
        var LENIENT: ResolverStyle
    }
}

external open class SignStyle {
    open fun equals(other: Any): Boolean
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

external open class TextStyle {
    open fun asNormal(): TextStyle
    open fun asStandalone(): TextStyle
    open fun isStandalone(): Boolean
    open fun equals(other: Any): Boolean
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

external open class ParsePosition(index: Number) {
    open fun getIndex(): Number
    open fun setIndex(index: Number)
    open fun getErrorIndex(): Number
    open fun setErrorIndex(errorIndex: Number)
}

external open class ZoneOffsetTransition {
    open fun compareTo(transition: ZoneOffsetTransition): Number
    open fun dateTimeAfter(): LocalDateTime
    open fun dateTimeBefore(): LocalDateTime
    open fun duration(): Duration
    open fun durationSeconds(): Number
    open fun equals(other: Any): Boolean
    override fun hashCode(): Number
    open fun instant(): Instant
    open fun isGap(): Boolean
    open fun isOverlap(): Boolean
    open fun isValidOffset(offset: ZoneOffset): Boolean
    open fun offsetAfter(): ZoneOffset
    open fun offsetBefore(): ZoneOffset
    open fun toEpochSecond(): Number
    override fun toString(): String
    open fun validOffsets(): Array<ZoneOffset>

    companion object {
        fun of(transition: LocalDateTime, offsetBefore: ZoneOffset, offsetAfter: ZoneOffset): ZoneOffsetTransition
    }
}

external interface ZoneOffsetTransitionRule

external open class ZoneRules {
    open fun offset(instant: Instant): ZoneOffset
    open fun offset(localDateTime: LocalDateTime): ZoneOffset
    open fun toJSON(): String
    open fun daylightSavings(instant: Instant): Duration
    open fun isDaylightSavings(instant: Instant): Boolean
    open fun isFixedOffset(): Boolean
    open fun isValidOffset(localDateTime: LocalDateTime, offset: ZoneOffset): Boolean
    open fun nextTransition(instant: Instant): ZoneOffsetTransition
    open fun offsetOfEpochMilli(epochMilli: Number): ZoneOffset
    open fun offsetOfInstant(instant: Instant): ZoneOffset
    open fun offsetOfLocalDateTime(localDateTime: LocalDateTime): ZoneOffset
    open fun previousTransition(instant: Instant): ZoneOffsetTransition
    open fun standardOffset(instant: Instant): ZoneOffset
    override fun toString(): String
    open fun transition(localDateTime: LocalDateTime): ZoneOffsetTransition
    open fun transitionRules(): Array<ZoneOffsetTransitionRule>
    open fun transitions(): Array<ZoneOffsetTransition>
    open fun validOffsets(localDateTime: LocalDateTime): Array<ZoneOffset>

    companion object {
        fun of(offest: ZoneOffset): ZoneRules
    }
}

external open class ZoneRulesProvider {
    companion object {
        fun getRules(zoneId: String): ZoneRules
        fun getAvailableZoneIds(): Array<String>
    }
}

external open class IsoChronology {
    open fun equals(other: Any): Boolean
    open fun resolveDate(fieldValues: Any, resolverStyle: Any): Any
    override fun toString(): String

    companion object {
        fun isLeapYear(prolepticYear: Number): Boolean
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
    open fun toEpochSecond(offset: ZoneOffset): Number
    open fun toInstant(offset: ZoneOffset): Instant
}

external open class ChronoZonedDateTime : Temporal {
    open fun compareTo(other: ChronoZonedDateTime): Number
    open fun equals(other: Any): Boolean
    open fun format(formatter: DateTimeFormatter): String
    open fun isAfter(other: ChronoZonedDateTime): Boolean
    open fun isBefore(other: ChronoZonedDateTime): Boolean
    open fun isEqual(other: ChronoZonedDateTime): Boolean
    open fun toEpochSecond(): Number
    open fun toInstant(): Instant
}

external interface Locale

external fun nativeJs(date: Date, zone: ZoneId = definedExternally): TemporalAccessor

external fun nativeJs(date: Date): TemporalAccessor

external fun nativeJs(date: Any, zone: ZoneId = definedExternally): TemporalAccessor

external fun nativeJs(date: Any): TemporalAccessor

external interface `T$0` {
    var toDate: () -> Date
    var toEpochMilli: () -> Number
}

external fun convert(temporal: LocalDate, zone: ZoneId = definedExternally): `T$0`

external fun convert(temporal: LocalDate): `T$0`

external fun convert(temporal: LocalDateTime, zone: ZoneId = definedExternally): `T$0`

external fun convert(temporal: LocalDateTime): `T$0`

external fun convert(temporal: ZonedDateTime, zone: ZoneId = definedExternally): `T$0`

external fun convert(temporal: ZonedDateTime): `T$0`

external fun use(plugin: Function<*>): Any