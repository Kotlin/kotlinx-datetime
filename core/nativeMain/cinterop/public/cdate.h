/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
// This file specifies the native interface for datetime information queries.
#include <time.h>
#include <stdint.h>
#include <stdbool.h>

/* Returns a string that must be freed by the caller, or null. */
char * get_system_timezone();

/* Returns an array of strings. The end of the array is marked with a NULL.
   The array and its contents must be freed by the caller.
   In case of an error, NULL is returned. */
char ** available_zone_ids();

// returns the offset, or INT_MAX if there's a problem with the time zone.
int offset_at_instant(const char *zone_name, int64_t epoch_sec);

bool is_known_timezone(const char *zone_name);

/* Sets the result in "offset"; in case an existing value in "offset" is an
   acceptable one, leaves it untouched. Returns the number of seconds that the
   caller needs to add to their existing estimation of date, which is needed in
   case the time does not exist, having fallen in the gap.
   In case of an error, "offset" is set to INT_MAX. */
int offset_at_datetime(const char *zone_name, int64_t epoch_sec, int *offset);
