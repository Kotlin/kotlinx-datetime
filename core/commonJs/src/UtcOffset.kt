/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.JSJoda.ZoneOffset as jtZoneOffset
import kotlinx.datetime.internal.JSJoda.ChronoField as jtChronoField
import kotlinx.datetime.internal.JSJoda.DateTimeFormatterBuilder as jtDateTimeFormatterBuilder
import kotlinx.datetime.internal.JSJoda.DateTimeFormatter as jtDateTimeFormatter
import kotlinx.datetime.internal.JSJoda.ResolverStyle as jtResolverStyle
import kotlinx.datetime.format.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable

@Suppress("DEPRECATION")
@Serializable(with = UtcOffsetSerializer::class)
public actual class UtcOffset internal constructor(internal val zoneOffset: jtZoneOffset) {
    public actual val totalSeconds: Int get() = zoneOffset.totalSeconds()

    override fun hashCode(): Int = zoneOffset.hashCode()
    actual override fun equals(other: Any?): Boolean =
        other is UtcOffset && (this.zoneOffset === other.zoneOffset || this.zoneOffset.equals(other.zoneOffset))
    actual override fun toString(): String = zoneOffset.toString()

    public actual companion object {
        public actual val ZERO: UtcOffset = UtcOffset(jtZoneOffset.UTC)

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
                UtcOffset(jsTry { jtZoneOffset.ofHoursMinutesSeconds(hours, minutes ?: 0, seconds ?: 0) })
            minutes != null ->
                UtcOffset(jsTry { jtZoneOffset.ofHoursMinutesSeconds(minutes / 60, minutes % 60, seconds ?: 0) })
            else -> {
                UtcOffset(jsTry { jtZoneOffset.ofTotalSeconds(seconds ?: 0) })
            }
        }
    } catch (e: Throwable) {
        if (e.isJodaDateTimeException()) throw IllegalArgumentException(e) else throw e
    }

private val isoFormat by lazy {
    jtDateTimeFormatterBuilder().parseCaseInsensitive().appendOffsetId().toFormatter(jtResolverStyle.STRICT)
}
private val isoBasicFormat by lazy {
    jtDateTimeFormatterBuilder().parseCaseInsensitive().appendOffset("+HHmmss", "Z").toFormatter(jtResolverStyle.STRICT)
}
private val fourDigitsFormat by lazy {
    jtDateTimeFormatterBuilder().parseCaseInsensitive().appendOffset("+HHMM", "+0000").toFormatter(jtResolverStyle.STRICT)
}

private fun parseWithFormat(input: CharSequence, format: jtDateTimeFormatter) = UtcOffset(seconds = try {
    jsTry { format.parse(input.toString()).get(jtChronoField.OFFSET_SECONDS) }
} catch (e: Throwable) {
    if (e.isJodaDateTimeParseException()) throw DateTimeFormatException(e)
    if (e.isJodaDateTimeException()) throw DateTimeFormatException(e)
    throw e
})
