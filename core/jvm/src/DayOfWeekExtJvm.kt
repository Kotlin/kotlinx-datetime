/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmName("DayOfWeekKt")
@file:JvmMultifileClass
package kotlinx.datetime

/**
 * Returns the localized display name for this day of the week using java.time.DayOfWeek.
 */
public actual fun DayOfWeek.displayName(
    textStyle: TextStyle,
    locale: Locale
): String {
    val javaTextStyle = when (textStyle) {
        TextStyle.FULL -> java.time.format.TextStyle.FULL
        TextStyle.FULL_STANDALONE -> java.time.format.TextStyle.FULL_STANDALONE
        TextStyle.SHORT -> java.time.format.TextStyle.SHORT
        TextStyle.SHORT_STANDALONE -> java.time.format.TextStyle.SHORT_STANDALONE
        TextStyle.NARROW -> java.time.format.TextStyle.NARROW
        TextStyle.NARROW_STANDALONE -> java.time.format.TextStyle.NARROW_STANDALONE
    }
    return toJavaDayOfWeek().getDisplayName(javaTextStyle, locale.value)
}
