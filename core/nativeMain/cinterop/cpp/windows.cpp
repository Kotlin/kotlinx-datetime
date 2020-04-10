#if WIN32
/* only Windows 8 and later is supported. This is needed for
   `EnumDynamicTimeZoneInformation` to be available. */
#define _WIN32_WINNT _WIN32_WINNT_WIN8
// avoid bloat from including `Windows.h`.
#ifndef WIN32_LEAN_AND_MEAN
#    define WIN32_LEAN_AND_MEAN
#endif

#include <Windows.h>
#include <Timezoneapi.h>
#include <stdexcept>
#include <string>
#include <cstring>
#include <set>
#ifdef DEBUG
#include <iostream>
#endif
#include <chrono>
#include <shared_mutex>
#include "date/date.h"
#include "windows_zones.hpp"

#define MAX_KEY_LENGTH 128

// from the `date` library, function `getTimeZoneKeyName()`.
static std::string copy_name(const DYNAMIC_TIME_ZONE_INFORMATION& dtzi) {
    auto wlen = wcslen(dtzi.TimeZoneKeyName);
    char buf[MAX_KEY_LENGTH] = {};
    if (sizeof(buf) < wlen+1) {
        throw std::runtime_error("Anomalously long timezone key");
    }
    wcstombs(buf, dtzi.TimeZoneKeyName, wlen);
    if (strcmp(buf, "Coordinated Universal Time") == 0)
        return "UTC";
    return buf;
}

// inspired by `native_to_standard_timezone_name` from the `date` library
static const char *native_name_to_standard_name(const char *native) {
    if (strncmp(native, "UTC", MAX_KEY_LENGTH) == 0) {
        return "Etc/UTC";
    }
    try {
        return windows_to_standard.at(native).c_str();
    } catch (std::out_of_range e) {
        return nullptr;
    }
}

static std::chrono::time_point<std::chrono::steady_clock>
    next_flush = std::chrono::steady_clock::now();
static std::unordered_map<
    std::string, DYNAMIC_TIME_ZONE_INFORMATION> cache;
static std::shared_mutex rwlock;

static void repopulate_timezone_cache(
    std::chrono::time_point<std::chrono::steady_clock> current_time)
{
    rwlock.lock();
    if (current_time < next_flush) {
        rwlock.unlock();
        return;
    }
    cache.clear();
    DYNAMIC_TIME_ZONE_INFORMATION dtzi;
    next_flush = current_time + std::chrono::minutes(5);
    for (DWORD dwResult = 0, i = 0; dwResult != ERROR_NO_MORE_ITEMS; ++i) {
        dwResult = EnumDynamicTimeZoneInformation(i, &dtzi);
        if (dwResult == ERROR_SUCCESS) {
            cache[copy_name(dtzi)] = dtzi;
        }
    }
    rwlock.unlock();
}

static bool time_zone_by_native_name(
    const std::string& native_name, DYNAMIC_TIME_ZONE_INFORMATION& dtzi)
{
    auto current_time = std::chrono::steady_clock::now();
    if (current_time > next_flush) {
        repopulate_timezone_cache(current_time);
    }
    rwlock.lock_shared();
    bool result = true;
    try {
        dtzi = cache.at(native_name);
    } catch (std::out_of_range e) {
        result = false;
    }
    rwlock.unlock_shared();
    return result;
}

static bool time_zone_by_name(
    const char *name, DYNAMIC_TIME_ZONE_INFORMATION& dtzi)
{
    try {
        auto native_name = standard_to_windows.at(name);
        return time_zone_by_native_name(native_name, dtzi);
    } catch (std::out_of_range e) {
        return false;
    }
}

/* this code is explained at
https://docs.microsoft.com/en-us/windows/win32/api/timezoneapi/ns-timezoneapi-time_zone_information
in the section about `StandardDate`.
In short, the `StandardDate` structure uses `SYSTEMTIME` in a...
non-conventional way.
*/
static void get_transition_date(int year, const SYSTEMTIME& src, SYSTEMTIME& dst)
{
    // if the year is 0, this is the absolute time.
    if (src.wYear != 0) {
        dst = src;
        return;
    }
    // otherwise, the transition happens yearly...
    dst.wYear = year;
    // at the specified month...
    dst.wMonth = src.wMonth;
    // specified hour...
    dst.wHour = src.wHour;
    // and minute...
    dst.wMinute = src.wMinute;
    // at the specified day of the week.
    dst.wDayOfWeek = src.wDayOfWeek;
    // The number of the occurrence of the specified day of week in the month,
    // or the special value "5" to denote the last such occurrence.
    unsigned int dowOccurrenceNumber = src.wDay;
    // lastly, we find the real date that corresponds to the nth occurrence.
    date::sys_days days = dowOccurrenceNumber == 5 ?
        date::sys_days(
        date::year_month_weekday_last(date::year(year), date::month(src.wMonth),
            date::weekday_last{date::weekday{src.wDayOfWeek}})) :
        date::sys_days(
        date::year_month_weekday(date::year(year), date::month(src.wMonth),
            date::weekday_indexed{
                date::weekday{src.wDayOfWeek}, dowOccurrenceNumber}));
    auto date = date::year_month_day(days);
    dst.wDay = (unsigned int)date.day();
}

#ifdef DEBUG
static void printSystime(const SYSTEMTIME& time)
{
    std::cout << time.wYear << "/" << time.wMonth << "/" <<
        time.wDay << " (" << time.wDayOfWeek << ") " <<
        time.wHour << ":" << time.wMinute << ":" << time.wSecond;
}
#endif

#define SECS_BETWEEN_1601_1970 11644473600LL
#define WINDOWS_TICKS_PER_SEC 10000000

static void unix_time_to_systemtime(int64_t epoch_sec, SYSTEMTIME& systime)
{
    int64_t windows_ticks = (epoch_sec + SECS_BETWEEN_1601_1970)
        * WINDOWS_TICKS_PER_SEC;
    ULARGE_INTEGER li;
    li.QuadPart = windows_ticks;
    FILETIME ft { li.LowPart, li.HighPart };
    FileTimeToSystemTime(&ft, &systime);
}

static int64_t systemtime_to_ticks(const SYSTEMTIME& systime)
{
    FILETIME ft {0, 0};
    SystemTimeToFileTime(&systime, &ft);
    ULARGE_INTEGER li;
    li.LowPart = ft.dwLowDateTime;
    li.HighPart = ft.dwHighDateTime;
    return li.QuadPart;
}

static int64_t systemtime_to_unix_time(const SYSTEMTIME& systime)
{
    return systemtime_to_ticks(systime) / WINDOWS_TICKS_PER_SEC -
        SECS_BETWEEN_1601_1970;
}

static bool is_daylight_time(
    const DYNAMIC_TIME_ZONE_INFORMATION& dtzi,
    const TIME_ZONE_INFORMATION& tzi,
    const SYSTEMTIME& time)
{
    // it means that daylight saving time is not supported at all
    if (tzi.StandardDate.wMonth == 0) {
        return false;
    }
    SYSTEMTIME standard_local, daylight_local;
    get_transition_date(time.wYear, tzi.StandardDate, standard_local);
    get_transition_date(time.wYear, tzi.DaylightDate, daylight_local);
    int64_t standard = systemtime_to_ticks(standard_local) /
        WINDOWS_TICKS_PER_SEC + (tzi.Bias + tzi.DaylightBias) * 60;
    int64_t daylight = systemtime_to_ticks(daylight_local) /
        WINDOWS_TICKS_PER_SEC + (tzi.Bias + tzi.StandardBias) * 60;
    int64_t time_secs = systemtime_to_ticks(time) /
        WINDOWS_TICKS_PER_SEC;
#ifdef DEBUG
    printSystime(standard_local); std::cout << " ";
    printSystime(daylight_local); std::cout << " ";
    printSystime(time); std::cout << " ";
    SYSTEMTIME standard_tmp, daylight_tmp, time_tmp;
    unix_time_to_systemtime(standard - SECS_BETWEEN_1601_1970, standard_tmp);
    unix_time_to_systemtime(daylight - SECS_BETWEEN_1601_1970, daylight_tmp);
    unix_time_to_systemtime(time_secs - SECS_BETWEEN_1601_1970, time_tmp);
    printSystime(standard_tmp); std::cout << " ";
    printSystime(daylight_tmp); std::cout << " ";
    printSystime(time_tmp); std::cout << std::endl;
#endif
    if (daylight < standard) {
        // The year is |STANDARD|DAYLIGHT|STANDARD|
        return time_secs < standard && time_secs >= daylight;
    } else {
        // The year is |DAYLIGHT|STANDARD|DAYLIGHT|
        return time_secs < standard || time_secs >= daylight;
    }
}

static int offset_at_systime(DYNAMIC_TIME_ZONE_INFORMATION& dtzi,
    const SYSTEMTIME& systime)
{
    TIME_ZONE_INFORMATION tzi;
    bool result = GetTimeZoneInformationForYear(systime.wYear, &dtzi, &tzi);
    if (!result) {
        return INT_MAX;
    }
    auto bias = tzi.Bias;
    if (is_daylight_time(dtzi, tzi, systime)) {
        bias += tzi.DaylightBias;
    } else {
        bias += tzi.StandardBias;
    }
    return -bias * 60;
}

extern "C" {

#include "cdate.h"

const char * get_system_timezone()
{
    DYNAMIC_TIME_ZONE_INFORMATION dtzi{};
    auto result = GetDynamicTimeZoneInformation(&dtzi);
    if (result == TIME_ZONE_ID_INVALID)
        return nullptr;
    auto key = copy_name(dtzi);
    auto name = native_name_to_standard_name(key.c_str());
    if (name == nullptr) {
        return nullptr;
    } else {
        return strdup(name);
    }
}

/* this function is not atomic, but surely nobody mutates the timezone database
in the Windows registry in a way that requires to take a snapshot? */
const char ** available_zone_ids()
{
    std::set<std::string> known_native_names, known_ids;
    known_ids.insert("UTC");
    DYNAMIC_TIME_ZONE_INFORMATION dtzi{};
    for (DWORD dwResult = 0, i = 0; dwResult != ERROR_NO_MORE_ITEMS; ++i) {
        dwResult = EnumDynamicTimeZoneInformation(i, &dtzi);
        if (dwResult == ERROR_SUCCESS) {
            known_native_names.insert(copy_name(dtzi));
        }
    }
    for (auto it = standard_to_windows.begin();
        it != standard_to_windows.end(); ++it)
    {
        if (known_native_names.count(it->second)) {
            known_ids.insert(it->first);
        }
    }
    const char ** zones = (const char **)malloc(
        sizeof(const char *) * (known_ids.size() + 1));
    zones[known_ids.size()] = nullptr;
    unsigned int i = 0;
    for (auto it = known_ids.begin(); it != known_ids.end(); ++it) {
        zones[i] = strdup(it->c_str());
        ++i;
    }
    return zones;
}

int offset_at_instant(const char *zone_name, int64_t epoch_sec)
{
    DYNAMIC_TIME_ZONE_INFORMATION dtzi{};
    bool result = time_zone_by_name(zone_name, dtzi);
    if (!result) {
        return INT_MAX;
    }
    SYSTEMTIME systime;
    unix_time_to_systemtime(epoch_sec, systime);
    return offset_at_systime(dtzi, systime);
}

bool is_known_timezone(const char *zone_name)
{
    DYNAMIC_TIME_ZONE_INFORMATION dtzi{};
    return time_zone_by_name(zone_name, dtzi);
}

int offset_at_datetime(const char *zone_name, int64_t epoch_sec, int *offset)
{
    DYNAMIC_TIME_ZONE_INFORMATION dtzi{};
    bool result = time_zone_by_name(zone_name, dtzi);
    if (!result) {
        return INT_MAX;
    }
    SYSTEMTIME localtime, utctime, adjusted;
    unix_time_to_systemtime(epoch_sec, localtime);
    TzSpecificLocalTimeToSystemTimeEx(&dtzi, &localtime, &utctime);
    *offset = offset_at_systime(dtzi, utctime);
    SystemTimeToTzSpecificLocalTimeEx(&dtzi, &utctime, &adjusted);
    return (int)(systemtime_to_unix_time(adjusted) - epoch_sec);
}

}
#endif // WIN32
