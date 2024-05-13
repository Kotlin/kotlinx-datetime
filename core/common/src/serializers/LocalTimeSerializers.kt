/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.*
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * A serializer for [LocalTime] that uses the ISO 8601 representation.
 *
 * JSON example: `"12:01:03.999"`
 *
 * @see LocalTime.Formats.ISO
 */
public object LocalTimeIso8601Serializer : KSerializer<LocalTime>
by LocalTime.Formats.ISO.asKSerializer("kotlinx.datetime.LocalTime")

/**
 * A serializer for [LocalTime] that represents a value as its components.
 *
 * JSON example: `{"hour":12,"minute":1,"second":3,"nanosecond":999}`
 */
public object LocalTimeComponentSerializer : KSerializer<LocalTime> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlinx.datetime.LocalTime") {
            element<Short>("hour")
            element<Short>("minute")
            element<Short>("second", isOptional = true)
            element<Int>("nanosecond", isOptional = true)
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): LocalTime =
        decoder.decodeStructure(descriptor) {
            var hour: Short? = null
            var minute: Short? = null
            var second: Short = 0
            var nanosecond = 0
            loop@while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> hour = decodeShortElement(descriptor, 0)
                    1 -> minute = decodeShortElement(descriptor, 1)
                    2 -> second = decodeShortElement(descriptor, 2)
                    3 -> nanosecond = decodeIntElement(descriptor, 3)
                    CompositeDecoder.DECODE_DONE -> break@loop // https://youtrack.jetbrains.com/issue/KT-42262
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            if (hour == null) throw MissingFieldException(missingField = "hour", serialName = descriptor.serialName)
            if (minute == null) throw MissingFieldException(missingField = "minute", serialName = descriptor.serialName)
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

/**
 * An abstract serializer for [LocalTime] values that uses
 * a custom [DateTimeFormat] to serialize and deserialize the value.
 *
 * [name] is the name of the serializer.
 * The [SerialDescriptor.serialName] of the resulting serializer is `kotlinx.datetime.LocalTime serializer `[name].
 * [SerialDescriptor.serialName] must be unique across all serializers in the same serialization context.
 * When defining a serializer in a library, it is recommended to use the fully qualified class name in [name]
 * to avoid conflicts with serializers defined by other libraries and client code.
 *
 * This serializer is abstract and must be subclassed to provide a concrete serializer.
 * Example:
 * ```
 * // serializes LocalTime(12, 30) as "12:30:00.000"
 * object FixedWidthTimeSerializer : FormattedLocalTimeSerializer("my.package.FixedWidthTime", LocalTime.Format {
 *     hour(); char(':'); minute(); char(':'); second(); char('.'); secondFraction(3)
 * })
 * ```
 *
 * Note that [LocalTime] is [kotlinx.serialization.Serializable] by default,
 * so it is not necessary to create custom serializers when the format is not important.
 * Additionally, [LocalTimeIso8601Serializer] is provided for the ISO 8601 format.
 */
public abstract class FormattedLocalTimeSerializer(
    name: String, format: DateTimeFormat<LocalTime>
) : KSerializer<LocalTime> by format.asKSerializer("kotlinx.datetime.LocalTime serializer $name")

/**
 * A serializer for [LocalTime] that uses the ISO 8601 representation.
 *
 * JSON example: `"12:01:03.999"`
 *
 * @see LocalDate.parse
 * @see LocalDate.toString
 */
@PublishedApi
internal object LocalTimeSerializer : KSerializer<LocalTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.LocalTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalTime =
        LocalTime.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeString(value.toString())
    }
}
