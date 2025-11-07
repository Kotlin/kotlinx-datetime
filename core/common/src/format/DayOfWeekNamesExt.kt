/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

/**
 * Narrow English names of weekdays from 'M' to 'S'.
 * Used primarily as fallback for localization on platforms where narrow names are unavailable.
 */
public val DayOfWeekNames.Companion.ENGLISH_NARROW: List<String>
    get() = listOf("M", "T", "W", "T", "F", "S", "S")
