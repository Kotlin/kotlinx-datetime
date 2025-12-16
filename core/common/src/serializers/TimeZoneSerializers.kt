/*
 * Copyright 2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * A serializer for [TimeZone] that represents the time zone as its identifier.
 *
 * JSON example: `"Europe/Berlin"`
 */
@Deprecated(
    "Serializing TimeZone is discouraged. Please serialize the string id instead.",
    level = DeprecationLevel.WARNING,
)
public object TimeZoneSerializer: KSerializer<TimeZone> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlinx.datetime.TimeZone", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): TimeZone = TimeZone.of(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: TimeZone) {
        encoder.encodeString(value.id)
    }

}

/**
 * A serializer for [FixedOffsetTimeZone] that represents the time zone as its identifier.
 *
 * JSON example: `"+02:00"`
 */
@Deprecated(
    "Serializing FixedOffsetTimeZoneSerializer is discouraged. Please serialize the string id instead.",
    level = DeprecationLevel.WARNING,
)
public object FixedOffsetTimeZoneSerializer: KSerializer<FixedOffsetTimeZone> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlinx.datetime.FixedOffsetTimeZone", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): FixedOffsetTimeZone {
        val zone = TimeZone.of(decoder.decodeString())
        if (zone is FixedOffsetTimeZone) {
            return zone
        } else {
            throw SerializationException("Timezone identifier '$zone' does not correspond to a fixed-offset timezone")
        }
    }

    override fun serialize(encoder: Encoder, value: FixedOffsetTimeZone) {
        encoder.encodeString(value.id)
    }

}
