/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.cinterop.*
import kotlinx.datetime.internal.*
import platform.posix.*
import platform.windows.*

internal actual class RegionTimeZone(private val tzid: TimeZoneRules, actual override val id: String) : TimeZone() {
    actual companion object {
        actual fun of(zoneId: String): RegionTimeZone = try {
            RegionTimeZone(tzdbInRegistry.rulesForId(zoneId), zoneId)
        } catch (e: Exception) {
            throw IllegalTimeZoneException("Invalid zone ID: $zoneId", e)
        }

        actual fun currentSystemDefault(): RegionTimeZone {
            val zoneId = tzdbInRegistry.currentSystemDefault().second
            return of(zoneId.toString())
        }

        actual val availableZoneIds: Set<String>
            get() = tzdbInRegistry.availableTimeZoneIds()
    }

    actual override fun atStartOfDay(date: LocalDate): Instant = memScoped {
        val ldt = LocalDateTime(date, LocalTime.MIN)
        when (val info = tzid.infoAtDatetime(ldt)) {
            is Regular -> ldt.toInstant(info.offset)
            is Gap -> info.start
            is Overlap -> ldt.toInstant(info.offsetBefore)
        }
    }

    actual override fun atZone(dateTime: LocalDateTime, preferred: UtcOffset?): ZonedDateTime =
            when (val info = tzid.infoAtDatetime(dateTime)) {
                is Regular -> ZonedDateTime(dateTime, this, info.offset)
                is Gap -> {
                    try {
                        ZonedDateTime(dateTime.plusSeconds(info.transitionDurationSeconds), this, info.offsetAfter)
                    } catch (e: IllegalArgumentException) {
                        throw DateTimeArithmeticException(
                                "Overflow whet correcting the date-time to not be in the transition gap",
                                e
                        )
                    }
                }

                is Overlap -> ZonedDateTime(dateTime, this,
                        if (info.offsetAfter == preferred) info.offsetAfter else info.offsetBefore)
            }

    actual override fun offsetAtImpl(instant: Instant): UtcOffset = tzid.infoAtInstant(instant)
}

@SharedImmutable
private val tzdbInRegistry = TzdbInRegistry()

// The timezone cache.
@ThreadLocal
private val cache: MutableMap<String, DYNAMIC_TIME_ZONE_INFORMATION> = mutableMapOf()

internal fun getLastWindowsError(): String = memScoped {
    val buf = alloc<CArrayPointerVar<WCHARVar>>()
    FormatMessage!!(
            (FORMAT_MESSAGE_ALLOCATE_BUFFER or FORMAT_MESSAGE_FROM_SYSTEM or FORMAT_MESSAGE_IGNORE_INSERTS).toUInt(),
            null,
            GetLastError(),
            0u,
            buf.ptr.reinterpret(),
            0u,
            null,
    )
    buf.value!!.toKStringFromUtf16().also { LocalFree(buf.ptr) }
}


private fun offsetAtSystime(time: SYSTEMTIME, zone: DYNAMIC_TIME_ZONE_INFORMATION): UtcOffset = memScoped {
    val yearSpecificRules = alloc<TIME_ZONE_INFORMATION>()
    val result = GetTimeZoneInformationForYear(time.wYear, zone.ptr, yearSpecificRules.ptr)
    check(result != 0) { "Could not query the offset at the given time (err: $result): ${getLastWindowsError()}" }
    offsetAtSystime(time, yearSpecificRules)
}

private fun offsetAtSystime(time: SYSTEMTIME, rules: TIME_ZONE_INFORMATION): UtcOffset {
    var bias = rules.Bias
    bias += if (isDaylightTime(rules, time)) {
        rules.DaylightBias
    } else {
        rules.StandardBias
    }
    return UtcOffset(seconds = -bias * 60)
}

internal actual fun currentTime(): Instant = memScoped {
    val tm = alloc<timespec>()
    val error = clock_gettime(CLOCK_REALTIME, tm.ptr)
    check(error == 0) { "Error when reading the system clock: ${strerror(errno)}" }
    try {
        require(tm.tv_nsec in 0 until NANOS_PER_ONE)
        Instant(tm.tv_sec, tm.tv_nsec)
    } catch (e: IllegalArgumentException) {
        throw IllegalStateException("The readings from the system clock (${tm.tv_sec} seconds, ${tm.tv_nsec} nanoseconds) are not representable as an Instant")
    }
}

private fun LocalDateTime.intoSystemTime(target: SYSTEMTIME) = with(target) {
    wYear = year.toUShort()
    wMonth = monthNumber.toUShort()
    wDay = dayOfMonth.toUShort()
    wDayOfWeek = (if (dayOfWeek == DayOfWeek.SUNDAY) 0 else dayOfWeek.isoDayNumber).toUShort()
    wHour = hour.toUShort()
    wMinute = minute.toUShort()
    wSecond = second.toUShort()
    wMilliseconds = ((nanosecond + NANOS_PER_MILLI / 2) / NANOS_PER_MILLI).toUShort()
}

/* this code is explained at
https://docs.microsoft.com/en-us/windows/win32/api/timezoneapi/ns-timezoneapi-time_zone_information
in the section about `StandardDate`.
In short, the `StandardDate` structure uses `SYSTEMTIME` in a...
non-conventional way. This function translates that representation to one
representing a proper date at a given year.
*/
private fun decodeTransitionDate(year: Int, src: SYSTEMTIME): LocalDateTime {
    // we set seconds and nanoseconds to 0 because they are 0 in practice,
    // but the Windows registry contains some broken data where these values are invalid.
    val localTime = with(src) { LocalTime(wHour.toInt(), wMinute.toInt(), 0, 0) }
    val localDate = if (src.wYear != 0.toUShort()) {
        // if the year is not 0, this is the absolute time.
        with(src) { LocalDate(wYear.toInt(), wMonth.toInt(), wDay.toInt()) }
    } else {
        /* otherwise, the transition happens yearly at the specified month, hour,
        and minute at the specified day of the week. */
        // The number of the occurrence of the specified day of week in the month,
        // or the special value "5" to denote the last such occurrence.
        val dowOccurrenceNumber = src.wDay.toInt()
        val month = Month(src.wMonth.toInt())
        val dayOfWeek = if (src.wDayOfWeek == 0.toUShort()) DayOfWeek.SUNDAY else DayOfWeek(src.wDayOfWeek.toInt())
        val initialDate = LocalDate(year, month, 1)
        val newDate = initialDate.nextDateWithDay(dayOfWeek).plus(dowOccurrenceNumber - 1, DateTimeUnit.WEEK)
        if (newDate.month == month) newDate else newDate.minus(1, DateTimeUnit.WEEK)
    }
    return localDate.atTime(localTime)
}

private fun LocalDate.nextDateWithDay(newDayOfWeek: DayOfWeek) =
        plus((newDayOfWeek.isoDayNumber - this.dayOfWeek.isoDayNumber).mod(7), DateTimeUnit.DAY)

private fun isDaylightTime(out: TIME_ZONE_INFORMATION, systime: SYSTEMTIME): Boolean {
    // it means that daylight saving time is not supported at all
    if (out.StandardDate.wMonth == 0.toUShort())
        return false
    /* translate the "date" values stored in `tzi` into real dates of
       transitions to and from the daylight saving time. */
    val standardLocal = decodeTransitionDate(systime.wYear.toInt(), out.StandardDate)
    val daylightLocal = decodeTransitionDate(systime.wYear.toInt(), out.DaylightDate)
    /* Two things happen here:
    * All the relevant dates are converted to a number of ticks at some
      unified scale, counted in seconds. This is done so that we are able
      to easily add to and compare between dates.
    * `standard_local` and `daylight_local` are represented as dates in the
      local time that was active *just before* the transition. For example,
      `standard_local` contains the date of the transition to the standard
      time, as seen by a person that is currently on the daylight saving
      time. So, in order for the dates to be on the same scale, the biases
      that are assumed to be currently active are negated. */
    val standard = standardLocal.toInstant(UtcOffset(minutes = -(out.Bias + out.DaylightBias)))
    val daylight = daylightLocal.toInstant(UtcOffset(minutes = -(out.Bias + out.StandardBias)))
    val currentTime = systime.toInstant()
    /* Maybe `else` is never hit, but the documentation doesn't say so. */
    return if (daylight < standard) {
        // The year is |STANDARD|DAYLIGHT|STANDARD|, as it should be
        currentTime < standard && currentTime >= daylight
    } else {
        // The year is |DAYLIGHT|STANDARD|DAYLIGHT|
        currentTime < standard || currentTime >= daylight
    }
}

private fun SYSTEMTIME.toInstant(): Instant = decodeTransitionDate(1601, this).toInstant(UtcOffset.ZERO)

private fun offsetAtDatetime(
        zone: DYNAMIC_TIME_ZONE_INFORMATION, dateTime: LocalDateTime, preferred: UtcOffset?, gapHandling: GapHandling
): Pair<LocalDateTime, UtcOffset> = when (val offsetInfo = offsetsAtDatetime(zone, dateTime)) {
    is Regular -> Pair(dateTime, offsetInfo.offset)
    is Overlap ->
        Pair(dateTime, if (offsetInfo.offsetBefore == preferred) offsetInfo.offsetBefore else offsetInfo.offsetAfter)

    is Gap -> when (gapHandling) {
        GapHandling.MOVE_FORWARD -> {
            Pair(dateTime.plusSeconds(offsetInfo.offsetAfter.totalSeconds - offsetInfo.offsetBefore.totalSeconds), offsetInfo.offsetAfter)
        }

        GapHandling.NEXT_CORRECT -> Pair(offsetInfo.start.toLocalDateTime(offsetInfo.offsetBefore), offsetInfo.offsetAfter)
    }
}

private fun offsetsAtDatetime(zone: DYNAMIC_TIME_ZONE_INFORMATION, dateTime: LocalDateTime): OffsetInfo = memScoped {
    val localTime = alloc<SYSTEMTIME>().also { dateTime.intoSystemTime(it) }
    val yearSpecificRules = alloc<TIME_ZONE_INFORMATION>()
    GetTimeZoneInformationForYear(localTime.wYear, zone.ptr, yearSpecificRules.ptr)
    if (yearSpecificRules.DaylightDate.wMonth == 0.toUShort()) {
        return Regular(UtcOffset(minutes = -yearSpecificRules.Bias))
    }
    val dstStart = decodeTransitionDate(dateTime.year, yearSpecificRules.DaylightDate)
    val stdStart = decodeTransitionDate(dateTime.year, yearSpecificRules.StandardDate)
    check(dstStart < stdStart) { "Invalid timezone info readings: DST transition $dstStart occurred before the transition back $stdStart" }
    val standardOffset = UtcOffset(minutes = -(yearSpecificRules.Bias + yearSpecificRules.StandardBias))
    val daylightOffset = UtcOffset(minutes = -(yearSpecificRules.Bias + yearSpecificRules.DaylightBias))
    when {
        dateTime >= dstStart && dateTime < dstStart.plusSeconds(daylightOffset.totalSeconds - standardOffset.totalSeconds) ->
            Gap(dstStart.toInstant(standardOffset), standardOffset, daylightOffset)

        dateTime >= stdStart.plusSeconds(daylightOffset.totalSeconds - standardOffset.totalSeconds) && dateTime < stdStart ->
            Overlap(stdStart.toInstant(daylightOffset), daylightOffset, standardOffset)

        dateTime >= dstStart && dateTime < stdStart -> Regular(daylightOffset)
        else -> Regular(standardOffset)
    }
}

private enum class GapHandling {
    MOVE_FORWARD,
    NEXT_CORRECT,
}