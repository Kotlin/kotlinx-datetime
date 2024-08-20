/*
 * Copyright 2019-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * A serializer for [LocalDate] that uses the ISO 8601 representation.
 *
 * JSON example: `"2020-01-01"`
 *
 * @see LocalDate.Formats.ISO
 */
public object LocalDateIso8601Serializer : KSerializer<LocalDate>
by LocalDate.Formats.ISO.asKSerializer("kotlinx.datetime.LocalDate ISO")

/**
 * A serializer for [LocalDate] that represents a value as its components.
 *
 * JSON example: `{"year":2020,"month":12,"day":9}`
 */
public object LocalDateComponentSerializer: KSerializer<LocalDate> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlinx.datetime.LocalDate components") {
            element<Int>("year")
            element<Short>("month")
            element<Short>("day")
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): LocalDate =
        decoder.decodeStructure(descriptor) {
            var year: Int? = null
            var month: Short? = null
            var day: Short? = null
            loop@while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> year = decodeIntElement(descriptor, 0)
                    1 -> month = decodeShortElement(descriptor, 1)
                    2 -> day = decodeShortElement(descriptor, 2)
                    CompositeDecoder.DECODE_DONE -> break@loop // https://youtrack.jetbrains.com/issue/KT-42262
                    else -> throwUnknownIndexException(index)
                }
            }
            if (year == null) throw MissingFieldException(missingField = "year", serialName = descriptor.serialName)
            if (month == null) throw MissingFieldException(missingField = "month", serialName = descriptor.serialName)
            if (day == null) throw MissingFieldException(missingField = "day", serialName = descriptor.serialName)
            LocalDate(year, month.toInt(), day.toInt())
        }

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.year)
            encodeShortElement(descriptor, 1, value.monthNumber.toShort())
            encodeShortElement(descriptor, 2, value.dayOfMonth.toShort())
        }
    }

}

/**
 * An abstract serializer for [LocalDate] values that uses
 * a custom [DateTimeFormat] to serialize and deserialize the value.
 *
 * [name] is the name of the serializer.
 * The [SerialDescriptor.serialName] of the resulting serializer is `kotlinx.datetime.LocalDate serializer `[name].
 * [SerialDescriptor.serialName] must be unique across all serializers in the same serialization context.
 * When defining a serializer in a library, it is recommended to use the fully qualified class name in [name]
 * to avoid conflicts with serializers defined by other libraries and client code.
 *
 * This serializer is abstract and must be subclassed to provide a concrete serializer.
 * Example:
 * ```
 * // serializes LocalDate(2020, 1, 4) as the string "20200104"
 * object IsoBasicLocalDateSerializer :
 *     FormattedLocalDateSerializer("my.package.ISO_BASIC", LocalDate.Formats.ISO_BASIC)
 * ```
 *
 * Note that [LocalDate] is [kotlinx.serialization.Serializable] by default,
 * so it is not necessary to create custom serializers when the format is not important.
 * Additionally, [LocalDateIso8601Serializer] is provided for the ISO 8601 format.
 */
public abstract class FormattedLocalDateSerializer(
    name: String, format: DateTimeFormat<LocalDate>
) : KSerializer<LocalDate> by format.asKSerializer("kotlinx.datetime.LocalDate serializer $name")

internal fun <T> DateTimeFormat<T>.asKSerializer(serialName: String): KSerializer<T> =
    object : KSerializer<T> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): T = parse(decoder.decodeString())

        override fun serialize(encoder: Encoder, value: T) {
            encoder.encodeString(format(value))
        }

        override fun toString(): String = serialName
    }

/**
 * A serializer for [LocalDate] that uses the default [LocalDate.toString]/[LocalDate.parse].
 *
 * JSON example: `"2020-01-01"`
 */
@PublishedApi
internal object LocalDateSerializer: KSerializer<LocalDate> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.LocalDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate =
        LocalDate.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }

}
