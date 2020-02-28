#include "date/date.h"
using namespace date;
using namespace std::chrono;
extern "C" {

#include "cdate.h"

instant instant_now() {
    instant t;

    /* According to the standard https://pubs.opengroup.org/onlinepubs/7908799/xsh/clock_gettime.html,
    the possible error values for `clock_gettime()` are as follows:
    - [EINVAL] The clock_id argument does not specify a known clock.
    - [ENOSYS] The functions clock_settime(), clock_gettime(), and clock_getres() are not supported.
    Both these error conditions will fail the tests if they happen, so we may ignore the error.
    */
    clock_gettime(CLOCK_REALTIME, &t);

    return t;
}

}