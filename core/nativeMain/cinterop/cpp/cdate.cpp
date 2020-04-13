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
using namespace date;
using namespace std::chrono;

template <class T>
static char * timezone_name(const T& zone)
{
    auto name = zone.name();
    char * name_copy = (char *)malloc(sizeof(char) * (name.size() + 1));
    if (name_copy == nullptr) { return nullptr; }
    name_copy[name.size()] = '\0';
    name.copy(name_copy, name.size());
    return name_copy;
}

extern "C" {

#include "cdate.h"

char * get_system_timezone()
{
    try {
        auto& tzdb = get_tzdb();
        auto zone = tzdb.current_zone();
        return timezone_name(*zone);
    } catch (std::runtime_error e) {
        return nullptr;
    }
}

char ** available_zone_ids()
{
    try {
        auto& tzdb = get_tzdb();
        auto& zones = tzdb.zones;
        char ** zones_copy = (char **)malloc(
                sizeof(char *) * (zones.size() + 1));
        if (zones_copy == nullptr) { return nullptr; }
        zones_copy[zones.size()] = nullptr;
        for (unsigned long i = 0; i < zones.size(); ++i) {
            PUSH_BACK_OR_RETURN(zones_copy, i, timezone_name(zones[i]));
        }
        return zones_copy;
    } catch (std::runtime_error e) {
        return nullptr;
    }
}

int offset_at_instant(const char *zone_name, int64_t epoch_sec)
{
    try {
        auto& tzdb = get_tzdb();
        /* `sys_time` is usually Unix time (UTC, not counting leap seconds).
           Starting from C++20, it is specified in the standard. */
        auto stime = sys_time<std::chrono::seconds>(
            std::chrono::seconds(epoch_sec));
        auto zone = tzdb.locate_zone(zone_name);
        auto info = zone->get_info(stime);
        return info.offset.count();
    } catch (std::runtime_error e) {
        return INT_MAX;
    }
}

bool is_known_timezone(const char *zone_name)
{
    try {
        auto& tzdb = get_tzdb();
        tzdb.locate_zone(zone_name);
        return true;
    } catch (std::runtime_error e) {
        return false;
    }
}

int offset_at_datetime(const char *zone_name, int64_t epoch_sec, int *offset)
{
    try {
        auto& tzdb = get_tzdb();
        auto zone = tzdb.locate_zone(zone_name);
        local_seconds seconds((std::chrono::seconds(epoch_sec)));
        auto info = zone->get_info(seconds);
        switch (info.result) {
            case local_info::unique:
                *offset = info.first.offset.count();
                return 0;
            case local_info::nonexistent: {
                auto trans_duration = info.second.offset.count() -
                    info.first.offset.count();
                *offset = info.second.offset.count();
                return trans_duration;
            }
            case local_info::ambiguous:
                if (info.second.offset.count() != *offset)
                    *offset = info.first.offset.count();
                return 0;
        }
    } catch (std::runtime_error e) {
        *offset = INT_MAX;
        return 0;
    }
}

}
#endif // !DATETIME_TARGET_WIN32
#endif // !TARGET_OS_IPHONE
