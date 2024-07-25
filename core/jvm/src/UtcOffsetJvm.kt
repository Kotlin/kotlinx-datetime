/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.time.format.*

@Suppress("DEPRECATION")
@Serializable(with = UtcOffsetSerializer::class)
public actual class UtcOffset(internal val zoneOffset: ZoneOffset) {
    public actual val totalSeconds: Int get() = zoneOffset.totalSeconds

    override fun hashCode(): Int = zoneOffset.hashCode()
    actual override fun equals(other: Any?): Boolean = other is UtcOffset && this.zoneOffset == other.zoneOffset
    actual override fun toString(): String = zoneOffset.toString()

    public actual companion object {
        public actual val ZERO: UtcOffset = UtcOffset(ZoneOffset.UTC)

        public actual fun parse(input: CharSequence, format: DateTimeFormat<UtcOffset>): UtcOffset = when {
            format === Formats.ISO -> parseWithFormat(input, isoFormat)
            format === Formats.ISO_BASIC -> parseWithFormat(input, isoBasicFormat)
            format === Formats.FOUR_DIGITS -> parseWithFormat(input, fourDigitsFormat)
            else -> format.parse(input)
        }

        @Deprecated("This overload is only kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        public fun parse(offsetString: String): UtcOffset = parse(input = offsetString)

        @Suppress("FunctionName")
        public actual fun Format(block: DateTimeFormatBuilder.WithUtcOffset.() -> Unit): DateTimeFormat<UtcOffset> =
            UtcOffsetFormat.build(block)
    }

    public actual object Formats {
        public actual val ISO: DateTimeFormat<UtcOffset> get() = ISO_OFFSET
        public actual val ISO_BASIC: DateTimeFormat<UtcOffset> get() = ISO_OFFSET_BASIC
        public actual val FOUR_DIGITS: DateTimeFormat<UtcOffset> get() = FOUR_DIGIT_OFFSET
    }
}

@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun UtcOffset(hours: Int? = null, minutes: Int? = null, seconds: Int? = null): UtcOffset =
    try {
        when {
            hours != null ->
                UtcOffset(ZoneOffset.ofHoursMinutesSeconds(hours, minutes ?: 0, seconds ?: 0))
            minutes != null ->
                UtcOffset(ZoneOffset.ofHoursMinutesSeconds(minutes / 60, minutes % 60, seconds ?: 0))
            else -> {
                UtcOffset(ZoneOffset.ofTotalSeconds(seconds ?: 0))
            }
        }
    } catch (e: DateTimeException) {
        throw IllegalArgumentException(e)
    }

private val isoFormat by lazy {
    DateTimeFormatterBuilder().parseCaseInsensitive().appendOffsetId().toFormatter()
}
private val isoBasicFormat by lazy {
    DateTimeFormatterBuilder().parseCaseInsensitive().appendOffset("+HHmmss", "Z").toFormatter()
}
private val fourDigitsFormat by lazy {
    DateTimeFormatterBuilder().parseCaseInsensitive().appendOffset("+HHMM", "+0000").toFormatter()
}

private fun parseWithFormat(input: CharSequence, format: DateTimeFormatter) = try {
    format.parse(input, ZoneOffset::from).let(::UtcOffset)
} catch (e: DateTimeException) {
    throw DateTimeFormatException(e)
}
