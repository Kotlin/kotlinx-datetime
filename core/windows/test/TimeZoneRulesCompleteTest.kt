/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.datetime.test

import kotlinx.cinterop.*
import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import platform.windows.*
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class TimeZoneRulesCompleteTest {

    /** Tests that all transitions that our system recognizes are actually there. */
    @Test
    fun iterateOverAllTimezones() {
        val tzdb = TzdbInRegistry()
        memScoped {
            val inputSystemtime = alloc<SYSTEMTIME>()
            val outputSystemtime = alloc<SYSTEMTIME>()
            val dtzi = alloc<DYNAMIC_TIME_ZONE_INFORMATION>()
            var i: DWORD = 0u
            while (true) {
                when (val dwResult: Int = EnumDynamicTimeZoneInformation(i++, dtzi.ptr).toInt()) {
                    ERROR_NO_MORE_ITEMS -> break
                    ERROR_SUCCESS -> {
                        val windowsName = dtzi.TimeZoneKeyName.toKString()
                        val id = windowsToStandard[windowsName]
                        if (id == null) {
                            assertTrue(
                                windowsName == "Mid-Atlantic Standard Time" ||
                                windowsName == "Kamchatka Standard Time", windowsName
                            )
                            continue
                        }
                        val rules = tzdb.rulesForId(id)
                        fun checkAtInstant(instant: Instant) {
                            val ldt = instant.toLocalDateTime(dtzi, inputSystemtime.ptr, outputSystemtime.ptr)
                            val offset = rules.infoAtInstant(instant)
                            val ourLdt = instant.toLocalDateTime(offset)
                            assertEquals(ldt, ourLdt, "in zone $windowsName, at $instant (our guess at the offset is $offset)")
                        }
                        fun checkTransition(instant: Instant) {
                            checkAtInstant(instant - 2.milliseconds)
                            checkAtInstant(instant)
                        }
                        // check historical data
                        for (transition in rules.transitionEpochSeconds) {
                            checkTransition(Instant.fromEpochSeconds(transition))
                        }
                        // check recurring rules
                        if (windowsName != "Morocco Standard Time" && windowsName != "West Bank Standard Time") {
                            // we skip checking these two time zones because Windows does something arbitrary with them
                            // after 2030. For example, Morocco DST transitions are linked to the month of Ramadan,
                            // and after 2030, Windows doesn't seem to calculate Ramadan properly, but also, it doesn't
                            // follow the rules stored in the registry. Odd, but it doesn't seem worth it trying to
                            // reverse engineer results that aren't even correct.
                            val lastTransition = Instant.fromEpochSeconds(
                                rules.transitionEpochSeconds.lastOrNull() ?: 1715000000 // arbitrary time
                            )
                            val lastTransitionYear = lastTransition.toLocalDateTime(TimeZone.UTC).year
                            for (year in lastTransitionYear + 1..lastTransitionYear + 15) {
                                val rulesForYear = rules.recurringZoneRules!!.rulesForYear(year)
                                if (rulesForYear.isEmpty()) {
                                    checkAtInstant(
                                        LocalDate(year, 6, 1).atStartOfDayIn(TimeZone.UTC)
                                    )
                                } else {
                                    for (rule in rulesForYear) {
                                        checkTransition(rule.transitionDateTime)
                                    }
                                }
                            }
                        }
                    }
                    else -> error("Unexpected error code $dwResult")
                }
            }
        }
    }
}

private fun Instant.toLocalDateTime(
    tzinfo: DYNAMIC_TIME_ZONE_INFORMATION,
    inputBuffer: CPointer<SYSTEMTIME>,
    outputBuffer: CPointer<SYSTEMTIME>
): LocalDateTime {
    toLocalDateTime(TimeZone.UTC).toSystemTime(inputBuffer)
    SystemTimeToTzSpecificLocalTimeEx(tzinfo.ptr, inputBuffer, outputBuffer)
    return outputBuffer.pointed.toLocalDateTime()
}

private fun LocalDateTime.toSystemTime(outputBuffer: CPointer<SYSTEMTIME>) {
    require(year in 1601..30827)
    outputBuffer.pointed.apply {
        wYear = year.convert()
        wMonth = monthNumber.convert()
        wDay = dayOfMonth.convert()
        wDayOfWeek = (dayOfWeek.isoDayNumber % 7).convert()
        wHour = hour.convert()
        wMinute = minute.convert()
        wSecond = second.convert()
        wMilliseconds = (nanosecond / (NANOS_PER_ONE / MILLIS_PER_ONE)).convert()
    }
}

private fun SYSTEMTIME.toLocalDateTime(): LocalDateTime =
    LocalDateTime(
        year = wYear.convert(),
        monthNumber = wMonth.convert(),
        dayOfMonth = wDay.convert(),
        hour = wHour.convert(),
        minute = wMinute.convert(),
        second = wSecond.convert(),
        nanosecond = wMilliseconds.convert<Int>() * (NANOS_PER_ONE / MILLIS_PER_ONE)
    )
