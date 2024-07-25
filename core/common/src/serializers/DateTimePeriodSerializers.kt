/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * A serializer for [DateTimePeriod] that uses a different field for each component, only listing non-zero components.
 *
 * JSON example: `{"days":1,"hours":-1}`
 */
public object DateTimePeriodComponentSerializer: KSerializer<DateTimePeriod> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlinx.datetime.DateTimePeriod components") {
            element<Int>("years", isOptional = true)
            element<Int>("months", isOptional = true)
            element<Int>("days", isOptional = true)
            element<Int>("hours", isOptional = true)
            element<Int>("minutes", isOptional = true)
            element<Int>("seconds", isOptional = true)
            element<Long>("nanoseconds", isOptional = true)
        }

    override fun deserialize(decoder: Decoder): DateTimePeriod =
        decoder.decodeStructure(descriptor) {
            var years = 0
            var months = 0
            var days = 0
            var hours = 0
            var minutes = 0
            var seconds = 0
            var nanoseconds = 0L
            loop@while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> years = decodeIntElement(descriptor, 0)
                    1 -> months = decodeIntElement(descriptor, 1)
                    2 -> days = decodeIntElement(descriptor, 2)
                    3 -> hours = decodeIntElement(descriptor, 3)
                    4 -> minutes = decodeIntElement(descriptor, 4)
                    5 -> seconds = decodeIntElement(descriptor, 5)
                    6 -> nanoseconds = decodeLongElement(descriptor, 6)
                    CompositeDecoder.DECODE_DONE -> break@loop // https://youtrack.jetbrains.com/issue/KT-42262
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            DateTimePeriod(years, months, days, hours, minutes, seconds, nanoseconds)
        }

    override fun serialize(encoder: Encoder, value: DateTimePeriod) {
        encoder.encodeStructure(descriptor) {
            with(value) {
                if (years != 0) encodeIntElement(descriptor, 0, years)
                if (months != 0) encodeIntElement(descriptor, 1, months)
                if (days != 0) encodeIntElement(descriptor, 2, days)
                if (hours != 0) encodeIntElement(descriptor, 3, hours)
                if (minutes != 0) encodeIntElement(descriptor, 4, minutes)
                if (seconds != 0) encodeIntElement(descriptor, 5, seconds)
                if (nanoseconds != 0) encodeLongElement(descriptor, 6, value.nanoseconds.toLong())
            }
        }
    }

}

/**
 * A serializer for [DateTimePeriod] that represents it as an ISO 8601 duration string.
 *
 * JSON example: `"P1DT-1H"`
 *
 * @see DateTimePeriod.toString
 * @see DateTimePeriod.parse
 */
public object DateTimePeriodIso8601Serializer: KSerializer<DateTimePeriod> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.DateTimePeriod ISO", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): DateTimePeriod =
        DateTimePeriod.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: DateTimePeriod) {
        encoder.encodeString(value.toString())
    }

}

/**
 * A serializer for [DatePeriod] that uses a different field for each component, only listing non-zero components.
 *
 * Deserializes the time components as well when they are present ensuring they are zero.
 *
 * JSON example: `{"months":1,"days":15}`
 */
public object DatePeriodComponentSerializer: KSerializer<DatePeriod> {

    private fun unexpectedNonzero(fieldName: String, value: Long) {
        if (value != 0L) {
            throw SerializationException("DatePeriod should have non-date components be zero, but got $value in '$fieldName'")
        }
    }

    private fun unexpectedNonzero(fieldName: String, value: Int) = unexpectedNonzero(fieldName, value.toLong())

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlinx.datetime.DatePeriod components") {
            element<Int>("years", isOptional = true)
            element<Int>("months", isOptional = true)
            element<Int>("days", isOptional = true)
            element<Int>("hours", isOptional = true)
            element<Int>("minutes", isOptional = true)
            element<Int>("seconds", isOptional = true)
            element<Long>("nanoseconds", isOptional = true)
        }

    override fun deserialize(decoder: Decoder): DatePeriod =
        decoder.decodeStructure(descriptor) {
            var years = 0
            var months = 0
            var days = 0
            loop@while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> years = decodeIntElement(descriptor, 0)
                    1 -> months = decodeIntElement(descriptor, 1)
                    2 -> days = decodeIntElement(descriptor, 2)
                    3 -> unexpectedNonzero("hours", decodeIntElement(descriptor, 3))
                    4 -> unexpectedNonzero("minutes", decodeIntElement(descriptor, 4))
                    5 -> unexpectedNonzero("seconds", decodeIntElement(descriptor, 5))
                    6 -> unexpectedNonzero("nanoseconds", decodeLongElement(descriptor, 6))
                    CompositeDecoder.DECODE_DONE -> break@loop // https://youtrack.jetbrains.com/issue/KT-42262
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            DatePeriod(years, months, days)
        }

    override fun serialize(encoder: Encoder, value: DatePeriod) {
        encoder.encodeStructure(descriptor) {
            with(value) {
                if (years != 0) encodeIntElement(DateTimePeriodComponentSerializer.descriptor, 0, years)
                if (months != 0) encodeIntElement(DateTimePeriodComponentSerializer.descriptor, 1, months)
                if (days != 0) encodeIntElement(DateTimePeriodComponentSerializer.descriptor, 2, days)
            }
        }
    }

}

/**
 * A serializer for [DatePeriod] that represents it as an ISO 8601 duration string.
 *
 * Deserializes the time components as well, as long as they are zero.
 *
 * JSON example: `"P2Y1M"`
 *
 * @see DatePeriod.toString
 * @see DatePeriod.parse
 */
public object DatePeriodIso8601Serializer: KSerializer<DatePeriod> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.DatePeriod ISO", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): DatePeriod =
        when (val period = DateTimePeriod.parse(decoder.decodeString())) {
            is DatePeriod -> period
            else -> throw SerializationException("$period is not a date-based period")
        }

    override fun serialize(encoder: Encoder, value: DatePeriod) {
        encoder.encodeString(value.toString())
    }

}

@PublishedApi
internal object DateTimePeriodSerializer: KSerializer<DateTimePeriod> by DateTimePeriodIso8601Serializer {
    override val descriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.DateTimePeriod", PrimitiveKind.STRING)
}

@PublishedApi
internal object DatePeriodSerializer: KSerializer<DatePeriod> by DatePeriodIso8601Serializer {
    override val descriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.DatePeriod", PrimitiveKind.STRING)
}
