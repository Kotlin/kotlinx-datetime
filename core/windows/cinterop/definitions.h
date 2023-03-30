#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <minwinbase.h>

typedef struct _REG_TZI_FORMAT {
  LONG Bias;
  LONG StandardBias;
  LONG DaylightBias;
  SYSTEMTIME StandardDate;
  SYSTEMTIME DaylightDate;
} REG_TZI_FORMAT;