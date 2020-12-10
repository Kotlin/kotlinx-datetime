/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("LocalDateTimeJvmKt")
package kotlinx.datetime

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.time.DateTimeException
import java.time.format.DateTimeParseException
import java.time.LocalDateTime as jtLocalDateTime
import java.time.LocalDate as jtLocalDate
import java.time.LocalTime as jtLocalTime
import kotlinx.serialization.internal.*

public actual typealias Month = java.time.Month
public actual typealias DayOfWeek = java.time.DayOfWeek

actual object LocalDateTimeCompactSerializer: KSerializer<LocalDateTime> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("LocalDateTime") {
            element<Long>("epochDay")
            element<Long>("nanoOfDay")
        }

    @Suppress("INVISIBLE_MEMBER") // to be able to throw `MissingFieldException`
    override fun deserialize(decoder: Decoder): LocalDateTime =
        decoder.decodeStructure(descriptor) {
            var epochDay: Long? = null
            var nanoOfDay: Long? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> epochDay = decodeLongElement(descriptor, 0)
                    1 -> nanoOfDay = decodeLongElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            if (epochDay == null) throw MissingFieldException("epochDay")
            if (nanoOfDay == null) throw MissingFieldException("nanoOfDay")
            LocalDateTime(jtLocalDateTime.of(jtLocalDate.ofEpochDay(epochDay), jtLocalTime.ofNanoOfDay(nanoOfDay)))
        }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.date.value.toEpochDay())
            encodeLongElement(descriptor, 1, value.value.toLocalTime().toNanoOfDay())
        }
    }

}

@Serializable(with = LocalDateTimeISO8601Serializer::class)
public actual class LocalDateTime internal constructor(internal val value: jtLocalDateTime) : Comparable<LocalDateTime> {

    public actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(try {
                jtLocalDateTime.of(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond)
            } catch (e: DateTimeException) {
                throw IllegalArgumentException(e)
            })

    public actual constructor(year: Int, month: Month, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(year, month.number, dayOfMonth, hour, minute, second, nanosecond)

    public actual val year: Int get() = value.year
    public actual val monthNumber: Int get() = value.monthValue
    public actual val month: Month get() = value.month
    public actual val dayOfMonth: Int get() = value.dayOfMonth
    public actual val dayOfWeek: DayOfWeek get() = value.dayOfWeek
    public actual val dayOfYear: Int get() = value.dayOfYear

    public actual val hour: Int get() = value.hour
    public actual val minute: Int get() = value.minute
    public actual val second: Int get() = value.second
    public actual val nanosecond: Int get() = value.nano

    public actual val date: LocalDate get() = LocalDate(value.toLocalDate()) // cache?

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is LocalDateTime && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalDateTime): Int = this.value.compareTo(other.value)

    actual companion object {
        public actual fun parse(isoString: String): LocalDateTime = try {
            jtLocalDateTime.parse(isoString).let(::LocalDateTime)
        } catch (e: DateTimeParseException) {
            throw DateTimeFormatException(e)
        }

        internal actual val MIN: LocalDateTime = LocalDateTime(jtLocalDateTime.MIN)
        internal actual val MAX: LocalDateTime = LocalDateTime(jtLocalDateTime.MAX)
    }

}

