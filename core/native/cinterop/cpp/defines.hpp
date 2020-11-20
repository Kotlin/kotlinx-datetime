/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* This file is implicitly included at the beginning of every other .cpp file
   in the project with the `-include` flag to the compiler. It is used to
   define platform-specific constants and bindings, since it is seemingly
   difficult to run `cinterop` with different flags depending on the platform.
*/
#define AUTO_DOWNLOAD 0
#define HAS_REMOTE_API 0
#define ONLY_C_LOCALE 1

#if __APPLE__
    // Needed to set TARGET_OS_IPHONE
    #include <TargetConditionals.h>
#endif

#if TARGET_OS_IPHONE
    #define DATETIME_TARGET_WIN32 0
#else
    #define TARGET_OS_IPHONE 0
    #ifdef _WIN32
        #define DATETIME_TARGET_WIN32 1
    #else
        /* A very dangerous action. This is needed so that we can use C++17 to
        have `std::shared_mutex` in the Windows implementation; however, this
        has the unfortunate side effect of the `date` library recognizing that
        it deals with C++17, which has a lot of additional features compared to
        C++11. However, many of these features are actually unavailable due to
        an outdated GCC root used for Linux. */
        #undef __cplusplus
        #define __cplusplus 201103
        #define DATETIME_TARGET_WIN32 0
    #endif
#endif

#if DATETIME_TARGET_WIN32
    #define USE_OS_TZDB 0
#else
    #define USE_OS_TZDB 1
#endif
