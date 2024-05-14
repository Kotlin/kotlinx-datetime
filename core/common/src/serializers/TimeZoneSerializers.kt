/*
 * Copyright 2019-2021 JetBrains s.r.o.
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

/**
 * A serializer for [UtcOffset] that uses the ISO 8601 representation.
 *
 * JSON example: `"+02:00"`
 *
 * @see UtcOffset.Formats.ISO
 */
public object UtcOffsetIso8601Serializer : KSerializer<UtcOffset>
by DateTimeFormatSerializer(UtcOffset.Formats.ISO, "kotlinx.datetime.UtcOffset")

/**
 * A serializer for [UtcOffset] that uses the default [UtcOffset.toString]/[UtcOffset.parse].
 *
 * JSON example: `"+02:00"`
 */
@Deprecated("Use UtcOffset.serializer() instead", ReplaceWith("UtcOffset.serializer()"))
public object UtcOffsetSerializer: KSerializer<UtcOffset> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlinx.datetime.UtcOffset", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UtcOffset {
        return UtcOffset.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: UtcOffset) {
        encoder.encodeString(value.toString())
    }

}
