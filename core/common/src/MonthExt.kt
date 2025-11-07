/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmName("MonthKt")
@file:JvmMultifileClass

package kotlinx.datetime

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Returns the localized display name for this month.
 *
 * The display name is formatted according to the specified [textStyle] and [locale].
 * For example, [Month.JANUARY] with [TextStyle.FULL_STANDALONE] and English locale
 * returns "January", while with [TextStyle.SHORT] it returns "Jan".
 *
 * The distinction between standalone and non-standalone styles matters in some languages
 * where grammatical forms differ based on context. For example, in Polish, the standalone
 * form of January is "styczeń", while the genitive form used in dates is "stycznia".
 *
 * @param textStyle the text style to use for formatting (default: [TextStyle.FULL_STANDALONE])
 * @param locale the locale to use for formatting (default: system default locale)
 * @return the localized display name for this month
 * @sample kotlinx.datetime.test.samples.MonthSamples.displayName
 */
public expect fun Month.displayName(
    textStyle: TextStyle = TextStyle.FULL_STANDALONE,
    locale: Locale = Locale.getDefault()
): String
