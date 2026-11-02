/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmName("DayOfWeekKt")
@file:JvmMultifileClass
package kotlinx.datetime

@Deprecated("Use kotlinx.datetime.DayOfWeek", ReplaceWith("toKotlinDayOfWeek().isoDayNumber"), level = DeprecationLevel.ERROR)
public val java.time.DayOfWeek.isoDayNumber: Int get() = toKotlinDayOfWeek().isoDayNumber
