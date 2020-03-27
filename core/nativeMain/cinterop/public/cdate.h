#include <time.h>
#include <stdint.h>
#include <stdbool.h>

/* Returns a string that must be freed by the caller. */
const char * get_system_timezone();

/* Returns an array of strings. The end of the array is marked with a NULL.
   The array and its contents must be freed by the caller. */
const char ** available_zone_ids();

int offset_at_instant(const char *zone_name, int64_t epoch_sec);

bool is_known_timezone(const char *zone_name);

/* Sets the result in "offset"; in case an existing value in "offset" is an
   acceptable one, leaves it untouched. Returns the number of seconds that the
   caller needs to add to their existing estimation of date, which is needed in
   case the time does not exist, having fallen in the gap. */
int offset_at_datetime(const char *zone_name, int64_t epoch_sec, int *offset);
