/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.*
import kotlinx.datetime.format.DateTimeFormat
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
 * A serializer for [UtcOffset] that uses the extended ISO 8601 representation.
 *
 * JSON example: `"+02:00"`
 *
 * @see UtcOffset.parse
 * @see UtcOffset.toString
 */
public object UtcOffsetSerializer: KSerializer<UtcOffset> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlinx.datetime.UtcOffset", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UtcOffset {
        return UtcOffset.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: UtcOffset) {
        encoder.encodeString(value.toString())
    }

}

/**
 * An abstract serializer for [UtcOffset] values that uses
 * a custom [DateTimeFormat] to serialize and deserialize the value.
 *
 * [name] is the name of the serializer.
 * The [SerialDescriptor.serialName] of the resulting serializer is `kotlinx.datetime.UtcOffset serializer `[name].
 * [SerialDescriptor.serialName] must be unique across all serializers in the same serialization context.
 * When defining a serializer in a library, it is recommended to use the fully qualified class name in [name]
 * to avoid conflicts with serializers defined by other libraries and client code.
 *
 * This serializer is abstract and must be subclassed to provide a concrete serializer.
 * Example:
 * ```
 * // serializes the UTC offset UtcOffset(hours = 2) as the string "+0200"
 * object FourDigitOffsetSerializer : FormattedUtcOffsetSerializer(
 *     "my.package.FOUR_DIGITS", UtcOffset.Formats.FOUR_DIGITS
 * )
 * ```
 *
 * Note that [UtcOffset] is [kotlinx.serialization.Serializable] by default,
 * so it is not necessary to create custom serializers when the format is not important.
 * Additionally, [UtcOffsetSerializer] is provided for the ISO 8601 format.
 */
public abstract class FormattedUtcOffsetSerializer(
    name: String, format: DateTimeFormat<UtcOffset>
) : KSerializer<UtcOffset> by format.asKSerializer("kotlinx.datetime.UtcOffset serializer $name")
