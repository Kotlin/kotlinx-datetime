/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmName("DayOfWeekKt")
@file:JvmMultifileClass

package kotlinx.datetime

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Returns the localized display name for this day of the week.
 *
 * The display name is formatted according to the specified [textStyle] and [locale].
 * For example, [DayOfWeek.MONDAY] with [TextStyle.FULL_STANDALONE] and English locale
 * returns "Monday", while with [TextStyle.SHORT] it returns "Mon".
 *
 * The distinction between standalone and non-standalone styles matters in some languages
 * where grammatical forms differ based on context. For example, in some Slavic languages,
 * the standalone form is used when the day appears alone, while the non-standalone form
 * is used when it's part of a date phrase.
 *
 * @param textStyle the text style to use for formatting (default: [TextStyle.FULL_STANDALONE])
 * @param locale the locale to use for formatting (default: system default locale)
 * @return the localized display name for this day of the week
 * @sample kotlinx.datetime.test.samples.DayOfWeekSamples.displayName
 */
public expect fun DayOfWeek.displayName(
    textStyle: TextStyle = TextStyle.FULL_STANDALONE,
    locale: Locale = Locale.getDefault()
): String
