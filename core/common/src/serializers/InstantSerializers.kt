/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.parse
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

/**
 * An abstract serializer for [Instant] values that uses
 * a custom [DateTimeFormat] for serializing to and deserializing.
 *
 * [format] should be a format that includes enough components to unambiguously define a date, a time, and a UTC offset.
 * See [Instant.parse] for details of how deserialization is performed.
 *
 * [name] is the name of the serializer.
 * The [SerialDescriptor.serialName] of the resulting serializer is `kotlinx.datetime.Instant/serializer/`[name].
 * [SerialDescriptor.serialName] must be unique across all serializers in the same serialization context.
 * When defining a serializer in a library, it is recommended to use the fully qualified class name in [name]
 * to avoid conflicts with serializers defined by other libraries and client code.
 *
 * When serializing, the [Instant] value is formatted as a string using the specified [format]
 * in the [ZERO][kotlinx.datetime.UtcOffset.ZERO] UTC offset.
 *
 * This serializer is abstract and must be subclassed to provide a concrete serializer.
 * Example:
 * ```
 * // serializes LocalDateTime(2008, 6, 30, 11, 5, 30).toInstant(TimeZone.UTC)
 * // as the string "Mon, 30 Jun 2008 11:05:30 GMT"
 * object Rfc1123InstantSerializer : FormattedInstantSerializer(
 *     "my.package.RFC1123", DateTimeComponents.Formats.RFC_1123
 * )
 * ```
 *
 * Note that `kotlinx.serialization` already provides [kotlinx.serialization.Serializable] support for [Instant],
 * so [Instant] values can be serialized and deserialized using the default serializer.
 */
public abstract class FormattedInstantSerializer(
    name: String,
    private val format: DateTimeFormat<DateTimeComponents>,
) : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlin.time.Instant/serializer/$name", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant =
        Instant.parse(decoder.decodeString(), format)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.format(format))
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toString(): String = descriptor.serialName
}
