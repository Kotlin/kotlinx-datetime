/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("LocalTimeJvmKt")

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.LocalTimeIso8601Serializer
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.format.DateTimeParseException
import java.time.LocalTime as jtLocalTime

@Serializable(with = LocalTimeIso8601Serializer::class)
public actual class LocalTime internal constructor(
    // only a `var` to allow Java deserialization
    internal var value: jtLocalTime
) : Comparable<LocalTime>, java.io.Serializable {

    public actual constructor(hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(
                try {
                    jtLocalTime.of(hour, minute, second, nanosecond)
                } catch (e: DateTimeException) {
                    throw IllegalArgumentException(e)
                }
            )

    public actual val hour: Int get() = value.hour
    public actual val minute: Int get() = value.minute
    public actual val second: Int get() = value.second
    public actual val nanosecond: Int get() = value.nano
    public actual fun toSecondOfDay(): Int = value.toSecondOfDay()
    public actual fun toMillisecondOfDay(): Int = (value.toNanoOfDay() / NANOS_PER_MILLI).toInt()
    public actual fun toNanosecondOfDay(): Long = value.toNanoOfDay()

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is LocalTime && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalTime): Int = this.value.compareTo(other.value)

    public actual companion object {
        public actual fun parse(input: CharSequence, format: DateTimeFormat<LocalTime>): LocalTime =
            if (format === Formats.ISO) {
                try {
                    jtLocalTime.parse(input).let(::LocalTime)
                } catch (e: DateTimeParseException) {
                    throw DateTimeFormatException(e)
                }
            } else {
                format.parse(input)
            }

        @Deprecated("This overload is only kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        public fun parse(isoString: String): LocalTime = parse(input = isoString)

        public actual fun fromSecondOfDay(secondOfDay: Int): LocalTime = try {
            jtLocalTime.ofSecondOfDay(secondOfDay.toLong()).let(::LocalTime)
        } catch (e: DateTimeException) {
            throw IllegalArgumentException(e)
        }

        public actual fun fromMillisecondOfDay(millisecondOfDay: Int): LocalTime = try {
            jtLocalTime.ofNanoOfDay(millisecondOfDay * 1_000_000L).let(::LocalTime)
        } catch (e: Throwable) {
            throw IllegalArgumentException(e)
        }

        public actual fun fromNanosecondOfDay(nanosecondOfDay: Long): LocalTime = try {
            jtLocalTime.ofNanoOfDay(nanosecondOfDay).let(::LocalTime)
        } catch (e: DateTimeException) {
            throw IllegalArgumentException(e)
        }

        internal actual val MIN: LocalTime = LocalTime(jtLocalTime.MIN)
        internal actual val MAX: LocalTime = LocalTime(jtLocalTime.MAX)

        @Suppress("FunctionName")
        public actual fun Format(builder: DateTimeFormatBuilder.WithTime.() -> Unit): DateTimeFormat<LocalTime> =
            LocalTimeFormat.build(builder)

        @JvmStatic
        private val serialVersionUID: Long = 1L
    }

    public actual object Formats {
        public actual val ISO: DateTimeFormat<LocalTime> get() = ISO_TIME

    }

    private fun writeObject(oStream: java.io.ObjectOutputStream) {
        oStream.defaultWriteObject()
        oStream.writeObject(value.toString())
    }

    private fun readObject(iStream: java.io.ObjectInputStream) {
        iStream.defaultReadObject()
        val field = this::class.java.getDeclaredField(::value.name)
        field.isAccessible = true
        field.set(this, jtLocalTime.parse(iStream.readObject() as String))
    }

    private fun readObjectNoData() {
        throw java.io.InvalidObjectException("Stream data required")
    }
}
