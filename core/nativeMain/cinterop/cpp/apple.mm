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

char * get_system_timezone()
{
    CFTimeZoneRef zone = CFTimeZoneCopySystem(); // always succeeds
    auto name = CFTimeZoneGetName(zone);
    CFIndex bufferSize = CFStringGetLength(name) + 1;
    char * buffer = (char *)malloc(sizeof(char) * bufferSize);
    if (buffer == nullptr) {
        CFRelease(zone);
        return nullptr;
    }
    // only fails if the name is not UTF8-encoded, which is an anomaly.
    if (CFStringGetCString(name, buffer, bufferSize, kCFStringEncodingUTF8))
    {
        CFRelease(zone);
        return buffer;
    }
    CFRelease(zone);
    free(buffer);
    return nullptr;
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
    char ** zones_copy = (char **)malloc(
            sizeof(char *) * (ids.size() + 1));
    if (zones_copy == nullptr) {
        return nullptr;
    }
    zones_copy[ids.size()] = nullptr;
    unsigned long i = 0;
    for (auto it = ids.begin(); it != ids.end(); ++i, ++it) {
        PUSH_BACK_OR_RETURN(zones_copy, i, strdup(it->c_str()));
    }
    return zones_copy;
}

int offset_at_instant(const char *zone_name, int64_t epoch_sec)
{
    auto zone_name_nsstring = [NSString stringWithUTF8String: zone_name];
    auto zone = zone_by_name(zone_name_nsstring);
    auto date = [NSDate dateWithTimeIntervalSince1970: epoch_sec];
    return (int32_t)[zone secondsFromGMTForDate: date];
}

bool is_known_timezone(const char *zone_name) {
    auto zone_name_nsstring = [NSString stringWithUTF8String: zone_name];
    return (zone_by_name(zone_name_nsstring) != nil);
}

int offset_at_datetime(const char *zone_name, int64_t epoch_sec, int *offset) {
    *offset = INT_MAX;
    // timezone name
    auto zone_name_nsstring = [NSString stringWithUTF8String: zone_name];
    // timezone
    auto zone = zone_by_name(zone_name_nsstring);
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
