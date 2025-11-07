/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.MonthNames
import platform.Foundation.NSCalendar

// Cache for calendars per locale to avoid repeated creation
private val monthCalendarCache = mutableMapOf<Locale, NSCalendar>()

private fun getOrCreateMonthCalendar(locale: Locale): NSCalendar {
    return monthCalendarCache.getOrPut(locale) {
        NSCalendar.currentCalendar.apply {
            this.locale = locale.value
        }
    }
}

/**
 * Returns the localized display name for this month using NSCalendar.
 */
public actual fun Month.displayName(
    textStyle: TextStyle,
    locale: Locale
): String {
    val calendar = getOrCreateMonthCalendar(locale)

    // Month.ordinal is 0-based (JANUARY=0, DECEMBER=11)
    // NSCalendar month symbols are 0-indexed arrays
    val monthSymbols = when (textStyle) {
        TextStyle.FULL -> calendar.monthSymbols
        TextStyle.FULL_STANDALONE -> calendar.standaloneMonthSymbols
        TextStyle.SHORT -> calendar.shortMonthSymbols
        TextStyle.SHORT_STANDALONE -> calendar.shortStandaloneMonthSymbols
        TextStyle.NARROW -> calendar.veryShortMonthSymbols
        TextStyle.NARROW_STANDALONE -> calendar.veryShortStandaloneMonthSymbols
    }

    // Safely convert NSArray to List<String>
    // In Kotlin/Native, NSArray is already mapped to List<*>
    @Suppress("UNCHECKED_CAST")
    val monthNames = monthSymbols as? List<String>

    val result = monthNames?.getOrNull(this.ordinal)
    return result ?: when (textStyle) {
        TextStyle.FULL, TextStyle.FULL_STANDALONE -> MonthNames.ENGLISH_FULL.names[this.ordinal]
        TextStyle.SHORT, TextStyle.SHORT_STANDALONE -> MonthNames.ENGLISH_ABBREVIATED.names[this.ordinal]
        TextStyle.NARROW, TextStyle.NARROW_STANDALONE -> listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")[this.ordinal]
    }
}
