/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.DateTimeUnit
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlin.reflect.KClass

public object TimeBasedDateTimeUnitSerializer: KSerializer<DateTimeUnit.TimeBased> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TimeBased") {
        element<Long>("nanoseconds")
    }

    override fun serialize(encoder: Encoder, value: DateTimeUnit.TimeBased) {
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.nanoseconds)
        }
    }

    @ExperimentalSerializationApi
    @Suppress("INVISIBLE_MEMBER") // to be able to throw `MissingFieldException`
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
                        else -> throw UnknownFieldException(elementIndex)
                    }
                }
            }
        }
        if (!seen) throw MissingFieldException("nanoseconds")
        return DateTimeUnit.TimeBased(nanoseconds)
    }
}

public object DayBasedDateTimeUnitSerializer: KSerializer<DateTimeUnit.DateBased.DayBased> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DayBased") {
        element<Int>("days")
    }

    override fun serialize(encoder: Encoder, value: DateTimeUnit.DateBased.DayBased) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.days)
        }
    }

    @ExperimentalSerializationApi
    @Suppress("INVISIBLE_MEMBER") // to be able to throw `MissingFieldException`
    override fun deserialize(decoder: Decoder): DateTimeUnit.DateBased.DayBased {
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
                        else -> throw UnknownFieldException(elementIndex)
                    }
                }
            }
        }
        if (!seen) throw MissingFieldException("days")
        return DateTimeUnit.DateBased.DayBased(days)
    }
}

public object MonthBasedDateTimeUnitSerializer: KSerializer<DateTimeUnit.DateBased.MonthBased> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MonthBased") {
        element<Int>("months")
    }

    override fun serialize(encoder: Encoder, value: DateTimeUnit.DateBased.MonthBased) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.months)
        }
    }

    @ExperimentalSerializationApi
    @Suppress("INVISIBLE_MEMBER") // to be able to throw `MissingFieldException`
    override fun deserialize(decoder: Decoder): DateTimeUnit.DateBased.MonthBased {
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
                        else -> throw UnknownFieldException(elementIndex)
                    }
                }
            }
        }
        if (!seen) throw MissingFieldException("months")
        return DateTimeUnit.DateBased.MonthBased(months)
    }
}

@Suppress("EXPERIMENTAL_API_USAGE_ERROR", "INVISIBLE_MEMBER")
public object DateBasedDateTimeUnitSerializer: AbstractPolymorphicSerializer<DateTimeUnit.DateBased>() {

    private val impl = SealedClassSerializer("kotlinx.datetime.DateTimeUnit.DateBased",
        DateTimeUnit.DateBased::class,
        arrayOf(DateTimeUnit.DateBased.DayBased::class, DateTimeUnit.DateBased.MonthBased::class),
        arrayOf(DayBasedDateTimeUnitSerializer, MonthBasedDateTimeUnitSerializer))

    @InternalSerializationApi
    override fun findPolymorphicSerializerOrNull(decoder: CompositeDecoder, klassName: String?):
            DeserializationStrategy<out DateTimeUnit.DateBased>? =
        impl.findPolymorphicSerializerOrNull(decoder, klassName)

    @InternalSerializationApi
    override fun findPolymorphicSerializerOrNull(encoder: Encoder, value: DateTimeUnit.DateBased):
            SerializationStrategy<DateTimeUnit.DateBased>? =
        impl.findPolymorphicSerializerOrNull(encoder, value)

    @InternalSerializationApi
    override val baseClass: KClass<DateTimeUnit.DateBased>
        get() = DateTimeUnit.DateBased::class

    @InternalSerializationApi
    override val descriptor: SerialDescriptor
        get() = impl.descriptor

}

@Suppress("EXPERIMENTAL_API_USAGE_ERROR", "INVISIBLE_MEMBER")
public object DateTimeUnitSerializer: AbstractPolymorphicSerializer<DateTimeUnit>() {

    private val impl = SealedClassSerializer("kotlinx.datetime.DateTimeUnit",
        DateTimeUnit::class,
        arrayOf(DateTimeUnit.DateBased.DayBased::class, DateTimeUnit.DateBased.MonthBased::class, DateTimeUnit.TimeBased::class),
        arrayOf(DayBasedDateTimeUnitSerializer, MonthBasedDateTimeUnitSerializer, TimeBasedDateTimeUnitSerializer))

    @InternalSerializationApi
    override fun findPolymorphicSerializerOrNull(decoder: CompositeDecoder, klassName: String?): DeserializationStrategy<out DateTimeUnit>? =
        impl.findPolymorphicSerializerOrNull(decoder, klassName)

    @InternalSerializationApi
    override fun findPolymorphicSerializerOrNull(encoder: Encoder, value: DateTimeUnit): SerializationStrategy<DateTimeUnit>? =
        impl.findPolymorphicSerializerOrNull(encoder, value)

    @InternalSerializationApi
    override val baseClass: KClass<DateTimeUnit>
        get() = DateTimeUnit::class

    @InternalSerializationApi
    override val descriptor: SerialDescriptor
        get() = impl.descriptor

}
