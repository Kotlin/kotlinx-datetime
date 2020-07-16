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
    auto date = [NSDate dateWithTimeIntervalSince1970: epoch_sec];
    return (int32_t)[zone secondsFromGMTForDate: date];
}

TZID timezone_by_name(const char *zone_name) {
    return id_by_name([NSString stringWithUTF8String: zone_name]);
}

int offset_at_datetime(TZID zone_id, int64_t epoch_sec, int *offset) {
    *offset = INT_MAX;
    // timezone
    auto zone = timezone_by_id(zone_id);
    if (zone == nil) { return 0; }
    /* a date in an unspecified timezone, defined by the number of seconds since
       the start of the epoch in *that* unspecified timezone */
    NSDate *date = [NSDate dateWithTimeIntervalSince1970: epoch_sec];
    // The Gregorian calendar.
    NSCalendar *gregorian = [NSCalendar
        calendarWithIdentifier:NSCalendarIdentifierGregorian];
    if (gregorian == nil) { return 0; }
    // The UTC time zone
    NSTimeZone *utc = [NSTimeZone timeZoneForSecondsFromGMT: 0];
    /* Now, we say that the date that we initially meant is `date`, only with
       the context of being in a timezone `zone`. */
    NSDateComponents *dateComponents = [gregorian
        componentsInTimeZone: utc
        fromDate: date];
    dateComponents.timeZone = zone;
    NSDate *newDate = [gregorian dateFromComponents:dateComponents];
    if (newDate == nil) { return 0; }
    // we now know the offset of that timezone at this time.
    *offset = (int)[zone secondsFromGMTForDate: newDate];
    /* `dateFromComponents` automatically corrects the date to avoid gaps. We
       need to learn which adjustments it performed. */
    int result = (int)((int64_t)[newDate timeIntervalSince1970] +
      (int64_t)*offset - epoch_sec);
    return result;
}

}
#endif // TARGET_OS_IPHONE
