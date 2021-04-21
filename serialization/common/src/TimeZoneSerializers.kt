/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.TimeZone
import kotlinx.datetime.ZoneOffset
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

public object TimeZoneSerializer: KSerializer<TimeZone> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TimeZone", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): TimeZone = TimeZone.of(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: TimeZone) {
        encoder.encodeString(value.id)
    }

}

public object ZoneOffsetSerializer: KSerializer<ZoneOffset> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ZoneOffset", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ZoneOffset {
        val zone = TimeZone.of(decoder.decodeString())
        if (zone is ZoneOffset) {
            return zone
        } else {
            throw SerializationException("Timezone identifier '$zone' does not correspond to a fixed-offset timezone")
        }
    }

    override fun serialize(encoder: Encoder, value: ZoneOffset) {
        encoder.encodeString(value.id)
    }

}
