/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* This file implements the functions specified in `cdate.h` using the C++20
   chrono API. Since this is not yet widely supported, it relies on the `date`
   library. This implementation is used for MacOS and Linux, but once <chrono>
   is available for all the target platforms, the dependency on `date` can be
   removed along with the neighboring implementations. */
#if !TARGET_OS_IPHONE
#if !DATETIME_TARGET_WIN32
#include "date/date.h"
#include "date/tz.h"
#include "helper_macros.hpp"
#include <cstring>
using namespace date;
using namespace std::chrono;

static int64_t first_instant_of_year(const year& yr) {
    return sys_seconds{sys_days{yr/January/1}}.time_since_epoch().count();
}
/* This constant represents the earliest moment that our system recognizes;
everything earlier than that is considered the same moment.
This doesn't make us lose any precision in computations, as timezone
information doesn't make sense to use at that time, as there were no
timezones, and even the calendars were all different.
The reason for this explicit check is that the years that are considered
valid by the "date" library are [-32767; 32767], and library crashes if
it sees a date in year -32768 or earlier. */
static const int64_t min_available_instant =
    first_instant_of_year(++year::min());
// Lack of this check didn't cause any problems yet, but why not add it too?
static const int64_t max_available_instant =
    first_instant_of_year(--year::max());

static seconds saturating(int64_t epoch_sec)
{
    if (epoch_sec < min_available_instant)
        epoch_sec = min_available_instant;
    else if (epoch_sec > max_available_instant)
        epoch_sec = max_available_instant;
    return seconds(epoch_sec);
}

extern "C" {
#include "cdate.h"
}

template <class T>
static char * timezone_name(const T& zone)
{
    return strdup(zone.name().c_str());
}

static const time_zone *zone_by_id(TZID id)
{
    /* The `date` library provides a linked list of `tzdb` objects. `get_tzdb()`
       always returns the head of that list. For now, the list never changes:
       a call to `reload_tzdb()` would be required to load the updated version
       of the timezone database. We never do this because for now (with use of
       `date`) this operation is not even present for the configuration that
       uses the system timezone database. If we move to C++20 support for this,
       it may be feasible to call `reload_tzdb()` and construct a more elaborate
       ID scheme. */
    auto& tzdb = get_tzdb();
    try {
        return &tzdb.zones.at(id);
    } catch (std::out_of_range e) {
        throw std::runtime_error("Invalid timezone id");
    }
}

static TZID id_by_zone(const tzdb& db, const time_zone* tz)
{
    size_t id = tz - &db.zones[0];
    if (id >= db.zones.size()) {
        throw std::runtime_error("The time zone is not part of the tzdb");
    }
    return id;
}

extern "C" {

char * get_system_timezone(TZID * id)
{
    try {
        auto& tzdb = get_tzdb();
        auto zone = tzdb.current_zone();
        *id = id_by_zone(tzdb, zone);
        return timezone_name(*zone);
    } catch (std::runtime_error e) {
        *id = TZID_INVALID;
        return nullptr;
    }
}

char ** available_zone_ids()
{
    try {
        auto& tzdb = get_tzdb();
        auto& zones = tzdb.zones;
        char ** zones_copy = check_allocation(
            (char **)malloc(sizeof(char *) * (zones.size() + 1)));
        zones_copy[zones.size()] = nullptr;
        for (unsigned long i = 0; i < zones.size(); ++i) {
            zones_copy[i] = timezone_name(zones[i]);
        }
        return zones_copy;
    } catch (std::runtime_error e) {
        return nullptr;
    }
}

int offset_at_instant(TZID zone_id, int64_t epoch_sec)
{
    try {
        /* `sys_time` is usually Unix time (UTC, not counting leap seconds).
           Starting from C++20, it is specified in the standard. */
        auto stime = sys_time<std::chrono::seconds>(saturating(epoch_sec));
        auto zone = zone_by_id(zone_id);
        auto info = zone->get_info(stime);
        return info.offset.count();
    } catch (std::runtime_error e) {
        return INT_MAX;
    }
}

TZID timezone_by_name(const char *zone_name)
{
    try {
        auto& tzdb = get_tzdb();
        return id_by_zone(tzdb, tzdb.locate_zone(zone_name));
    } catch (std::runtime_error e) {
        return TZID_INVALID;
    }
}

static int offset_at_datetime_impl(TZID zone_id, seconds sec, int *offset,
GAP_HANDLING gap_handling)
{
    try {
        auto zone = zone_by_id(zone_id);
        local_seconds seconds(sec);
        auto info = zone->get_info(seconds);
        switch (info.result) {
            case local_info::unique:
                *offset = info.first.offset.count();
                return 0;
            case local_info::nonexistent: {
                *offset = info.second.offset.count();
                switch (gap_handling) {
                    case GAP_HANDLING_MOVE_FORWARD:
                        return info.second.offset.count() -
                            info.first.offset.count();
                    case GAP_HANDLING_NEXT_CORRECT: {
                        return duration_cast<std::chrono::seconds>(
                            info.second.begin.time_since_epoch()).count() -
                            sec.count() + info.second.offset.count();
                    }
                    default:
                        // impossible
                        *offset = INT_MAX;
                        return 0;
                }
            }
            case local_info::ambiguous:
                if (info.second.offset.count() != *offset)
                    *offset = info.first.offset.count();
                return 0;
            default:
                // the pattern matching above is supposedly exhaustive
                *offset = INT_MAX;
                return 0;
        }
    } catch (std::runtime_error e) {
        *offset = INT_MAX;
        return 0;
    }
}

int offset_at_datetime(TZID zone_id, int64_t epoch_sec, int *offset)
{
    return offset_at_datetime_impl(zone_id, saturating(epoch_sec), offset,
        GAP_HANDLING_MOVE_FORWARD);
}

int64_t at_start_of_day(TZID zone_id, int64_t epoch_sec)
{
    int offset = 0;
    int trans = offset_at_datetime_impl(zone_id, saturating(epoch_sec), &offset,
        GAP_HANDLING_NEXT_CORRECT);
    if (offset == INT_MAX)
        return LONG_MAX;
    if (epoch_sec > max_available_instant || epoch_sec < min_available_instant) {
        trans = 0;
    }
    return epoch_sec - offset + trans;
}

}
#endif // !DATETIME_TARGET_WIN32
#endif // !TARGET_OS_IPHONE
