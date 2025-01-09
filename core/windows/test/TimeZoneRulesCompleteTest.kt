/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.datetime.test

import kotlinx.cinterop.*
import kotlinx.cinterop.ptr
import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import platform.windows.*
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class TimeZoneRulesCompleteTest {

    /** Tests that all transitions that our system recognizes are actually there. */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun iterateOverAllTimezones() {
        val tzdb = TzdbInRegistry()
        memScoped {
            val inputSystemtime = alloc<SYSTEMTIME>()
            val outputSystemtime = alloc<SYSTEMTIME>()
            val dtzi = alloc<DYNAMIC_TIME_ZONE_INFORMATION>()
            fun offsetAtAccordingToWindows(instant: Instant): Int {
                val ldtAccordingToWindows =
                    instant.toLocalDateTime(dtzi, inputSystemtime.ptr, outputSystemtime.ptr)
                return (ldtAccordingToWindows.toInstant(UtcOffset.ZERO) - instant).inWholeSeconds.toInt()
            }
            fun transitionsAccordingToWindows(year: Int): List<OffsetInfo> = buildList {
                var lastInstant = LocalDate(year, Month.JANUARY, 1)
                    .atTime(0, 0).toInstant(UtcOffset.ZERO)
                var lastOffsetAccordingToWindows = offsetAtAccordingToWindows(lastInstant)
                repeat(LocalDate(year, Month.DECEMBER, 31).dayOfYear - 1) {
                    val instant = lastInstant + 24.hours
                    val offset = offsetAtAccordingToWindows(instant)
                    if (lastOffsetAccordingToWindows != offset) {
                        add(OffsetInfo(
                            binarySearchInstant(lastInstant, instant) {
                                offset == offsetAtAccordingToWindows(it)
                            },
                            UtcOffset(seconds = lastOffsetAccordingToWindows),
                            UtcOffset(seconds = offset)
                        ))
                        lastOffsetAccordingToWindows = offset
                    }
                    lastInstant = instant
                }
            }
            val issues = mutableListOf<IncompatibilityWithWindowsRegistry>()
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
                        fun MutableList<Mismatch>.checkAtInstant(instant: Instant) {
                            val ldt = instant.toLocalDateTime(dtzi, inputSystemtime.ptr, outputSystemtime.ptr)
                            val offset = rules.infoAtInstant(instant)
                            val ourLdt = instant.toLocalDateTime(offset)
                            if (ldt != ourLdt) add(Mismatch(ourLdt, ldt, instant))
                        }
                        fun MutableList<Mismatch>.checkTransition(instant: Instant) {
                            checkAtInstant(instant - 2.milliseconds)
                            checkAtInstant(instant)
                        }
                        val mismatches = buildList {
                            // check historical data
                            if (windowsName == "Central Brazilian Standard Time") {
                                // This one reports transitions on Jan 1st for years 1970..2003, but the registry contains transitions
                                // on the first Thursday of January.
                                // Neither of these is correct: https://en.wikipedia.org/wiki/Daylight_saving_time_in_Brazil
                                for (transition in rules.transitionEpochSeconds) {
                                    val instant = Instant.fromEpochSeconds(transition)
                                    if (instant.toLocalDateTime(TimeZone.UTC).year >= 2004) {
                                        checkTransition(instant)
                                    }
                                }
                            } else {
                                for (transition in rules.transitionEpochSeconds) {
                                    checkTransition(Instant.fromEpochSeconds(transition))
                                }
                            }
                            // check recurring rules
                            if (windowsName !in strangeTimeZones) {
                                // we skip checking these time zones because Windows does something arbitrary with them
                                // after 2030. For example, Morocco DST transitions are linked to the month of Ramadan,
                                // and after 2030, Windows doesn't seem to calculate Ramadan properly, but also, it doesn't
                                // follow the rules stored in the registry. Odd, but it doesn't seem worth it trying to
                                // reverse engineer results that aren't even correct.
                                val lastTransitionYear = Instant.fromEpochSeconds(
                                    rules.transitionEpochSeconds.lastOrNull() ?: 1715000000 // arbitrary time
                                ).toLocalDateTime(TimeZone.UTC).year
                                val firstTransitionYear = Instant.fromEpochSeconds(
                                    rules.transitionEpochSeconds.firstOrNull() ?: 0 // arbitrary time
                                ).toLocalDateTime(TimeZone.UTC).year
                                val yearsToCheck = (maxOf(firstTransitionYear - 15, 1970)..<firstTransitionYear) +
                                        ((lastTransitionYear + 1)..(lastTransitionYear + 15))
                                for (year in yearsToCheck) {
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
                        if (mismatches.isNotEmpty()) {
                            val mismatchYears =
                                mismatches.map { it.instant.toLocalDateTime(TimeZone.UTC).year }.distinct()
                            val rawData = memScoped {
                                val hKey = alloc<HKEYVar>()
                                RegOpenKeyExW(HKEY_LOCAL_MACHINE!!, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Time Zones\\$windowsName", 0u, KEY_READ.toUInt(), hKey.ptr)
                                try {
                                    val cbDataBuffer = alloc<DWORDVar>()
                                    val SIZE_BYTES = 44
                                    val zoneInfoBuffer = allocArray<BYTEVar>(SIZE_BYTES)
                                    cbDataBuffer.value = SIZE_BYTES.convert()
                                    RegQueryValueExW(hKey.value, "TZI", null, null, zoneInfoBuffer, cbDataBuffer.ptr)
                                    zoneInfoBuffer.readBytes(SIZE_BYTES).toHexString()
                                } finally {
                                    RegCloseKey(hKey.value)
                                }
                            }
                            issues.add(
                                IncompatibilityWithWindowsRegistry(
                                timeZoneName = windowsName,
                                dataOnAffectedYears = mismatchYears.flatMap {
                                    transitionsAccordingToWindows(it)
                                },
                                registryData = rawData,
                                mismatches = mismatches,
                            )
                            )
                        }
                    }
                    else -> error("Unexpected error code $dwResult")
                }
            }
            if (issues.isNotEmpty()) throw AssertionError(issues.toString())
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

private val strangeTimeZones = listOf(
    // These report transitions in the future that are not in the registry:
    "Morocco Standard Time", "West Bank Standard Time", "Iran Standard Time", "Syria Standard Time",
    "Paraguay Standard Time",
)

private fun binarySearchInstant(instant1: Instant, instant2: Instant, predicate: (Instant) -> Boolean): Instant {
    var low = instant1
    var high = instant2
    while (low < high) {
        val mid = low + (high - low) / 2
        if (predicate(mid)) {
            high = mid
        } else {
            low = mid + 1.milliseconds
        }
    }
    return low
}

private data class IncompatibilityWithWindowsRegistry(
    val timeZoneName: String,
    val dataOnAffectedYears: List<OffsetInfo>,
    val registryData: String,
    val mismatches: List<Mismatch>,
)

private data class Mismatch(
    val ourGuess: LocalDateTime,
    val windowsGuess: LocalDateTime,
    val instant: Instant,
)
