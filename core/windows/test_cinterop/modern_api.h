#define WIN32_LEAN_AND_MEAN
#include <timezoneapi.h>

BOOL SystemTimeToTzSpecificLocalTimeEx(
    const DYNAMIC_TIME_ZONE_INFORMATION *lpTimeZoneInformation,
    const SYSTEMTIME                    *lpUniversalTime,
    LPSYSTEMTIME                        lpLocalTime
);

DWORD EnumDynamicTimeZoneInformation(
    const DWORD                     dwIndex,
    PDYNAMIC_TIME_ZONE_INFORMATION  lpTimeZoneInformation
);