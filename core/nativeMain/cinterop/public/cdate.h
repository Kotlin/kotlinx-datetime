/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
// This file specifies the native interface for datetime information queries.
#pragma once
#include <stdint.h>
#include <stddef.h>

typedef size_t TZID;
const TZID TZID_INVALID = SIZE_MAX;

enum GAP_HANDLING {
    GAP_HANDLING_MOVE_FORWARD,
    GAP_HANDLING_NEXT_CORRECT,
};

/* Returns a string that must be freed by the caller, or null.
   If something is returned, `id` has the id of the timezone. */
char * get_system_timezone(TZID* id);

/* Returns an array of strings. The end of the array is marked with a NULL.
   The array and its contents must be freed by the caller.
   In case of an error, NULL is returned. */
char ** available_zone_ids();

// returns the offset, or INT_MAX if there's a problem with the time zone.
int offset_at_instant(TZID zone, int64_t epoch_sec);

// returns the id of the timezone or TZID_INVALID in case of an error.
TZID timezone_by_name(const char *zone_name);

/* Sets the result in "offset"; in case an existing value in "offset" is an
   acceptable one, leaves it untouched. Returns the number of seconds that the
   caller needs to add to their existing estimation of date, which is needed in
   case the time does not exist, having fallen in the gap.
   In case of an error, "offset" is set to INT_MAX. */
int offset_at_datetime(TZID zone, int64_t epoch_sec, int *offset);

int64_t at_start_of_day(TZID zone, int64_t midnight_epoch_sec);
