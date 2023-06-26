/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.LocalDate
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * A serializer for [LocalDate] that uses the ISO-8601 representation.
 *
 * JSON example: `"2020-01-01"`
 *
 * @see LocalDate.parse
 * @see LocalDate.toString
 */
public object LocalDateIso8601Serializer: KSerializer<LocalDate> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate =
        LocalDate.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }

}

/**
 * A serializer for [LocalDate] that represents a value as its components.
 *
 * JSON example: `{"year":2020,"month":12,"day":9}`
 */
public object LocalDateComponentSerializer: KSerializer<LocalDate> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("LocalDate") {
            element<Int>("year")
            element<Short>("month")
            element<Short>("day")
        }

    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE") // to be able to throw `MissingFieldException`
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
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            if (year == null) throw MissingFieldException("year")
            if (month == null) throw MissingFieldException("month")
            if (day == null) throw MissingFieldException("day")
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
