@file:JvmMultifileClass
@file:JvmName("UtcOffsetJvmKt")

package kotlinx.datetime

import kotlinx.datetime.format.*

// Preserving binary compatibility with 0.8.x, before UtcOffset.kt was reorganized
@PublishedApi
@JvmName("parseOrNull")
internal fun UtcOffset.Companion.parseOrNullJvm(
    input: CharSequence, format: DateTimeFormat<UtcOffset> = UtcOffset.Formats.ISO
): UtcOffset? = parseOrNull(input, format)

// Preserving binary compatibility with 0.8.x, before UtcOffset.kt was reorganized
@PublishedApi
@JvmName("UtcOffset")
internal fun UtcOffsetJvm(hours: Int? = null, minutes: Int? = null, seconds: Int? = null): UtcOffset =
    UtcOffset(hours, minutes, seconds)

