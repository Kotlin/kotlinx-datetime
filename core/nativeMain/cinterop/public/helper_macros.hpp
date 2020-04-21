/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
#pragma once

/* Frees an array of allocated pointers. `array` is the array to free,
   and `length_var` is a variable that holds the current amount of
   cells that are filled. */
#define ARRAY_CLEANUP(array, length_var) \
    while (length_var > 0) { --length_var; free(array[length_var]); }; \
    free(array);

/* Tries to set the next element of the array or, if the element is null,
   frees all the existing elements of the array and the array itself and
   returns. */
#define PUSH_BACK_OR_RETURN(array, length_var, next_value) \
    { auto _value_to_put = (next_value); \
    if (_value_to_put == nullptr) { \
        ARRAY_CLEANUP(array, length_var); return nullptr; } \
    array[length_var] = _value_to_put; }
