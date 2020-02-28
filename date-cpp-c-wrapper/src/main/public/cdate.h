#include <time.h>
#include <stdint.h>

typedef struct timespec instant;

instant instant_now();

long long to_unix_millis(instant);
