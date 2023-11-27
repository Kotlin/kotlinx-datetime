#include <stdbool.h>

const bool kotlinxDatetimeRunningInSimulator =
  #if TARGET_OS_SIMULATOR
      true
  #else
      false
  #endif
;
