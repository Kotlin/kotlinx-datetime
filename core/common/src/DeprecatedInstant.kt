/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

/**
 * This is a deprecated type alias for `kotlinx.datetime.Instant`.
 *
 * Before 0.7.0, this type was used to represent a clock in the `kotlinx.datetime` library.
 * However, in Kotlin 2.1.20,
 * the `kotlin.time.Instant` type was introduced and is now the preferred way to represent a moment in time.
 *
 * This type alias is kept for making sure the existing code that uses `kotlinx.datetime.Instant` compiles.
 * For ensuring binary compatibility instead of source compatibility,
 * see <https://github.com/Kotlin/kotlinx-datetime?tab=readme-ov-file#deprecation-of-instant>.
 */
@Deprecated(
    "This type is deprecated in favor of `kotlin.time.Instant`.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("kotlin.time.Instant", "kotlin.time.Instant"))
public typealias Instant = kotlin.time.Instant
