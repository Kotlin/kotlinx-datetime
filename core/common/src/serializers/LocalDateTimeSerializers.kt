/*
 * Copyright 2019-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * A serializer for [LocalDateTime] that uses the ISO 8601 representation.
 *
 * JSON example: `"2007-12-31T23:59:01"`
 *
 * @see LocalDateTime.parse
 * @see LocalDateTime.toString
 */
public object LocalDateTimeIso8601Serializer: KSerializer<LocalDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime =
        LocalDateTime.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString())
    }

}

/**
 * A serializer for [LocalDateTime] that represents a value as its components.
 *
 * JSON example: `{"year":2008,"month":7,"day":5,"hour":2,"minute":1}`
 */
public object LocalDateTimeComponentSerializer: KSerializer<LocalDateTime> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlinx.datetime.LocalDateTime") {
            element<Int>("year")
            element<Short>("month")
            element<Short>("day")
            element<Short>("hour")
            element<Short>("minute")
            element<Short>("second", isOptional = true)
            element<Int>("nanosecond", isOptional = true)
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): LocalDateTime =
        decoder.decodeStructure(descriptor) {
            var year: Int? = null
            var month: Short? = null
            var day: Short? = null
            var hour: Short? = null
            var minute: Short? = null
            var second: Short = 0
            var nanosecond = 0
            loop@while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> year = decodeIntElement(descriptor, 0)
                    1 -> month = decodeShortElement(descriptor, 1)
                    2 -> day = decodeShortElement(descriptor, 2)
                    3 -> hour = decodeShortElement(descriptor, 3)
                    4 -> minute = decodeShortElement(descriptor, 4)
                    5 -> second = decodeShortElement(descriptor, 5)
                    6 -> nanosecond = decodeIntElement(descriptor, 6)
                    CompositeDecoder.DECODE_DONE -> break@loop // https://youtrack.jetbrains.com/issue/KT-42262
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            if (year == null) throw MissingFieldException(missingField = "year", serialName = descriptor.serialName)
            if (month == null) throw MissingFieldException(missingField = "month", serialName = descriptor.serialName)
            if (day == null) throw MissingFieldException(missingField = "day", serialName = descriptor.serialName)
            if (hour == null) throw MissingFieldException(missingField = "hour", serialName = descriptor.serialName)
            if (minute == null) throw MissingFieldException(missingField = "minute", serialName = descriptor.serialName)
            LocalDateTime(year, month.toInt(), day.toInt(), hour.toInt(), minute.toInt(), second.toInt(), nanosecond)
        }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.year)
            encodeShortElement(descriptor, 1, value.month.number.toShort())
            encodeShortElement(descriptor, 2, value.day.toShort())
            encodeShortElement(descriptor, 3, value.hour.toShort())
            encodeShortElement(descriptor, 4, value.minute.toShort())
            if (value.second != 0 || value.nanosecond != 0) {
                encodeShortElement(descriptor, 5, value.second.toShort())
                if (value.nanosecond != 0) {
                    encodeIntElement(descriptor, 6, value.nanosecond)
                }
            }
        }
    }

}
