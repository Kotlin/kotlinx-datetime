/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.math.*
import kotlin.test.*
import kotlin.time.*

public actual enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;
}

// This is a function and not a value due to https://github.com/Kotlin/kotlinx-datetime/issues/5
// org.threeten.bp.format.DateTimeFormatterBuilder.InstantPrinterParser#parse
private val instantParser: Parser<Instant>
    get() = localDateParser
        .chainIgnoring(concreteCharParser('T').or(concreteCharParser('t')))
        .chain(intParser(2, 2)) // hour
        .chainIgnoring(concreteCharParser(':'))
        .chain(intParser(2, 2)) // minute
        .chainIgnoring(concreteCharParser(':'))
        .chain(intParser(2, 2)) // second
        .chain(optional(
            concreteCharParser('.')
                .chainSkipping(fractionParser(0, 9, 9)) // nanos
        ))
        .chainIgnoring(concreteCharParser('Z').or(concreteCharParser('z')))
        .map {
            val (dateHourMinuteSecond, nanosVal) = it
            val (dateHourMinute, secondsVal) = dateHourMinuteSecond
            val (dateHour, minutesVal) = dateHourMinute
            val (dateVal, hoursVal) = dateHour

            val nano = nanosVal ?: 0
            val (days, hours, min, seconds) = if (hoursVal == 24 && minutesVal == 0 && secondsVal == 0 && nano == 0) {
                listOf(1, 0, 0, 0)
            } else if (hoursVal == 23 && minutesVal == 59 && secondsVal == 60) {
                // parsed a leap second, but it seems it isn't used
                listOf(0, 23, 59, 59)
            } else {
                listOf(0, hoursVal, minutesVal, secondsVal)
            }

            val localDate = dateVal.withYear(dateVal.year % 10000).plus(days, CalendarUnit.DAY)
            val localTime = LocalTime.of(hours, min, seconds, 0)
            val secDelta: Long = safeMultiply((dateVal.year / 10000).toLong(), SECONDS_PER_10000_YEARS)
            val epochDay: Long = localDate.toEpochDay()
            val instantSecs = epochDay * 86400 + localTime.toSecondOfDay() + secDelta

            Instant(instantSecs, nano)
        }

/**
 * The minimum supported epoch second.
 */
private const val MIN_SECOND = -31557014167219200L

/**
 * The maximum supported epoch second.
 */
private const val MAX_SECOND = 31556889864403199L

@OptIn(ExperimentalTime::class)
public actual class Instant internal constructor(actual val epochSeconds: Long, actual val nanosecondsOfSecond: Int) : Comparable<Instant> {

    init {
        if (epochSeconds < MIN_SECOND || epochSeconds > MAX_SECOND) {
            throw DateTimeException("Instant exceeds minimum or maximum instant")
        }
    }

    // org.threeten.bp.Instant#toEpochMilli
    actual fun toEpochMilliseconds(): Long =
        if (epochSeconds >= 0) {
            val millis: Long = safeMultiply(epochSeconds, MILLIS_PER_ONE.toLong())
            safeAdd(millis, nanosecondsOfSecond / NANOS_PER_MILLI.toLong())
        } else {
            val millis: Long = safeMultiply(epochSeconds + 1, MILLIS_PER_ONE.toLong())
            safeSubtract(millis, MILLIS_PER_ONE.toLong() - nanosecondsOfSecond / NANOS_PER_MILLI)
        }

    // org.threeten.bp.Instant#plus(long, long)
    internal fun plus(secondsToAdd: Long, nanosToAdd: Long): Instant {
        if ((secondsToAdd or nanosToAdd) == 0L) {
            return this
        }
        val epochSec: Long = safeAdd(epochSeconds, secondsToAdd)
        val newEpochSeconds = safeAdd(epochSec, (nanosToAdd / NANOS_PER_ONE))
        val newNanosToAdd = nanosToAdd % NANOS_PER_ONE
        val nanoAdjustment = (nanosecondsOfSecond + newNanosToAdd) // safe int+NANOS_PER_ONE
        return fromEpochSeconds(newEpochSeconds, nanoAdjustment)
    }

    actual operator fun plus(duration: Duration): Instant = duration.toComponents { epochSeconds, nanos ->
        plus(epochSeconds, nanos.toLong())
    }

    actual operator fun minus(duration: Duration): Instant = plus(-duration)

    actual operator fun minus(other: Instant): Duration =
        (this.epochSeconds - other.epochSeconds).seconds + // won't overflow given the instant bounds
            (this.nanosecondsOfSecond - other.nanosecondsOfSecond).nanoseconds

    actual override fun compareTo(other: Instant): Int {
        val s = epochSeconds.compareTo(other.epochSeconds)
        if (s != 0) {
            return s
        }
        return nanosecondsOfSecond.compareTo(other.nanosecondsOfSecond)
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is Instant && this.epochSeconds == other.epochSeconds && this.nanosecondsOfSecond == other.nanosecondsOfSecond

    // org.threeten.bp.Instant#hashCode
    override fun hashCode(): Int =
        (epochSeconds xor (epochSeconds ushr 32)).toInt() + 51 * nanosecondsOfSecond

    // org.threeten.bp.format.DateTimeFormatterBuilder.InstantPrinterParser#print
    override fun toString(): String {
        val buf = StringBuilder()
        val inNano: Int = nanosecondsOfSecond
        if (epochSeconds >= -SECONDS_0000_TO_1970) { // current era
            val zeroSecs: Long = epochSeconds - SECONDS_PER_10000_YEARS + SECONDS_0000_TO_1970
            val hi: Long = floorDiv(zeroSecs, SECONDS_PER_10000_YEARS) + 1
            val lo: Long = floorMod(zeroSecs, SECONDS_PER_10000_YEARS)
            val ldt: LocalDateTime = Instant(lo - SECONDS_0000_TO_1970, 0)
                .toLocalDateTime(TimeZone.UTC)
            if (hi > 0) {
                buf.append('+').append(hi)
            }
            buf.append(ldt)
            if (ldt.second == 0) {
                buf.append(":00")
            }
        } else { // before current era
            val zeroSecs: Long = epochSeconds + SECONDS_0000_TO_1970
            val hi: Long = zeroSecs / SECONDS_PER_10000_YEARS
            val lo: Long = zeroSecs % SECONDS_PER_10000_YEARS
            val ldt: LocalDateTime = Instant(lo - SECONDS_0000_TO_1970, 0)
                .toLocalDateTime(TimeZone.UTC)
            val pos = buf.length
            buf.append(ldt)
            if (ldt.second == 0) {
                buf.append(":00")
            }
            if (hi < 0) {
                when {
                    ldt.year == -10000 -> {
                        buf.deleteCharAt(pos)
                        buf.deleteCharAt(pos)
                        buf.insert(pos, (hi - 1).toString())
                    }
                    lo == 0L -> {
                        buf.insert(pos, hi)
                    }
                    else -> {
                        buf.insert(pos + 1, abs(hi))
                    }
                }
            }
        }
        //fraction
        if (inNano != 0) {
            buf.append('.')
            when {
                inNano % 1000000 == 0 -> {
                    buf.append((inNano / 1000000 + 1000).toString().substring(1))
                }
                inNano % 1000 == 0 -> {
                    buf.append((inNano / 1000 + 1000000).toString().substring(1))
                }
                else -> {
                    buf.append((inNano + 1000000000).toString().substring(1))
                }
            }
        }
        buf.append('Z')
        return buf.toString()
    }

    actual companion object {
        actual fun now(): Instant = memScoped {
            val timespecBuf = alloc<timespec>()
            val error = clock_gettime(CLOCK_REALTIME, timespecBuf.ptr)
            assertEquals(0, error)
            // according to https://en.cppreference.com/w/c/chrono/timespec,
            // tv_nsec in [0; 10^9), so no need to call [ofEpochSecond].
            val seconds = timespecBuf.tv_sec.convert<Long>()
            val nanosec = timespecBuf.tv_nsec.toInt()
            Instant(seconds, nanosec)
        }

        // org.threeten.bp.Instant#ofEpochMilli
        actual fun fromEpochMilliseconds(epochMilliseconds: Long): Instant =
            Instant(floorDiv(epochMilliseconds, MILLIS_PER_ONE.toLong()),
                (floorMod(epochMilliseconds, MILLIS_PER_ONE.toLong()) * NANOS_PER_MILLI).toInt())

        // org.threeten.bp.Instant#ofEpochSecond(long, long)
        actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long): Instant {
            val secs = safeAdd(epochSeconds, floorDiv(nanosecondAdjustment, NANOS_PER_ONE.toLong()))
            val nos = floorMod(nanosecondAdjustment, NANOS_PER_ONE.toLong()).toInt()
            return Instant(secs, nos)
        }

        actual fun parse(isoString: String): Instant =
            instantParser.parse(isoString)
    }

}

actual fun Instant.plus(period: CalendarPeriod, zone: TimeZone): Instant {
    // See [Instant.plus(Instant, long, CalendarUnit, TimeZone)] for an explanation of why time inside day is special
    val seconds = with(period) {
        safeAdd(seconds, safeAdd(
            minutes.toLong() * SECONDS_PER_MINUTE,
            hours.toLong() * SECONDS_PER_HOUR))
    }
    val localDateTime = toZonedLocalDateTime(zone)
    return with(period) {
        localDateTime
            .run { if (years != 0 && months == 0) plusYears(years.toLong()) else this }
            .run { if (months != 0) plusMonths(years * 12L + months.toLong()) else this }
            .run { if (days != 0) plusDays(days.toLong()) else this }
    }.toInstant().plus(seconds, period.nanoseconds)
}

actual fun Instant.plus(value: Int, unit: CalendarUnit, zone: TimeZone): Instant =
    plus(value.toLong(), unit, zone)

actual fun Instant.plus(value: Long, unit: CalendarUnit, zone: TimeZone): Instant =
    when (unit) {
        CalendarUnit.YEAR -> toZonedLocalDateTime(zone).plusYears(value).toInstant()
        CalendarUnit.MONTH -> toZonedLocalDateTime(zone).plusMonths(value).toInstant()
        CalendarUnit.WEEK -> toZonedLocalDateTime(zone).plusDays(value * 7).toInstant()
        CalendarUnit.DAY -> toZonedLocalDateTime(zone).plusDays(value).toInstant()
        /* From org.threeten.bp.ZonedDateTime#plusHours: the time is added to the raw LocalDateTime,
           then org.threeten.bp.ZonedDateTime#create is called on the absolute instant
           (gotten from org.threeten.bp.chrono.ChronoLocalDateTime#toEpochSecond). This, in turn,
           finds an applicable offset and builds the local representation of the zoned datetime.
           If we then feed the result to org.threeten.bp.chrono.ChronoZonedDateTime#toInstant, it simply
           builds the instant with org.threeten.bp.chrono.ChronoZonedDateTime#toEpochSecond, which, once
           more, finds the absolute instant. Thus, we can summarize the composition of `atZone`, `plusHours`
           and `toInstant` like this:
           1. An absolute instant is converted to a zoned datetime by adding an offset;
           2. Time is added to a datetime, ignoring the offset;
           3. Using the know offset, it is converted to an absolute instant;
           4. The instant is adapted to a zoned datetime representation;
           5. The zoned datetime is converted to an absolute instant.
           4 and 5 are invertible by their form: their composition adds and subtracts the offset to and from
           the unix epoch. 1-3 can then be simplified to just adding the time to the instant directly.
         */
        CalendarUnit.HOUR -> plus(safeMultiply(value, SECONDS_PER_HOUR.toLong()), 0)
        CalendarUnit.MINUTE -> plus(safeMultiply(value, SECONDS_PER_MINUTE.toLong()), 0)
        CalendarUnit.SECOND -> plus(value, 0)
        CalendarUnit.NANOSECOND -> plus(0, value)
    }

@OptIn(ExperimentalTime::class)
actual fun Instant.periodUntil(other: Instant, zone: TimeZone): CalendarPeriod {
    var thisLdt = toZonedLocalDateTime(zone)
    val otherLdt = other.toZonedLocalDateTime(zone)

    val months = thisLdt.until(otherLdt, CalendarUnit.MONTH); thisLdt = thisLdt.plusMonths(months)
    val days = thisLdt.until(otherLdt, CalendarUnit.DAY); thisLdt = thisLdt.plusDays(days)
    val time = thisLdt.until(otherLdt, CalendarUnit.NANOSECOND).nanoseconds

    time.toComponents { hours, minutes, seconds, nanoseconds ->
        return CalendarPeriod((months / 12).toInt(), (months % 12).toInt(), days.toInt(), hours, minutes, seconds.toLong(), nanoseconds.toLong())
    }
}

actual fun Instant.until(other: Instant, unit: CalendarUnit, zone: TimeZone): Long =
    toZonedLocalDateTime(zone).until(other.toZonedLocalDateTime(zone), unit)

actual fun Instant.daysUntil(other: Instant, zone: TimeZone): Int = until(other, CalendarUnit.DAY, zone).toInt()
actual fun Instant.monthsUntil(other: Instant, zone: TimeZone): Int = until(other, CalendarUnit.MONTH, zone).toInt()
actual fun Instant.yearsUntil(other: Instant, zone: TimeZone): Int = until(other, CalendarUnit.YEAR, zone).toInt()
