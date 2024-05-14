/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.format.ISO_TIME
import kotlinx.datetime.format.LocalTimeFormat
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import kotlinx.datetime.internal.JSJoda.LocalTime as jtLocalTime

@Serializable(LocalTimeSerializer::class)
public actual class LocalTime internal constructor(internal val value: jtLocalTime) :
    Comparable<LocalTime> {

    public actual constructor(hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(
                try {
                    jsTry { jtLocalTime.of(hour, minute, second, nanosecond) }
                } catch (e: Throwable) {
                    if (e.isJodaDateTimeException()) throw IllegalArgumentException(e)
                    throw e
                }
            )

    public actual val hour: Int get() = value.hour()
    public actual val minute: Int get() = value.minute()
    public actual val second: Int get() = value.second()
    public actual val nanosecond: Int get() = value.nano().toInt()
    public actual fun toSecondOfDay(): Int = value.toSecondOfDay()
    public actual fun toMillisecondOfDay(): Int = (value.toNanoOfDay() / NANOS_PER_MILLI).toInt()
    public actual fun toNanosecondOfDay(): Long = value.toNanoOfDay().toLong()

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is LocalTime && (this.value === other.value || this.value.equals(other.value)))

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalTime): Int = this.value.compareTo(other.value)

    public actual companion object {
        public actual fun parse(input: CharSequence, format: DateTimeFormat<LocalTime>): LocalTime =
            if (format === Formats.ISO) {
                try {
                    jsTry { jtLocalTime.parse(input.toString()) }.let(::LocalTime)
                } catch (e: Throwable) {
                    if (e.isJodaDateTimeParseException()) throw DateTimeFormatException(e)
                    throw e
                }
            } else {
                format.parse(input)
            }

        @Deprecated("This overload is only kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        public fun parse(isoString: String): LocalTime = parse(input = isoString)

        public actual fun fromSecondOfDay(secondOfDay: Int): LocalTime = try {
            jsTry { jtLocalTime.ofSecondOfDay(secondOfDay, 0) }.let(::LocalTime)
        } catch (e: Throwable) {
            throw IllegalArgumentException(e)
        }

        public actual fun fromMillisecondOfDay(millisecondOfDay: Int): LocalTime = try {
            jsTry { jtLocalTime.ofNanoOfDay(millisecondOfDay * 1_000_000.0) }.let(::LocalTime)
        } catch (e: Throwable) {
            throw IllegalArgumentException(e)
        }

        public actual fun fromNanosecondOfDay(nanosecondOfDay: Long): LocalTime = try {
            // number of nanoseconds in a day is much less than `Number.MAX_SAFE_INTEGER`.
            jsTry { jtLocalTime.ofNanoOfDay(nanosecondOfDay.toDouble()) }.let(::LocalTime)
        } catch (e: Throwable) {
            throw IllegalArgumentException(e)
        }

        internal actual val MIN: LocalTime = LocalTime(jtLocalTime.MIN)
        internal actual val MAX: LocalTime = LocalTime(jtLocalTime.MAX)

        @Suppress("FunctionName")
        public actual fun Format(builder: DateTimeFormatBuilder.WithTime.() -> Unit): DateTimeFormat<LocalTime> =
            LocalTimeFormat.build(builder)
    }

    public actual object Formats {
        public actual val ISO: DateTimeFormat<LocalTime> get() = ISO_TIME
    }
}
