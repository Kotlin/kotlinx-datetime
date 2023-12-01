/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package kotlinx.datetime

import kotlinx.cinterop.*
import kotlinx.datetime.internal.*
import platform.windows.*

internal class TzdbInRegistry: TimezoneDatabase {

    // TODO: starting version 1703 of Windows 10, the ICU library is also bundled, with more accurate/ timezone information.
    // When Kotlin/Native drops support for Windows 7, we should investigate moving to the ICU.
    private val windowsToRules: Map<String, TimeZoneRules> = buildMap {
        processTimeZonesInRegistry { name, recurring, historic ->
            val transitions = recurring.transitions?.let { listOf(it.first, it.second) } ?: emptyList()
            val rules = if (historic.isEmpty()) {
                TimeZoneRules(recurring.standardOffset, RecurringZoneRules(transitions))
            } else {
                val transitionEpochSeconds = mutableListOf<Long>()
                val offsets = mutableListOf(historic[0].second.standardOffset)
                for ((year, record) in historic) {
                    if (record.transitions == null) continue
                    val (trans1, trans2) = record.transitions
                    val transTime1 = trans1.transitionDateTime.toInstant(year, trans1.offsetBefore)
                    val transTime2 = trans2.transitionDateTime.toInstant(year, trans2.offsetBefore)
                    if (transTime2 >= transTime1) {
                        transitionEpochSeconds.add(transTime1.epochSeconds)
                        offsets.add(trans1.offsetAfter)
                        transitionEpochSeconds.add(transTime2.epochSeconds)
                        offsets.add(trans2.offsetAfter)
                    } else {
                        transitionEpochSeconds.add(transTime2.epochSeconds)
                        offsets.add(trans2.offsetAfter)
                        transitionEpochSeconds.add(transTime1.epochSeconds)
                        offsets.add(trans1.offsetAfter)
                    }
                }
                TimeZoneRules(transitionEpochSeconds, offsets, RecurringZoneRules(transitions))
            }
            put(name, rules)
        }
    }

    override fun rulesForId(id: String): TimeZoneRules {
        val standardName = standardToWindows[id] ?: throw IllegalTimeZoneException("Unknown time zone $id")
        return windowsToRules[standardName]
                ?: throw IllegalTimeZoneException("The rules for time zone $id are absent in the Windows registry")
    }

    override fun availableTimeZoneIds(): Set<String> = standardToWindows.filter {
        windowsToRules.containsKey(it.value)
    }.keys

    internal fun currentSystemDefault(): Pair<String, TimeZoneRules> = memScoped {
        val dtzi = alloc<DYNAMIC_TIME_ZONE_INFORMATION>()
        val result = GetDynamicTimeZoneInformation(dtzi.ptr)
        check(result != TIME_ZONE_ID_INVALID) { "The current system time zone is invalid: ${getLastWindowsError()}" }
        val windowsName = dtzi.TimeZoneKeyName.toKStringFromUtf16()
        val ianaTzName = if (windowsName == "Coordinated Universal Time") "UTC" else windowsToStandard[windowsName]
                ?: throw IllegalStateException("Unknown time zone name '$windowsName'")
        val tz = windowsToRules[windowsName]
        check(tz != null) { "The system time zone is set to a value rules for which are not known: '$windowsName'" }
        ianaTzName to if (dtzi.DynamicDaylightTimeDisabled == 0.convert<BOOLEAN>()) {
            tz
        } else {
            // the user explicitly disabled DST transitions, so
            TimeZoneRules(UtcOffset(minutes = -(dtzi.Bias + dtzi.StandardBias)), RecurringZoneRules(emptyList()))
        }
    }
}

/* The maximum length of the registry key name for timezones. Taken from
   https://docs.microsoft.com/en-us/windows/win32/api/timezoneapi/ns-timezoneapi-dynamic_time_zone_information
   */
private const val MAX_KEY_LENGTH = 128

internal fun processTimeZonesInRegistry(onTimeZone: (String, PerYearZoneRulesData, List<Pair<Int, PerYearZoneRulesData>>) -> Unit) {
    memScoped {
        withRegistryKey(HKEY_LOCAL_MACHINE!!, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Time Zones", { err ->
            throw IllegalStateException("Error while opening the registry to fetch the time zones (err = $err): ${getLastWindowsError()}")
        }) { hKey ->
            var index = 0u
            while (true) {
                val windowsTzName = allocArray<WCHARVar>(MAX_KEY_LENGTH + 1)
                val bufSize = alloc<UIntVar>().apply { value = MAX_KEY_LENGTH.toUInt() + 1u }
                when (RegEnumKeyExW(hKey, index++, windowsTzName, bufSize.ptr, null, null, null, null)) {
                    ERROR_SUCCESS -> {
                        withRegistryKey(hKey, windowsTzName.toKString(), { err ->
                            throw IllegalStateException(
                                    "Error while opening the registry to fetch the time zone '${windowsTzName.toKString()} (err = $err)': ${getLastWindowsError()}"
                            )
                        }) { tzHKey ->
                            // first, read the current data from the TZI value
                            val tziRecord = getRegistryValue<REG_TZI_FORMAT>(tzHKey, "TZI")
                            val historicData = try {
                                readHistoricDataFromRegistry(tzHKey)
                            } catch (e: IllegalStateException) {
                                emptyList()
                            }
                            onTimeZone(windowsTzName.toKString(), tziRecord.toZoneRules(), historicData)
                        }
                    }
                    ERROR_MORE_DATA -> throw IllegalStateException("The name of a time zone in the registry was too long")
                    ERROR_NO_MORE_ITEMS -> break
                    else -> throw IllegalStateException("Error when reading a time zone from the registry: ${getLastWindowsError()}")
                }
            }
        }
    }
}

/**
 * Reads the historic data in the "Dynamic DST" subkey, if present.
 *
 * [tzHKey] is an open registry key pointing to the timezone record.
 *
 * Returns pairs of years and the corresponding rules in effect for those years.
 *
 * @throws IllegalStateException if the 'Dynamic DST' key is present but malformed.
 */
private fun MemScope.readHistoricDataFromRegistry(tzHKey: HKEY): List<Pair<Int, PerYearZoneRulesData>> {
    return withRegistryKey(tzHKey, "Dynamic DST", { emptyList() }) { dynDstHKey ->
        val firstEntry = getRegistryValue<DWORDVar>(dynDstHKey, "FirstEntry").value.toInt()
        val lastEntry = getRegistryValue<DWORDVar>(dynDstHKey, "LastEntry").value.toInt()
        (firstEntry..lastEntry).map { year ->
            year to getRegistryValue<REG_TZI_FORMAT>(dynDstHKey, year.toString()).toZoneRules()
        }
    }
}

private inline fun<T> MemScope.withRegistryKey(hKey: HKEY, subKeyName: String, onError: (Int) -> T, block: (HKEY) -> T): T {
    val subHKey: HKEYVar = alloc()
    val err = RegOpenKeyExW(hKey, subKeyName, 0u, KEY_READ.toUInt(), subHKey.ptr)
    return if (err != ERROR_SUCCESS) { onError(err) } else {
        try {
            block(subHKey.value!!)
        } finally {
            RegCloseKey(subHKey.value)
        }
    }
}

private inline fun<reified T: CVariable> MemScope.getRegistryValue(hKey: HKEY, valueName: String): T {
    val buffer = alloc<T>()
    val cbData = alloc<DWORDVar>().apply { value = sizeOf<T>().convert() }
    val err = RegQueryValueExW(hKey, valueName, null, null, buffer.reinterpret<BYTEVar>().ptr, cbData.ptr)
    check(err == ERROR_SUCCESS) { "The expected Windows registry value '$valueName' could not be accessed (err = $err)': ${getLastWindowsError()}" }
    check(cbData.value.toLong() == sizeOf<T>()) { "Expected '$valueName' to have size ${sizeOf<T>()}, but got ${cbData.value}" }
    return buffer
}

private fun _REG_TZI_FORMAT.toZoneRules(): PerYearZoneRulesData {
    val standardOffset = UtcOffset(minutes = -(StandardBias + Bias))
    val daylightOffset = UtcOffset(minutes = -(DaylightBias + Bias))
    if (DaylightDate.wMonth == 0.convert<WORD>()) {
        return PerYearZoneRulesData(standardOffset, null)
    }
    val changeToDst = RecurringZoneRules.Rule(DaylightDate.toMonthDayTime(), standardOffset, daylightOffset)
    val changeToStd = RecurringZoneRules.Rule(StandardDate.toMonthDayTime(), daylightOffset, standardOffset)
    return PerYearZoneRulesData(standardOffset, changeToDst to changeToStd)
}

/* this code is explained at
https://docs.microsoft.com/en-us/windows/win32/api/timezoneapi/ns-timezoneapi-time_zone_information
in the section about `StandardDate`.
In short, the `StandardDate` structure uses `SYSTEMTIME` in a... non-conventional way.
This function translates that representation to one representing a proper date at a given year.
*/
private fun SYSTEMTIME.toMonthDayTime(): MonthDayTime {
    val month = Month(wMonth.toInt())
    val localTime = if (wHour.toInt() == 23 && wMinute.toInt() == 59 && wSecond.toInt() == 59 && wMilliseconds.toInt() == 999) {
        MonthDayTime.TransitionLocaltime(24, 0, 0)
    } else {
        MonthDayTime.TransitionLocaltime(wHour.toInt(), wMinute.toInt(), wSecond.toInt())
    }
    val transitionDay: MonthDayOfYear.TransitionDay = if (wYear != 0.toUShort()) {
        // if the year is not 0, this is the absolute time.
        MonthDayOfYear.TransitionDay.ExactlyDayOfMonth(wDay.toInt())
    } else {
        /* otherwise, the transition happens yearly at the specified month at the specified day of the week. */
        // The number of the occurrence of the specified day of week in the month, or the special value "5" to denote
        // the last such occurrence.
        val dowOccurrenceNumber = wDay.toInt()
        val dayOfWeek = if (wDayOfWeek == 0.toUShort()) DayOfWeek.SUNDAY else DayOfWeek(wDayOfWeek.toInt())
        if (dowOccurrenceNumber == 5) MonthDayOfYear.TransitionDay.Last(dayOfWeek, null)
        else MonthDayOfYear.TransitionDay.Nth(dayOfWeek, dowOccurrenceNumber)
    }
    return MonthDayTime(MonthDayOfYear(month, transitionDay), localTime, MonthDayTime.OffsetResolver.WallClockOffset)
}

internal class PerYearZoneRulesData(
        val standardOffset: UtcOffset,
        val transitions: Pair<RecurringZoneRules.Rule<MonthDayTime>, RecurringZoneRules.Rule<MonthDayTime>>?,
) {
    override fun toString(): String = "standard offset is $standardOffset" + (transitions?.let {
        ", the transitions: ${it.first}, ${it.second}"
    } ?: "")
}

private fun getLastWindowsError(): String = memScoped {
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

internal val tzdbInRegistry = TzdbInRegistry()

internal actual val systemTzdb: TimezoneDatabase = tzdbInRegistry

internal actual fun currentSystemDefaultZone(): RegionTimeZone {
    val (name, zoneRules) = tzdbInRegistry.currentSystemDefault()
    return RegionTimeZone(zoneRules, name)
}
