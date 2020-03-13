#include <Foundation/Foundation.h>
#include <Foundation/NSTimeZone.h>
#include <Foundation/NSDate.h>
#include <Foundation/NSCalendar.h>

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

int offset_at_datetime(const char *zone_name, int64_t epoch_sec, int) {
    auto zone_name_cfstring = [[NSString alloc] initWithUTF8String: zone_name];
    auto zone = [NSTimeZone timeZoneWithName: zone_name_cfstring];
    NSDate *date = [[NSDate alloc] initWithTimeIntervalSince1970: epoch_sec];
    NSCalendar *gregorian = [[NSCalendar alloc]
        initWithCalendarIdentifier:NSCalendarIdentifierGregorian];
    NSDateComponents *dateComponents = [gregorian
        componentsInTimeZone: [NSTimeZone timeZoneForSecondsFromGMT: 0]
        fromDate: date];
    dateComponents.timeZone = zone;
    NSDate *newDate = [dateComponents date];
    auto result = [zone secondsFromGMTForDate: newDate];
    [gregorian release];
    [date release];
    [zone_name_cfstring release];
    return result;
}

}

