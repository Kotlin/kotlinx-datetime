/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

/**
 * Represents the style used for formatting date-time text, such as month or day-of-week names.
 *
 * Different styles provide different levels of detail and formatting:
 * - [FULL] and [FULL_STANDALONE] provide complete names (e.g., "January", "Monday")
 * - [SHORT] and [SHORT_STANDALONE] provide abbreviated names (e.g., "Jan", "Mon")
 * - [NARROW] and [NARROW_STANDALONE] provide minimal names (e.g., "J", "M")
 *
 * The standalone versions are used when the name appears in isolation,
 * while the non-standalone versions are used when the name appears as part of a larger phrase.
 * This distinction is important in some languages where grammatical forms differ based on context.
 * For example, in Polish, the standalone form of "January" is "styczeń",
 * while the genitive form used in dates is "stycznia".
 */
public enum class TextStyle {
    /**
     * Full text style, typically used in context (e.g., "Monday", "January").
     */
    FULL,

    /**
     * Full text style for standalone use (e.g., "Monday", "January").
     * This is the default style for most use cases.
     */
    FULL_STANDALONE,

    /**
     * Short or abbreviated text style, typically used in context (e.g., "Mon", "Jan").
     */
    SHORT,

    /**
     * Short or abbreviated text style for standalone use (e.g., "Mon", "Jan").
     */
    SHORT_STANDALONE,

    /**
     * Narrow text style, typically a single character, used in context (e.g., "M", "J").
     */
    NARROW,

    /**
     * Narrow text style for standalone use, typically a single character (e.g., "M", "J").
     */
    NARROW_STANDALONE,
}
