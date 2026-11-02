@file:JvmMultifileClass
@file:JvmName("YearMonthJvmKt")

package kotlinx.datetime

import kotlinx.datetime.format.*

// Preserving binary compatibility with 0.8.x, before YearMonth.kt was reorganized
@PublishedApi
@JvmName("parseOrNull")
internal fun YearMonth.Companion.parseOrNullJvm(
    input: CharSequence, format: DateTimeFormat<YearMonth> = YearMonth.Formats.ISO
): YearMonth? = parseOrNull(input, format)
