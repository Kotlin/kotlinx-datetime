/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlin.reflect.*
import kotlin.time.*

@Serializable(with = DateTimeUnitSerializer::class)
sealed class DateTimeUnit {

    abstract operator fun times(scalar: Int): DateTimeUnit

    @Serializable(with = TimeBasedSerializer::class)
    class TimeBased(val nanoseconds: Long) : DateTimeUnit() {

        /* fields without a default value can't be @Transient, so the more natural way of writing this
        (setting [unitName] and [unitScale] in init { ... }) won't work:
        https://github.com/Kotlin/kotlinx.serialization/issues/1227. */
        @Transient
        private val unitName: String = when {
            nanoseconds % 3600_000_000_000 == 0L -> "HOUR"
            nanoseconds % 60_000_000_000 == 0L -> "MINUTE"
            nanoseconds % 1_000_000_000 == 0L -> "SECOND"
            nanoseconds % 1_000_000 == 0L -> "MILLISECOND"
            nanoseconds % 1_000 == 0L -> "MICROSECOND"
            else -> "NANOSECOND"
        }

        @Transient
        private val unitScale: Long = when {
            nanoseconds % 3600_000_000_000 == 0L -> nanoseconds / 3600_000_000_000
            nanoseconds % 60_000_000_000 == 0L -> nanoseconds / 60_000_000_000
            nanoseconds % 1_000_000_000 == 0L -> nanoseconds / 1_000_000_000
            nanoseconds % 1_000_000 == 0L -> nanoseconds / 1_000_000
            nanoseconds % 1_000 == 0L -> nanoseconds / 1_000
            else -> nanoseconds
        }

        init {
            require(nanoseconds > 0) { "Unit duration must be positive, but was $nanoseconds ns." }
        }

        override fun times(scalar: Int): TimeBased = TimeBased(safeMultiply(nanoseconds, scalar.toLong()))

        @ExperimentalTime
        val duration: Duration
            get() = nanoseconds.nanoseconds

        override fun equals(other: Any?): Boolean =
                this === other || (other is TimeBased && this.nanoseconds == other.nanoseconds)

        override fun hashCode(): Int = nanoseconds.toInt() xor (nanoseconds shr Int.SIZE_BITS).toInt()

        override fun toString(): String = formatToString(unitScale, unitName)
    }

    @Serializable(with = DateBasedSerializer::class)
    sealed class DateBased : DateTimeUnit() {
        // TODO: investigate how to move subclasses up to DateTimeUnit scope
        @Serializable(with = DayBasedSerializer::class)
        class DayBased(val days: Int) : DateBased() {
            init {
                require(days > 0) { "Unit duration must be positive, but was $days days." }
            }

            override fun times(scalar: Int): DayBased = DayBased(safeMultiply(days, scalar))

            override fun equals(other: Any?): Boolean =
                    this === other || (other is DayBased && this.days == other.days)

            override fun hashCode(): Int = days xor 0x10000

            override fun toString(): String = if (days % 7 == 0)
                formatToString(days / 7, "WEEK")
            else
                formatToString(days, "DAY")
        }
        @Serializable(with = MonthBasedSerializer::class)
        class MonthBased(val months: Int) : DateBased() {
            init {
                require(months > 0) { "Unit duration must be positive, but was $months months." }
            }

            override fun times(scalar: Int): MonthBased = MonthBased(safeMultiply(months, scalar))

            override fun equals(other: Any?): Boolean =
                    this === other || (other is MonthBased && this.months == other.months)

            override fun hashCode(): Int = months xor 0x20000

            override fun toString(): String = when {
                months % 12_00 == 0 -> formatToString(months / 12_00, "CENTURY")
                months % 12 == 0 -> formatToString(months / 12, "YEAR")
                months % 3 == 0 -> formatToString(months / 3, "QUARTER")
                else -> formatToString(months, "MONTH")
            }
        }
    }

    protected fun formatToString(value: Int, unit: String): String = if (value == 1) unit else "$value-$unit"
    protected fun formatToString(value: Long, unit: String): String = if (value == 1L) unit else "$value-$unit"

    companion object {
        val NANOSECOND = TimeBased(nanoseconds = 1)
        val MICROSECOND = NANOSECOND * 1000
        val MILLISECOND = MICROSECOND * 1000
        val SECOND = MILLISECOND * 1000
        val MINUTE = SECOND * 60
        val HOUR = MINUTE * 60
        val DAY = DateBased.DayBased(days = 1)
        val WEEK = DAY * 7
        val MONTH = DateBased.MonthBased(months = 1)
        val QUARTER = MONTH * 3
        val YEAR = MONTH * 12
        val CENTURY = YEAR * 100
    }
}

object TimeBasedSerializer: KSerializer<DateTimeUnit.TimeBased> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TimeBased") {
        element<Long>("nanoseconds")
    }

    override fun serialize(encoder: Encoder, value: DateTimeUnit.TimeBased) {
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.nanoseconds);
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
                while (true) {
                    when (val elementIndex: Int = decodeElementIndex(descriptor)) {
                        0 -> {
                            nanoseconds = decodeLongElement(descriptor, 0)
                            seen = true
                        }
                        CompositeDecoder.DECODE_DONE -> break
                        else -> throw UnknownFieldException(elementIndex)
                    }
                }
            }
        }
        if (!seen) throw MissingFieldException("nanoseconds")
        return DateTimeUnit.TimeBased(nanoseconds)
    }
}

object DayBasedSerializer: KSerializer<DateTimeUnit.DateBased.DayBased> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DayBased") {
        element<Int>("days")
    }

    override fun serialize(encoder: Encoder, value: DateTimeUnit.DateBased.DayBased) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.days);
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
                while (true) {
                    when (val elementIndex: Int = decodeElementIndex(descriptor)) {
                        0 -> {
                            days = decodeIntElement(descriptor, 0)
                            seen = true
                        }
                        CompositeDecoder.DECODE_DONE -> break
                        else -> throw UnknownFieldException(elementIndex)
                    }
                }
            }
        }
        if (!seen) throw MissingFieldException("days")
        return DateTimeUnit.DateBased.DayBased(days)
    }
}

object MonthBasedSerializer: KSerializer<DateTimeUnit.DateBased.MonthBased> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MonthBased") {
        element<Int>("months")
    }

    override fun serialize(encoder: Encoder, value: DateTimeUnit.DateBased.MonthBased) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.months);
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
                while (true) {
                    when (val elementIndex: Int = decodeElementIndex(descriptor)) {
                        0 -> {
                            months = decodeIntElement(descriptor, 0)
                            seen = true
                        }
                        CompositeDecoder.DECODE_DONE -> break
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
object DateBasedSerializer: AbstractPolymorphicSerializer<DateTimeUnit.DateBased>() {

    private val impl = SealedClassSerializer("kotlinx.datetime.DateTimeUnit.DateBased",
        DateTimeUnit.DateBased::class,
        arrayOf(DateTimeUnit.DateBased.DayBased::class, DateTimeUnit.DateBased.MonthBased::class),
        arrayOf(DayBasedSerializer, MonthBasedSerializer))

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
object DateTimeUnitSerializer: AbstractPolymorphicSerializer<DateTimeUnit>() {

    private val impl = SealedClassSerializer("kotlinx.datetime.DateTimeUnit",
        DateTimeUnit::class,
        arrayOf(DateTimeUnit.DateBased.DayBased::class, DateTimeUnit.DateBased.MonthBased::class, DateTimeUnit.TimeBased::class),
        arrayOf(DayBasedSerializer, MonthBasedSerializer, TimeBasedSerializer))

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
