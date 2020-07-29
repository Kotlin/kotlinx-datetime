/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* This file implements the functions specified in `cdate.h` for Apple-based
   OS. This is used for iOS, but can also be used with no changes for MacOS.
   For now, MacOS uses the implementation based on the `date` library, along
   with Linux, and can be found in `cdate.cpp`. */
#if TARGET_OS_IPHONE // only enable for iOS.
#import <Foundation/Foundation.h>
#import <Foundation/NSTimeZone.h>
#import <Foundation/NSDate.h>
#import <Foundation/NSCalendar.h>
#import <limits.h>
#import <vector>
#import <set>
#import <string>
#include "helper_macros.hpp"

static NSTimeZone * zone_by_name(NSString *zone_name)
{
    auto abbreviations = NSTimeZone.abbreviationDictionary;
    auto true_name = [abbreviations valueForKey: zone_name];
    NSString *name = zone_name;
    if (true_name != nil) {
        name = true_name;
    }
    return [NSTimeZone timeZoneWithName: name];
}

static NSDate * dateWithTimeIntervalSince1970Saturating(int64_t epoch_sec)
{
    auto date = [NSDate dateWithTimeIntervalSince1970: epoch_sec];
    if ([date timeIntervalSinceDate:[NSDate distantPast]] < 0)
        date = [NSDate distantPast];
    else if ([date timeIntervalSinceDate:[NSDate distantFuture]] > 0)
        date = [NSDate distantFuture];
    return date;
}

extern "C" {
#include "cdate.h"
}

static std::vector<NSTimeZone *> populate()
{
    std::vector<NSTimeZone *> v;
    auto names = NSTimeZone.knownTimeZoneNames;
    for (size_t i = 0; i < names.count; ++i) {
        v.push_back([NSTimeZone timeZoneWithName: names[i]]);
    }
    return v;
}

static std::vector<NSTimeZone *> zones_cache = populate();

static TZID id_by_name(NSString *zone_name)
{
    auto abbreviations = NSTimeZone.abbreviationDictionary;
    auto true_name = [abbreviations valueForKey: zone_name];
    const NSString *name = zone_name;
    if (true_name != nil) {
        name = true_name;
    }
    for (size_t i = 0; i < zones_cache.size(); ++i) {
        if ([name isEqualToString:zones_cache[i].name]) {
            return i;
        }
    }
    return TZID_INVALID;
}

static NSTimeZone *timezone_by_id(TZID id)
{
    try {
        return zones_cache.at(id);
    } catch (std::out_of_range e) {
        return nullptr;
    }
}

extern "C" {

char * get_system_timezone(TZID *tzid)
{
    /* The framework has its own cache of the system timezone. Calls to
    [NSTimeZone systemTimeZone] do not reflect changes to the system timezone
    and instead just return the cached value. Thus, to acquire the current
    system timezone, first, the cache should be cleared.

    This solution is not without flaws, however. In particular, resetting the
    system timezone also resets the default timezone ([NSTimeZone default]) if
    it's the same as the cached system timezone:

        NSTimeZone.defaultTimeZone = [NSTimeZone
            timeZoneWithName: [[NSTimeZone systemTimeZone] name]];
        NSLog(@"%@", NSTimeZone.defaultTimeZone.name);
        NSLog(@"Change the system time zone, then press Enter");
        getchar();
        [NSTimeZone resetSystemTimeZone];
        NSLog(@"%@", NSTimeZone.defaultTimeZone.name); // will also change

    This is a fairly marginal problem:
        * It is only a problem when the developer deliberately sets the default
          timezone to the region that just happens to be the one that the user
          is in, and then the user moves to another region, and the app also
          uses the system timezone.
        * Since iOS 11, the significance of the default timezone has been
          de-emphasized. In particular, it is not included in the API for
          Swift: https://forums.swift.org/t/autoupdating-type-properties/4608/4

    Another possible solution could involve using [NSTimeZone localTimeZone].
    This is documented to reflect the current, uncached system timezone on
    iOS 11 and later:
    https://developer.apple.com/documentation/foundation/nstimezone/1387209-localtimezone
    However:
        * Before iOS 11, this was the same as the default timezone and did not
          reflect the system timezone.
        * Worse, on a Mac (10.15.5), I failed to get it to work as documented.
              NSLog(@"%@", NSTimeZone.localTimeZone.name);
              NSLog(@"Change the system time zone, then press Enter");
              getchar();
              // [NSTimeZone resetSystemTimeZone]; // uncomment to make it work
              NSLog(@"%@", NSTimeZone.localTimeZone.name);
          The printed strings are the same even if I wait for good 10 minutes
          before pressing Enter, unless the line with "reset" is uncommented--
          then the timezone is updated, as it should be. So, for some reason,
          NSTimeZone.localTimeZone, too, is cached.
          With no iOS device to test this on, it doesn't seem worth the effort
          to avoid just resetting the system timezone due to one edge case
          that's hard to avoid.
    */
    [NSTimeZone resetSystemTimeZone];
    NSTimeZone *zone = [NSTimeZone systemTimeZone];
    NSString *name = [zone name];
    *tzid = id_by_name(name);
    return strdup([name UTF8String]);
}

char ** available_zone_ids()
{
    std::set<std::string> ids;
    auto zones = NSTimeZone.knownTimeZoneNames;
    for (NSString * zone in zones) {
        ids.insert(std::string([zone UTF8String]));
    }
    auto abbrevs = NSTimeZone.abbreviationDictionary;
    for (NSString * key in abbrevs) {
        if (ids.count(std::string([abbrevs[key] UTF8String]))) {
            ids.insert(std::string([key UTF8String]));
        }
    }
    char ** zones_copy = check_allocation(
        (char **)malloc(sizeof(char *) * (ids.size() + 1)));
    zones_copy[ids.size()] = nullptr;
    unsigned long i = 0;
    for (auto it = ids.begin(); it != ids.end(); ++i, ++it) {
        zones_copy[i] = check_allocation(strdup(it->c_str()));
    }
    return zones_copy;
}

int offset_at_instant(TZID zone_id, int64_t epoch_sec)
{
    auto zone = timezone_by_id(zone_id);
    if (zone == nil) { return INT_MAX; }
    auto date = dateWithTimeIntervalSince1970Saturating(epoch_sec);
    return (int32_t)[zone secondsFromGMTForDate: date];
}

TZID timezone_by_name(const char *zone_name) {
    return id_by_name([NSString stringWithUTF8String: zone_name]);
}

static NSDate *system_date_by_local_date(NSTimeZone *zone, NSDate *local_date) {
    // The Gregorian calendar.
    NSCalendar *iso8601 = [NSCalendar
        calendarWithIdentifier: NSCalendarIdentifierISO8601];
    if (iso8601 == nil) { return nil; }
    // The UTC time zone
    NSTimeZone *utc = [NSTimeZone timeZoneForSecondsFromGMT: 0];
    /* Now, we say that the date that we initially meant is `date`, only with
       the context of being in a timezone `zone`. */
    NSDateComponents *dateComponents = [iso8601
        componentsInTimeZone: utc
        fromDate: local_date];
    dateComponents.timeZone = zone;
    return [iso8601 dateFromComponents:dateComponents];
}

int offset_at_datetime(TZID zone_id, int64_t epoch_sec, int *offset) {
    *offset = INT_MAX;
    // timezone
    auto zone = timezone_by_id(zone_id);
    if (zone == nil) { return 0; }
    /* a date in an unspecified timezone, defined by the number of seconds since
       the start of the epoch in *that* unspecified timezone */
    NSDate *date = dateWithTimeIntervalSince1970Saturating(epoch_sec);
    NSDate *newDate = system_date_by_local_date(zone, date);
    if (newDate == nil) { return 0; }
    // we now know the offset of that timezone at this time.
    *offset = (int)[zone secondsFromGMTForDate: newDate];
    /* `dateFromComponents` automatically corrects the date to avoid gaps. We
       need to learn which adjustments it performed. */
    int result = (int)((int64_t)[newDate timeIntervalSince1970] +
      (int64_t)*offset - (int64_t)[date timeIntervalSince1970]);
    return result;
}

int64_t at_start_of_day(TZID zone_id, int64_t epoch_sec) {
    // timezone
    auto zone = timezone_by_id(zone_id);
    if (zone == nil) { return INT_MAX; }
    NSDate *date = [NSDate dateWithTimeIntervalSince1970: epoch_sec];
    NSDate *newDate = system_date_by_local_date(zone, date);
    if (newDate == nil) { return INT_MAX; }
    int offset = (int)[zone secondsFromGMTForDate: newDate];
    /* if `epoch_sec` is not in the range supported by Darwin, assume that it
       is the correct local time for the midnight and just convert it to
       the system time. */
    if ([date timeIntervalSinceDate:[NSDate distantPast]] < 0 ||
        [date timeIntervalSinceDate:[NSDate distantFuture]] > 0)
        return epoch_sec - offset;
    // The ISO-8601 calendar.
    NSCalendar *iso8601 = [NSCalendar
        calendarWithIdentifier: NSCalendarIdentifierISO8601];
    iso8601.timeZone = zone;
    // start of the day denoted by `newDate`
    NSDate *midnight = [iso8601 startOfDayForDate: newDate];
    return (int64_t)([midnight timeIntervalSince1970]);
}

}
#endif // TARGET_OS_IPHONE
