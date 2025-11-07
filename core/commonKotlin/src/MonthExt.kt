/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.ENGLISH_NARROW
import kotlinx.datetime.format.MonthNames

/**
 * Returns the display name for this month.
 * On platforms without native locale support, returns English names only.
 */
public actual fun Month.displayName(
    textStyle: TextStyle,
    locale: Locale
): String {
    return when (textStyle) {
        TextStyle.FULL, TextStyle.FULL_STANDALONE -> MonthNames.ENGLISH_FULL.names[this.ordinal]
        TextStyle.SHORT, TextStyle.SHORT_STANDALONE -> MonthNames.ENGLISH_ABBREVIATED.names[this.ordinal]
        TextStyle.NARROW, TextStyle.NARROW_STANDALONE -> MonthNames.ENGLISH_NARROW[this.ordinal]
    }
}
