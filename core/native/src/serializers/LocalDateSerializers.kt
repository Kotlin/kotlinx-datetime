/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.LocalDate
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

actual object LocalDateLongSerializer: KSerializer<LocalDate> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): LocalDate = dateFromLongEpochDays(decoder.decodeLong())

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeLong(value.toEpochDay().toLong())
    }

    internal inline fun dateFromLongEpochDays(epochDays: Long): LocalDate =
        if (epochDays <= LocalDate.MAX_EPOCH_DAY.toLong() && epochDays >= LocalDate.MIN_EPOCH_DAY.toLong()) {
            LocalDate.ofEpochDay(epochDays.toInt())
        } else {
            throw SerializationException(
                "The passed value exceeds the platform-specific boundaries of days representable in LocalDate")
        }

}
