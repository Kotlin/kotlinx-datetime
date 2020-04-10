#define AUTO_DOWNLOAD 0
#define HAS_REMOTE_API 0
#define ONLY_C_LOCALE 1

#ifdef TARGET_OS_IPHONE
    #define WIN32 0
#else
    #define TARGET_OS_IPHONE 0
    #ifdef _WIN32
        #define WIN32 1
    #else
        /* A very dangerous action. This is needed so that we can use C++17 to
        have `std::shared_mutex` in the Windows implementation; however, this
        has the unfortunate side effect of the `date` library recognizing that
        it deals with C++17, which has a lot of additional features compared to
        C++11. However, many of these features are actually unavailable due to
        an outdated GCC root used for Linux. */
        #define __cplusplus 201103
        #define WIN32 0
    #endif
#endif

#if WIN32
    #define USE_OS_TZDB 0
#else
    #define USE_OS_TZDB 1
#endif
