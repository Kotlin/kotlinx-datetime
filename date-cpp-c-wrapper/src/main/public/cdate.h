#include <time.h>
#include <stdint.h>
#include <stdbool.h>

/* Returns a string that must be freed by the caller. */
const char * get_system_timezone();

/* Returns an array of strings. The end of the array is marked with a NULL.
   The array and its contents must be freed by the caller. */
const char ** available_zone_ids();

int offset_at_instant(const char *zone_name, const struct timespec *);

bool is_known_timezone(const char *zone_name);

int offset_at_datetime(const char *zone_name, int64_t epoch_sec, int preferred);
