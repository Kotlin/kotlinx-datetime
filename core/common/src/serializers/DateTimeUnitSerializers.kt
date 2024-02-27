/*
 * Copyright 2019-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlin.reflect.KClass

/**
 * A serializer for [DateTimeUnit.TimeBased] unit that represents the unit as a [Long] number of nanoseconds.
 *
 * JSON example: `{"nanoseconds":1000000000}`
 */
public object TimeBasedDateTimeUnitSerializer: KSerializer<DateTimeUnit.TimeBased> {

    // https://youtrack.jetbrains.com/issue/KT-63939
    override val descriptor: SerialDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildClassSerialDescriptor("kotlinx.datetime.TimeBased") {
            element<Long>("nanoseconds")
        }
    }

    override fun serialize(encoder: Encoder, value: DateTimeUnit.TimeBased) {
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.nanoseconds)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): DateTimeUnit.TimeBased {
        var seen = false
        var nanoseconds = 0L
        decoder.decodeStructure(descriptor) {
            if (decodeSequentially()) {
                nanoseconds = decodeLongElement(descriptor, 0)
                seen = true
            } else {
                loop@while (true) {
                    when (val elementIndex: Int = decodeElementIndex(descriptor)) {
                        0 -> {
                            nanoseconds = decodeLongElement(descriptor, 0)
                            seen = true
                        }
                        CompositeDecoder.DECODE_DONE -> break@loop // https://youtrack.jetbrains.com/issue/KT-42262
                        else -> throwUnknownIndexException(elementIndex)
                    }
                }
            }
        }
        if (!seen) throw MissingFieldException(missingField = "nanoseconds", serialName = descriptor.serialName)
        return DateTimeUnit.TimeBased(nanoseconds)
    }
}

/**
 * A serializer for [DateTimeUnit.DayBased] unit that represents the unit as an [Int] number of days.
 *
 * JSON example: `{"days":2}`
 */
public object DayBasedDateTimeUnitSerializer: KSerializer<DateTimeUnit.DayBased> {

    // https://youtrack.jetbrains.com/issue/KT-63939
    override val descriptor: SerialDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildClassSerialDescriptor("kotlinx.datetime.DayBased") {
            element<Int>("days")
        }
    }

    override fun serialize(encoder: Encoder, value: DateTimeUnit.DayBased) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.days)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): DateTimeUnit.DayBased {
        var seen = false
        var days = 0
        decoder.decodeStructure(descriptor) {
            if (decodeSequentially()) {
                days = decodeIntElement(descriptor, 0)
                seen = true
            } else {
                loop@while (true) {
                    when (val elementIndex: Int = decodeElementIndex(descriptor)) {
                        0 -> {
                            days = decodeIntElement(descriptor, 0)
                            seen = true
                        }
                        CompositeDecoder.DECODE_DONE -> break@loop // https://youtrack.jetbrains.com/issue/KT-42262
                        else -> throwUnknownIndexException(elementIndex)
                    }
                }
            }
        }
        if (!seen) throw MissingFieldException(missingField = "days", serialName = descriptor.serialName)
        return DateTimeUnit.DayBased(days)
    }
}

/**
 * A serializer for [DateTimeUnit.MonthBased] unit that represents the unit as an [Int] number of months.
 *
 * JSON example: `{"months":2}`
 */
public object MonthBasedDateTimeUnitSerializer: KSerializer<DateTimeUnit.MonthBased> {

    // https://youtrack.jetbrains.com/issue/KT-63939
    override val descriptor: SerialDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildClassSerialDescriptor("kotlinx.datetime.MonthBased") {
            element<Int>("months")
        }
    }

    override fun serialize(encoder: Encoder, value: DateTimeUnit.MonthBased) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.months)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): DateTimeUnit.MonthBased {
        var seen = false
        var months = 0
        decoder.decodeStructure(descriptor) {
            if (decodeSequentially()) {
                months = decodeIntElement(descriptor, 0)
                seen = true
            } else {
                loop@while (true) {
                    when (val elementIndex: Int = decodeElementIndex(descriptor)) {
                        0 -> {
                            months = decodeIntElement(descriptor, 0)
                            seen = true
                        }
                        CompositeDecoder.DECODE_DONE -> break@loop // https://youtrack.jetbrains.com/issue/KT-42262
                        else -> throwUnknownIndexException(elementIndex)
                    }
                }
            }
        }
        if (!seen) throw MissingFieldException(missingField = "months", serialName = descriptor.serialName)
        return DateTimeUnit.MonthBased(months)
    }
}

/**
 * A polymorphic serializer for [DateTimeUnit.DateBased] unit that represents the unit as an [Int] number of months or days.
 *
 * JSON example: `{"type":"DayBased","days":15}`
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE") // https://github.com/Kotlin/kotlinx.serialization/issues/2460
@OptIn(InternalSerializationApi::class)
public object DateBasedDateTimeUnitSerializer: AbstractPolymorphicSerializer<DateTimeUnit.DateBased>() {

    // https://youtrack.jetbrains.com/issue/KT-63939
    private val impl by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SealedClassSerializer("kotlinx.datetime.DateTimeUnit.DateBased",
            DateTimeUnit.DateBased::class,
            arrayOf(DateTimeUnit.DayBased::class, DateTimeUnit.MonthBased::class),
            arrayOf(DayBasedDateTimeUnitSerializer, MonthBasedDateTimeUnitSerializer))
    }

    @InternalSerializationApi
    override fun findPolymorphicSerializerOrNull(decoder: CompositeDecoder, klassName: String?):
            DeserializationStrategy<DateTimeUnit.DateBased>? =
        impl.findPolymorphicSerializerOrNull(decoder, klassName)

    @InternalSerializationApi
    override fun findPolymorphicSerializerOrNull(encoder: Encoder, value: DateTimeUnit.DateBased):
            SerializationStrategy<DateTimeUnit.DateBased>? =
        impl.findPolymorphicSerializerOrNull(encoder, value)

    override val baseClass: KClass<DateTimeUnit.DateBased>
        get() = DateTimeUnit.DateBased::class

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor
        get() = impl.descriptor

}

/**
 * A polymorphic serializer for [DateTimeUnit] that represents the unit as the [Int] number of months or days, or
 * the [Long] number of nanoseconds.
 *
 * JSON example: `{"type":"MonthBased","days":15}`
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE") // https://github.com/Kotlin/kotlinx.serialization/issues/2460
@OptIn(InternalSerializationApi::class)
public object DateTimeUnitSerializer : AbstractPolymorphicSerializer<DateTimeUnit>() {

    // https://youtrack.jetbrains.com/issue/KT-63939
    private val impl by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SealedClassSerializer("kotlinx.datetime.DateTimeUnit",
            DateTimeUnit::class,
            arrayOf(DateTimeUnit.DayBased::class, DateTimeUnit.MonthBased::class, DateTimeUnit.TimeBased::class),
            arrayOf(DayBasedDateTimeUnitSerializer, MonthBasedDateTimeUnitSerializer, TimeBasedDateTimeUnitSerializer))
    }

    @InternalSerializationApi
    override fun findPolymorphicSerializerOrNull(decoder: CompositeDecoder, klassName: String?): DeserializationStrategy<DateTimeUnit>? =
        impl.findPolymorphicSerializerOrNull(decoder, klassName)

    @InternalSerializationApi
    override fun findPolymorphicSerializerOrNull(encoder: Encoder, value: DateTimeUnit): SerializationStrategy<DateTimeUnit>? =
        impl.findPolymorphicSerializerOrNull(encoder, value)

    override val baseClass: KClass<DateTimeUnit>
        get() = DateTimeUnit::class

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor
        get() = impl.descriptor

}

internal fun throwUnknownIndexException(index: Int): Nothing {
    throw SerializationException("An unknown field for index $index")
}
