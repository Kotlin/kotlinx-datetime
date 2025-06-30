/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

/**
 * This is a deprecated type alias for `kotlinx.datetime.Clock`.
 *
 * Before 0.7.0, this type was used to represent a clock in the `kotlinx.datetime` library.
 * However, in Kotlin 2.1.20,
 * the `kotlin.time.Clock` type was introduced and is now the preferred way to represent a clock.
 *
 * This type alias is kept for making sure the existing code that uses `kotlinx.datetime.Clock` compiles.
 * For ensuring binary compatibility instead of source compatibility,
 * see <https://github.com/Kotlin/kotlinx-datetime?tab=readme-ov-file#deprecation-of-instant>.
 */
@Deprecated(
    "This type is deprecated in favor of `kotlin.time.Clock`.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("kotlin.time.Clock", "kotlin.time.Clock"))
public typealias Clock = kotlin.time.Clock
