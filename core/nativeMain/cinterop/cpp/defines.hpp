#define AUTO_DOWNLOAD 0
#define HAS_REMOTE_API 0

#ifdef TARGET_OS_IPHONE
    #define WIN32 0
#else
    #define TARGET_OS_IPHONE 0
    #ifdef _WIN32
        #define WIN32 1
    #else
        #define WIN32 0
    #endif
#endif

#if WIN32
    #define USE_OS_TZDB 0
#else
    #define USE_OS_TZDB 1
#endif
