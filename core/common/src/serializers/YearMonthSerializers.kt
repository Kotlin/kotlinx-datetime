/*
 * Copyright 2019-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * A serializer for [YearMonth] that uses the ISO 8601 representation.
 *
 * JSON example: `"2020-01"`
 *
 * @see YearMonth.parse
 * @see YearMonth.toString
 */
public object YearMonthIso8601Serializer: KSerializer<YearMonth> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.YearMonth", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): YearMonth =
        YearMonth.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: YearMonth) {
        encoder.encodeString(value.toString())
    }

}

/**
 * A serializer for [YearMonth] that represents a value as its components.
 *
 * JSON example: `{"year":2020,"month":12}`
 */
public object YearMonthComponentSerializer: KSerializer<YearMonth> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlinx.datetime.LocalDate") {
            element<Int>("year")
            element<Short>("month")
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): YearMonth =
        decoder.decodeStructure(descriptor) {
            var year: Int? = null
            var month: Short? = null
            loop@while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> year = decodeIntElement(descriptor, 0)
                    1 -> month = decodeShortElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break@loop // https://youtrack.jetbrains.com/issue/KT-42262
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
