/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.datetime.internal

import kotlinx.datetime.*
import kotlinx.cinterop.*
import platform.windows.*
import kotlin.experimental.*

internal class TzdbInRegistry: TimeZoneDatabase {

    // TODO: starting version 1703 of Windows 10, the ICU library is also bundled, with more accurate/ timezone information.
    // When Kotlin/Native drops support for Windows 7, we should investigate moving to the ICU.
    private val windowsToRules: Map<String, TimeZoneRules> = buildMap {
        processTimeZonesInRegistry { name, recurring, historic ->
            val recurringRules = RecurringZoneRules(recurring.transitions)
            val rules = run {
                val offsets = mutableListOf<UtcOffset>()
                val transitionEpochSeconds = mutableListOf<Long>()
                for ((year, record) in historic) {
                    when (record) {
                        is PerYearZoneRulesDataWithoutTransitions -> {
                            val lastOffset = offsets.lastOrNull()
                            if (lastOffset != record.standardOffset) {
                                lastOffset?.let {
                                    transitionEpochSeconds.add(START_OF_YEAR.toInstant(year, it).epochSeconds)
                                }
                                offsets.add(record.standardOffset)
                            }
                        }
                        is PerYearZoneRulesDataWithTransitions -> {
                            val toDstTime = record.daylightTransitionTime.toLocalDateTime(year)
                            val toStdTime = record.standardTransitionTime.toLocalDateTime(year)
                            val initialOffset =
                                if (toDstTime < toStdTime) record.standardOffset else record.daylightOffset
                            if (offsets.isEmpty()) { offsets.add(initialOffset) }
                            val newYearTransition: RecurringZoneRules.Rule<MonthDayTime> =
                                RecurringZoneRules.Rule(
                                    transitionDateTime = START_OF_YEAR,
                                    offsetBefore = offsets.last(),
                                    offsetAfter = initialOffset,
                                )
                            // The order is important: newYearTransition must be overwritten by the explicit Jan 1st transition
                            mapOf(
                                LocalDate(year, Month.JANUARY, 1).atTime(0, 0) to newYearTransition,
                                toDstTime to record.daylightTransition,
                                toStdTime to record.standardTransition
                            ).toList().sortedBy(Pair<LocalDateTime, *>::first).forEach { (time, transition) ->
                                // occasionally, there are transitions that have no effect at all, whose purpose is
                                // just to fulfill the contract that there are either two or zero transitions per year.
                                // We skip such transitions entirely to simplify handling.
                                if (offsets.last() != transition.offsetAfter) {
                                    transitionEpochSeconds.add(time.toInstant(transition.offsetBefore).epochSeconds)
                                    offsets.add(transition.offsetAfter)
                                }
                            }
                        }
                    }
                }
                offsets.lastOrNull()?.let { lastOffset ->
                    /* If there are already some offsets, we can not add a new offset without defining a transition to
                    it. The moment when we start using the recurring rules is the first year that does not have any
                    historic data provided. */
                    val firstYearWithRecurringRules = historic.last().first + 1
                    val newYearInLastOffset = LocalDate(firstYearWithRecurringRules, Month.JANUARY, 1).atTime(0, 0)
                        .toInstant(lastOffset)
                    transitionEpochSeconds.add(newYearInLastOffset.epochSeconds)
                }
                offsets.add(recurring.offsetAtYearStart())
                TimeZoneRules(transitionEpochSeconds, offsets, recurringRules)
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
private const val KEY_BUFFER_SIZE = MAX_KEY_LENGTH + 1

private fun processTimeZonesInRegistry(onTimeZone: (String, PerYearZoneRulesData, List<Pair<Int, PerYearZoneRulesData>>) -> Unit) {
    memScoped {
        alloc<HKEYVar>().withRegistryKey(HKEY_LOCAL_MACHINE!!, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Time Zones", { err ->
            throw IllegalStateException("Error while opening the registry to fetch the time zones (err = $err): ${getLastWindowsError()}")
        }) { hKey ->
            var index = 0u
            val cbDataBuffer = alloc<DWORDVar>()
            val zoneInfoBuffer = RegistryTimeZoneInfoBuffer.allocate(this, cbDataBuffer)
            val dwordBuffer = RegistryDwordBuffer.allocate(this, cbDataBuffer)
            val tzHkeyBuffer = alloc<HKEYVar>()
            val dynamicDstHkeyBuffer = alloc<HKEYVar>()
            val windowsTzNameBuffer = allocArray<WCHARVar>(KEY_BUFFER_SIZE)
            val windowsTzNameSizeBuffer = alloc<UIntVar>()
            while (true) {
                windowsTzNameSizeBuffer.value = KEY_BUFFER_SIZE.convert()
                when (RegEnumKeyExW(hKey, index++, windowsTzNameBuffer, windowsTzNameSizeBuffer.ptr, null, null, null, null)) {
                    ERROR_SUCCESS -> {
                        tzHkeyBuffer.withRegistryKey(hKey, windowsTzNameBuffer.toKString(), { err ->
                            throw IllegalStateException(
                                "Error while opening the registry to fetch the time zone '${windowsTzNameBuffer.toKString()} (err = $err)': ${getLastWindowsError()}"
                            )
                        }) { tzHKey ->
                            // first, read the current data from the TZI value
                            val tziRecord = zoneInfoBuffer.readZoneRules(tzHKey, "TZI")
                            val historicData = try {
                                dynamicDstHkeyBuffer.readHistoricDataFromRegistry(tzHKey, dwordBuffer, zoneInfoBuffer)
                            } catch (e: IllegalStateException) {
                                emptyList()
                            }
                            onTimeZone(windowsTzNameBuffer.toKString(), tziRecord, historicData)
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
private fun HKEYVar.readHistoricDataFromRegistry(
    tzHKey: HKEY, dwordBuffer: RegistryDwordBuffer, zoneRulesBuffer: RegistryTimeZoneInfoBuffer
): List<Pair<Int, PerYearZoneRulesData>> =
    withRegistryKey(tzHKey, "Dynamic DST", { emptyList() }) { dynDstHKey ->
        val firstEntry = dwordBuffer.readValue(dynDstHKey, "FirstEntry")
        val lastEntry = dwordBuffer.readValue(dynDstHKey, "LastEntry")
        (firstEntry..lastEntry).map { year ->
            year to zoneRulesBuffer.readZoneRules(dynDstHKey, year.toString())
        }
    }

private inline fun<T> HKEYVar.withRegistryKey(hKey: HKEY, subKeyName: String, onError: (Int) -> T, block: (HKEY) -> T): T {
    val err = RegOpenKeyExW(hKey, subKeyName, 0u, KEY_READ.toUInt(), ptr)
    return if (err != ERROR_SUCCESS) { onError(err) } else {
        try {
            block(value!!)
        } finally {
            RegCloseKey(value)
        }
    }
}

private fun getRegistryValue(
    cbDataBuffer: DWORDVar, buffer: CPointer<BYTEVar>, bufferSize: UInt, hKey: HKEY, valueName: String
) {
    cbDataBuffer.value = bufferSize
    val err = RegQueryValueExW(hKey, valueName, null, null, buffer, cbDataBuffer.ptr)
    check(err == ERROR_SUCCESS) {
        "The expected Windows registry value '$valueName' could not be accessed (err = $err)': ${getLastWindowsError()}"
    }
    check(cbDataBuffer.value.convert<UInt>() == bufferSize) {
        "Expected '$valueName' to have size $bufferSize, but got ${cbDataBuffer.value}"
    }
}

private class RegistryTimeZoneInfoBuffer private constructor(
    private val buffer: CPointer<BYTEVar>,
    private val cbData: DWORDVar,
) {
    /**
     * The data structure is described at
     * https://learn.microsoft.com/en-us/windows/win32/api/timezoneapi/ns-timezoneapi-time_zone_information, as
     * `_REG_TZI_FORMAT`:
     *
     * ```
     * // 16 bytes
     * typedef struct {
     *     uint16_t wYear;
     *     uint16_t wMonth;
     *     uint16_t wDayOfWeek;
     *     uint16_t wDay;
     *     uint16_t wHour;
     *     uint16_t wMinute;
     *     uint16_t wSecond;
     *     uint16_t wMilliseconds;
     * } SYSTEMTIME;
     *
     * // 44 bytes
     * typedef struct _REG_TZI_FORMAT
     * {
     *     int32_t Bias;
     *     int32_t StandardBias;
     *     int32_t DaylightBias;
     *     SYSTEMTIME StandardDate;
     *     SYSTEMTIME DaylightDate;
     * } REG_TZI_FORMAT;
     * ```
     */
    companion object {
        fun allocate(scope: MemScope, cbData: DWORDVar): RegistryTimeZoneInfoBuffer =
            RegistryTimeZoneInfoBuffer(scope.allocArray<BYTEVar>(SIZE_BYTES), cbData)

        private val SIZE_BYTES = 44
    }

    @OptIn(ExperimentalNativeApi::class)
    fun readZoneRules(tzHKey: HKEY, name: String): PerYearZoneRulesData {
        getRegistryValue(cbData, buffer, SIZE_BYTES.convert(), tzHKey, name)
        // convert the buffer to a byte array
        val byteArray = buffer.readBytes(SIZE_BYTES)
        // obtaining raw data
        val bias = byteArray.getIntAt(0)
        val standardBias = byteArray.getIntAt(4)
        val daylightBias = byteArray.getIntAt(8)
        val standardDate = (buffer + 12)!!.reinterpret<SYSTEMTIME>().pointed
        val daylightDate = (buffer + 28)!!.reinterpret<SYSTEMTIME>().pointed
        // calculating the things we're interested in
        if (daylightDate.wMonth == 0.convert<WORD>()) {
            return PerYearZoneRulesDataWithoutTransitions(UtcOffset(minutes = -bias))
        }
        val standardOffset = UtcOffset(minutes = -(standardBias + bias))
        val daylightOffset = UtcOffset(minutes = -(daylightBias + bias))
        val changeToDst = daylightDate.toMonthDayTime()
        val changeToStd = standardDate.toMonthDayTime()
        return PerYearZoneRulesDataWithTransitions(standardOffset, daylightOffset, changeToDst, changeToStd)
    }
}

private class RegistryDwordBuffer private constructor(
    private val buffer: CPointer<DWORDVar>,
    private val cbData: DWORDVar,
) {
    companion object {
        fun allocate(scope: MemScope, cbData: DWORDVar): RegistryDwordBuffer =
            RegistryDwordBuffer(scope.alloc<DWORDVar>().ptr, cbData)
    }

    fun readValue(hKey: HKEY, valueName: String): Int {
        getRegistryValue(cbData, buffer.reinterpret(), sizeOf<DWORDVar>().convert(), hKey, valueName)
        return buffer.pointed.value.toInt()
    }
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

private sealed interface PerYearZoneRulesData {
    val transitions: List<RecurringZoneRules.Rule<MonthDayTime>>
    fun offsetAtYearStart(): UtcOffset
}

private class PerYearZoneRulesDataWithoutTransitions(
    val standardOffset: UtcOffset
) : PerYearZoneRulesData {
    override val transitions: List<RecurringZoneRules.Rule<MonthDayTime>>
        get() = emptyList()

    override fun offsetAtYearStart(): UtcOffset = standardOffset

    override fun toString(): String = "standard offset is $standardOffset"
}

private class PerYearZoneRulesDataWithTransitions(
    val standardOffset: UtcOffset,
    val daylightOffset: UtcOffset,
    val daylightTransitionTime: MonthDayTime,
    val standardTransitionTime: MonthDayTime,
) : PerYearZoneRulesData {
    override val transitions: List<RecurringZoneRules.Rule<MonthDayTime>>
        get() = listOf(daylightTransition, standardTransition)

    val daylightTransition get() =
        RecurringZoneRules.Rule(daylightTransitionTime, offsetBefore = standardOffset, offsetAfter = daylightOffset)

    val standardTransition get() =
        RecurringZoneRules.Rule(standardTransitionTime, offsetBefore = daylightOffset, offsetAfter = standardOffset)

    override fun offsetAtYearStart(): UtcOffset = standardOffset // TODO: not true in all years + all zones

    override fun toString(): String = "standard offset is $standardOffset" +
            ", daylight offset is $daylightOffset" +
            ", the transitions: $daylightTransitionTime, $standardTransitionTime"
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

private val START_OF_YEAR = MonthDayTime(
    date = JulianDayOfYear(0),
    time = MonthDayTime.TransitionLocaltime(0),
    offset = MonthDayTime.OffsetResolver.WallClockOffset,
)
