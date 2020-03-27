#if TARGET_OS_IPHONE
#include <Foundation/Foundation.h>
#include <Foundation/NSTimeZone.h>
#include <Foundation/NSDate.h>
#include <Foundation/NSCalendar.h>
#include <iostream>

extern "C" {
#include "cdate.h"

const char * get_system_timezone()
{
    CFTimeZoneRef zone = CFTimeZoneCopySystem();
    auto name = CFTimeZoneGetName(zone);
    CFIndex bufferSize = CFStringGetLength(name) + 1;
    char * buffer = (char *)malloc(sizeof(char) * bufferSize);
    if (CFStringGetCString(name, buffer, bufferSize, kCFStringEncodingUTF8))
    {
        CFRelease(zone);
        return buffer;
    }

    CFRelease(zone);
    buffer[0] = '\0';
    return buffer;
}

const char ** available_zone_ids()
{
    auto zones = NSTimeZone.knownTimeZoneNames;
    auto abbrevs = NSTimeZone.abbreviationDictionary.allKeys;
    const char ** zones_copy = (const char **)malloc(
            sizeof(const char *) * (zones.count + abbrevs.count + 1));
    zones_copy[zones.count + abbrevs.count] = nullptr;
    for (unsigned long i = 0; i < zones.count; ++i) {
        CFIndex bufferSize = zones[i].length + 1;
        char * buffer = (char *)malloc(sizeof(char) * bufferSize);
        strncpy(buffer, zones[i].UTF8String, bufferSize);
        zones_copy[i] = buffer;
    }
    for (unsigned long i = 0; i < abbrevs.count; ++i) {
        CFIndex bufferSize = abbrevs[i].length + 1;
        char * buffer = (char *)malloc(sizeof(char) * bufferSize);
        strncpy(buffer, abbrevs[i].UTF8String, bufferSize);
        zones_copy[zones.count + i] = buffer;
    }
    return zones_copy;
}

int offset_at_instant(const char *zone_name, int64_t epoch_sec)
{
    auto cocoa_epoch_sec = epoch_sec - NSTimeIntervalSince1970;
    auto zone_name_cfstring = CFStringCreateWithCString(
        NULL, zone_name, kCFStringEncodingUTF8);
    auto zone = CFTimeZoneCreateWithName(NULL, zone_name_cfstring, true);
    CFRelease(zone_name_cfstring);
    auto interval = CFTimeZoneGetSecondsFromGMT(zone, cocoa_epoch_sec);
    CFRelease(zone);
    return (int32_t)interval;
}

bool is_known_timezone(const char *zone_name) {
    auto zone_name_cfstring = CFStringCreateWithCString(
        NULL, zone_name, kCFStringEncodingUTF8);
    auto zone = CFTimeZoneCreateWithName(NULL, zone_name_cfstring, true);
    CFRelease(zone_name_cfstring);
    if (zone != nullptr) {
        CFRelease(zone);
        return true;
    }
    return false;
}

int offset_at_datetime(const char *zone_name, int64_t epoch_sec, int *offset) {
    // timezone name
    auto zone_name_nsstring = [NSString stringWithUTF8String: zone_name];
    // timezone
    auto zone = [NSTimeZone timeZoneWithName: zone_name_nsstring];
    /* a date in an unspecified timezone, defined by the number of seconds since
       the start of the epoch in *that* unspecified timezone */
    NSDate *date = [NSDate dateWithTimeIntervalSince1970: epoch_sec];
    // The Gregorian calendar.
    NSCalendar *gregorian = [NSCalendar
        calendarWithIdentifier:NSCalendarIdentifierGregorian];
    // The UTC time zone
    NSTimeZone *utc = [NSTimeZone timeZoneForSecondsFromGMT: 0];
    /* Now, we say that the date that we initially meant is `date`, only with
       the context of being in a timezone `zone`. */
    NSDateComponents *dateComponents = [gregorian
        componentsInTimeZone: utc
        fromDate: date];
    dateComponents.timeZone = zone;
    NSDate *newDate = [gregorian dateFromComponents:dateComponents];
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
