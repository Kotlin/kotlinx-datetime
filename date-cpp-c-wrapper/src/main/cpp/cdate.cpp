#include "date/date.h"
#include "date/tz.h"
using namespace date;
using namespace std::chrono;

template <class T>
static const char * timezone_name(const T& zone)
{
    auto name = zone.name();
    char * name_copy = (char *)malloc(sizeof(char) * (name.size() + 1));
    name_copy[name.size()] = '\0';
    name.copy(name_copy, name.size());
    return name_copy;
}

extern "C" {

#include "cdate.h"

const char * get_system_timezone()
{
    auto& tzdb = get_tzdb();
    auto zone = tzdb.current_zone();
    return timezone_name(*zone);
}

const char ** available_zone_ids()
{
    auto& tzdb = get_tzdb();
    auto& zones = tzdb.zones;
    const char ** zones_copy = (const char **)malloc(
            sizeof(const char *) * (zones.size() + 1));
    zones_copy[zones.size()] = nullptr;
    for (int i = 0; i < zones.size(); ++i) {
        zones_copy[i] = timezone_name(zones[i]);
    }
    return zones_copy;
}

int offset_at_instant(const char *zone_name, int64_t epoch_sec)
{
    auto& tzdb = get_tzdb();
    /* `sys_time` is usually Unix time (UTC, not counting leap seconds).
    Starting from C++20, it is specified in the standard. */
    auto stime = sys_time<std::chrono::seconds>(std::chrono::seconds(epoch_sec));
    try {
        auto zone = tzdb.locate_zone(zone_name);
        auto info = zone->get_info(stime);
        return info.offset.count();
    } catch (std::runtime_error e) {
        return INT_MAX;
    }
}

bool is_known_timezone(const char *zone_name)
{
    auto& tzdb = get_tzdb();
    try {
        tzdb.locate_zone(zone_name);
        return true;
    } catch (std::runtime_error e) {
        return false;
    }
}

int offset_at_datetime(const char *zone_name, int64_t epoch_sec, int preferred)
{
    auto& tzdb = get_tzdb();
    try {
        auto zone = tzdb.locate_zone(zone_name);
        local_seconds seconds((std::chrono::seconds(epoch_sec)));
        auto info = zone->get_info(seconds);
        sys_info sinfo;
        switch (info.result) {
            case local_info::unique:
                sinfo = info.first;
                break;
            case local_info::nonexistent:
                sinfo = info.second;
                break;
            case local_info::ambiguous:
                if (info.second.offset.count() == preferred)
                    return preferred;
                sinfo = info.first;
                break;
        }
        return sinfo.offset.count();
    } catch (std::runtime_error e) {
        return INT_MAX;
    }
}

}
