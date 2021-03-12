/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.internal.JSJoda.LocalDate
import kotlinx.datetime.internal.JSJoda.LocalTime
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

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
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            if (epochDay == null) throw MissingFieldException("epochDay")
            if (nanoOfDay == null) throw MissingFieldException("nanoOfDay")
            LocalDateTime(kotlinx.datetime.internal.JSJoda.LocalDateTime.of(LocalDate.ofEpochDay(epochDay), LocalTime.ofNanoOfDay(nanoOfDay)))
        }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.date.value.toEpochDay().toLong())
            encodeLongElement(descriptor, 1, value.value.toLocalTime().toNanoOfDay().toLong())
        }
    }

}
