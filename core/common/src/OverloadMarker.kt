/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

/**
 * An instance of this class can not be obtained, and it should not be used.
 *
 * The purpose of this class is to allow defining functions that return `kotlin.time.Instant`,
 * but still keep the functions returning `kotlinx.datetime.Instant` for binary compatibility.
 * Kotlin does not allow two functions with the same name and parameter types but different return types,
 * so we need to use a trick to achieve this.
 * By introducing a fictional parameter of this type, we can pretend that the function has a different signature,
 * even though it can only be called exactly the same way as the function without this parameter used to be.
 * There is no ambiguity, as the old functions are deprecated and hidden and can not actually be called.
 *
 * @suppress this class is not meant to be used, so the documentation is only here for the curious reader.
 */
@Deprecated(
    "It is meaningless to try to pass an OverloadMarker to a function directly. " +
            "All functions accepting it have its instance as a default value.",
    level = DeprecationLevel.ERROR
)
public class OverloadMarker private constructor() {
    internal companion object {
        @Suppress("DEPRECATION_ERROR")
        internal val INSTANCE = OverloadMarker()
    }
}
