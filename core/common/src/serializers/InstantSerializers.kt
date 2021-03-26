/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

object InstantIso8601Serializer: KSerializer<Instant> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant =
        Instant.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

}

object InstantComponentSerializer: KSerializer<Instant> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Instant") {
            element<Long>("epochSeconds")
            element<Long>("nanosecondsOfSecond", isOptional = true)
        }

    @Suppress("INVISIBLE_MEMBER") // to be able to throw `MissingFieldException`
    override fun deserialize(decoder: Decoder): Instant =
        decoder.decodeStructure(descriptor) {
            var epochSeconds: Long? = null
            var nanosecondsOfSecond = 0
            loop@while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> epochSeconds = decodeLongElement(descriptor, 0)
                    1 -> nanosecondsOfSecond = decodeIntElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break@loop // https://youtrack.jetbrains.com/issue/KT-42262
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            if (epochSeconds == null) throw MissingFieldException("epochSeconds")
            Instant.fromEpochSeconds(epochSeconds, nanosecondsOfSecond)
        }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.epochSeconds)
            if (value.nanosecondsOfSecond != 0) {
                encodeIntElement(descriptor, 1, value.nanosecondsOfSecond)
            }
        }
    }

}
