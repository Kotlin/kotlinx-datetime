/*
 * Copyright 2019-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

public object LocalTimeIso8601Serializer : KSerializer<LocalTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalTime =
        LocalTime.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeString(value.toString())
    }
}

public object LocalTimeComponentSerializer : KSerializer<LocalTime> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("LocalTime") {
            element<Short>("hour")
            element<Short>("minute")
            element<Short>("second", isOptional = true)
            element<Int>("nanosecond", isOptional = true)
        }

    @Suppress("INVISIBLE_MEMBER") // to be able to throw `MissingFieldException`
    override fun deserialize(decoder: Decoder): LocalTime =
        decoder.decodeStructure(descriptor) {
            var hour: Short? = null
            var minute: Short? = null
            var second: Short = 0
            var nanosecond = 0
            loop@while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> hour = decodeShortElement(descriptor, 3)
                    1 -> minute = decodeShortElement(descriptor, 4)
                    2 -> second = decodeShortElement(descriptor, 5)
                    3 -> nanosecond = decodeIntElement(descriptor, 6)
                    CompositeDecoder.DECODE_DONE -> break@loop // https://youtrack.jetbrains.com/issue/KT-42262
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            if (hour == null) throw MissingFieldException("hour")
            if (minute == null) throw MissingFieldException("minute")
            LocalTime(hour.toInt(), minute.toInt(), second.toInt(), nanosecond)
        }

    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeStructure(descriptor) {
            encodeShortElement(descriptor, 0, value.hour.toShort())
            encodeShortElement(descriptor, 1, value.minute.toShort())
            if (value.second != 0 || value.nanosecond != 0) {
                encodeShortElement(descriptor, 2, value.second.toShort())
                if (value.nanosecond != 0) {
                    encodeIntElement(descriptor, 3, value.nanosecond)
                }
            }
        }
    }
}