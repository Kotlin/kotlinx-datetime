/*
 * Copyright 2019-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.YearMonth
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.number
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * A serializer for [YearMonth] that uses the ISO 8601 representation.
 *
 * JSON example: `"2020-01"`
 *
 * @see YearMonth.Formats.ISO
 */
public object YearMonthIso8601Serializer : KSerializer<YearMonth>
by YearMonth.Formats.ISO.asKSerializer("kotlinx.datetime.YearMonth/ISO")

/**
 * A serializer for [YearMonth] that represents a value as its components.
 *
 * JSON example: `{"year":2020,"month":12}`
 */
public object YearMonthComponentSerializer: KSerializer<YearMonth> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlinx.datetime.YearMonth/components") {
            element<Int>("year")
            element<Short>("month")
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): YearMonth =
        decoder.decodeStructure(descriptor) {
            var year: Int? = null
            var month: Short? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> year = decodeIntElement(descriptor, 0)
                    1 -> month = decodeShortElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throwUnknownIndexException(index)
                }
            }
            if (year == null) throw MissingFieldException(missingField = "year", serialName = descriptor.serialName)
            if (month == null) throw MissingFieldException(missingField = "month", serialName = descriptor.serialName)
            YearMonth(year, month.toInt())
        }

    override fun serialize(encoder: Encoder, value: YearMonth) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.year)
            encodeShortElement(descriptor, 1, value.month.number.toShort())
        }
    }

}

/**
 * An abstract serializer for [YearMonth] values that uses
 * a custom [DateTimeFormat] to serialize and deserialize the value.
 *
 * [name] is the name of the serializer.
 * The [SerialDescriptor.serialName] of the resulting serializer is `kotlinx.datetime.YearMonth/serializer/`[name].
 * [SerialDescriptor.serialName] must be unique across all serializers in the same serialization context.
 * When defining a serializer in a library, it is recommended to use the fully qualified class name in [name]
 * to avoid conflicts with serializers defined by other libraries and client code.
 *
 * This serializer is abstract and must be subclassed to provide a concrete serializer.
 * Example:
 * ```
 * // serializes YearMonth(2020, 1) as the string "202001"
 * object IsoBasicYearMonthSerializer :
 *     FormattedYearMonthSerializer("my.package.ISO_BASIC", YearMonth.Format { year(); monthNumber() })
 * ```
 *
 * Note that [YearMonth] is [kotlinx.serialization.Serializable] by default,
 * so it is not necessary to create custom serializers when the format is not important.
 * Additionally, [YearMonthIso8601Serializer] is provided for the ISO 8601 format.
 */
public abstract class FormattedYearMonthSerializer(
    name: String, format: DateTimeFormat<YearMonth>
) : KSerializer<YearMonth> by format.asKSerializer("kotlinx.datetime.YearMonth/serializer/$name")

/**
 * A serializer for [YearMonth] that uses the default [YearMonth.toString]/[YearMonth.parse].
 *
 * JSON example: `"2020-01"`
 */
@PublishedApi
internal object YearMonthSerializer: KSerializer<YearMonth> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.YearMonth", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): YearMonth =
        YearMonth.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: YearMonth) {
        encoder.encodeString(value.toString())
    }

}
