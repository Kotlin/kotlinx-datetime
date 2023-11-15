/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.LocalTimeIso8601Serializer
import kotlinx.serialization.Serializable
import kotlinx.datetime.internal.JSJoda.LocalTime as jtLocalTime

@Serializable(LocalTimeIso8601Serializer::class)
public actual class LocalTime internal constructor(internal val value: jtLocalTime) :
    Comparable<LocalTime> {

    public actual constructor(hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(
                try {
                    jsTry {
                        jtLocalTime.of(hour, minute, second, nanosecond)
                    }
                } catch (e: Throwable) {
                    if (e.isJodaDateTimeException()) throw IllegalArgumentException(e)
                    throw e
                }
            )

    public actual val hour: Int get() = value.hour().toInt()
    public actual val minute: Int get() = value.minute().toInt()
    public actual val second: Int get() = value.second().toInt()
    public actual val nanosecond: Int get() = value.nano().toInt()
    public actual fun toSecondOfDay(): Int = value.toSecondOfDay().toInt()
    public actual fun toMillisecondOfDay(): Int = (value.toNanoOfDay().toDouble() / NANOS_PER_MILLI).toInt()
    public actual fun toNanosecondOfDay(): Long = value.toNanoOfDay().toLong()

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is LocalTime && (this.value === other.value || this.value.equals(other.value)))

    override fun hashCode(): Int = value.hashCode().toInt()

    actual override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalTime): Int = this.value.compareTo(other.value).toInt()

    public actual companion object {
        public actual fun parse(isoString: String): LocalTime = try {
            jsTry {
                jtLocalTime.parse(isoString).let(::LocalTime)
            }
        } catch (e: Throwable) {
            if (e.isJodaDateTimeParseException()) throw DateTimeFormatException(e)
            throw e
        }

        public actual fun fromSecondOfDay(secondOfDay: Int): LocalTime = try {
            jsTry {
                jtLocalTime.ofSecondOfDay(secondOfDay, 0).let(::LocalTime)
            }
        } catch (e: Throwable) {
            throw IllegalArgumentException(e)
        }

        public actual fun fromMillisecondOfDay(millisecondOfDay: Int): LocalTime = try {
            jsTry {
                jtLocalTime.ofNanoOfDay(millisecondOfDay * 1_000_000.0).let(::LocalTime)
            }
        } catch (e: Throwable) {
            throw IllegalArgumentException(e)
        }

        public actual fun fromNanosecondOfDay(nanosecondOfDay: Long): LocalTime = try {
            // number of nanoseconds in a day is much less than `Number.MAX_SAFE_INTEGER`.
            jsTry {
                jtLocalTime.ofNanoOfDay(nanosecondOfDay.toDouble()).let(::LocalTime)
            }
        } catch (e: Throwable) {
            throw IllegalArgumentException(e)
        }

        internal actual val MIN: LocalTime = LocalTime(jtLocalTime.MIN)
        internal actual val MAX: LocalTime = LocalTime(jtLocalTime.MAX)
    }
}
