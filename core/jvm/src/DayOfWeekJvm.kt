/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmName("DayOfWeekKt")
@file:JvmMultifileClass
package kotlinx.datetime

@Deprecated("Use kotlinx.datetime.DayOfWeek", ReplaceWith("toKotlinDayOfWeek().isoDayNumber"))
public val java.time.DayOfWeek.isoDayNumber: Int get() = toKotlinDayOfWeek().isoDayNumber

@Deprecated(
    message = "This overload is only kept for binary compatibility",
    level = DeprecationLevel.HIDDEN,
)
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
public fun DayOfWeek(isoDayNumber: Int): java.time.DayOfWeek =
    DayOfWeek(isoDayNumber).toJavaDayOfWeek()
