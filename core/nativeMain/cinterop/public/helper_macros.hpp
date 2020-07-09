/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
#pragma once
#include <cstdlib>

/* Check the given pointer to see if it's null. If so, fail, printing to
   stderr that insufficient memory is available. */
template <typename T>
static inline T* check_allocation(T* value)
{
    if (value == nullptr) {
        fprintf(stderr, "Insufficient memory available\n");
        abort();
    }
    return value;
}
