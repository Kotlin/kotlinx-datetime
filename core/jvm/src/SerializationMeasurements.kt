/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.system.*

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.US_ASCII
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.random.*

const val REPETITIONS = 1

@ExperimentalSerializationApi
inline fun<reified T: Any> protobufMeasurements(serializer: KSerializer<T>, generator: () -> Array<T>) {
    repeat(REPETITIONS) {
        val values = generator()
        val result: ByteArray
        val time = measureTimeMillis {
            result = ProtoBuf.encodeToByteArray(ArraySerializer(serializer), values)
        }
        val newValues: Array<T>
        val time2 = measureTimeMillis {
            newValues = ProtoBuf.decodeFromByteArray(ArraySerializer(serializer), result)
        }
        assert(values.contentEquals(newValues))
        if (it == REPETITIONS - 1) {
            println("time (ser): $time\ttime (des): $time2\tbytes: ${result.size}\tfor $serializer")
        }
    }
}

@ExperimentalSerializationApi
inline fun<reified T: Any> jsonMeasurements(serializer: KSerializer<T>, generator: () -> Array<T>) {
    repeat(REPETITIONS) {
        val values = generator()
        val result: String
        val time = measureTimeMillis {
            result = Json.encodeToString(ArraySerializer(serializer), values)
        }
        val newValues: Array<T>
        val time2 = measureTimeMillis {
            newValues = Json.decodeFromString(ArraySerializer(serializer), result)
        }
        assert(values.contentEquals(newValues))
        if (it == REPETITIONS - 1) {
            println("time (ser): $time\ttime (des): $time2\tbytes: ${result.toByteArray().size}\tfor $serializer")
        }
    }
}

@ExperimentalSerializationApi
inline fun<reified T: Any> gzippedProtobufMeasurements(serializer: KSerializer<T>, generator: () -> Array<T>) {
    repeat(REPETITIONS) {
        val values = generator()
        val result: ByteArray
        val time = measureTimeMillis {
            val bos = ByteArrayOutputStream()
            GZIPOutputStream(bos).use {
                it.write(ProtoBuf.encodeToByteArray(ArraySerializer(serializer), values))
            }
            result = bos.toByteArray()
        }
        val newValues: Array<T>
        val time2 = measureTimeMillis {
            val ungzipped = GZIPInputStream(result.inputStream()).use { it.readBytes() }
            newValues = ProtoBuf.decodeFromByteArray(ArraySerializer(serializer), ungzipped)
        }
        assert(values.contentEquals(newValues))
        if (it == REPETITIONS - 1) {
            println("time (ser): $time\ttime (des): $time2\tbytes: ${result.size}\tfor $serializer")
        }
    }
}

@ExperimentalSerializationApi
inline fun<reified T: Any> gzippedJsonMeasurements(serializer: KSerializer<T>, generator: () -> Array<T>) {
    repeat(REPETITIONS) {
        val values = generator()
        val result: ByteArray
        val time = measureTimeMillis {
            val bos = ByteArrayOutputStream()
            GZIPOutputStream(bos).bufferedWriter(US_ASCII).use {
                it.write(Json.encodeToString(ArraySerializer(serializer), values))
            }
            result = bos.toByteArray()
        }
        val newValues: Array<T>
        val time2 = measureTimeMillis {
            val ungzipped = GZIPInputStream(result.inputStream()).bufferedReader(US_ASCII).use { it.readText() }
            newValues = Json.decodeFromString(ArraySerializer(serializer), ungzipped)
        }
        assert(values.contentEquals(newValues))
        if (it == REPETITIONS - 1) {
            println("time (ser): $time\ttime (des): $time2\tbytes: ${result.size}\tfor $serializer")
        }
    }
}

@ExperimentalSerializationApi
inline fun<reified T: Any> measurements(vararg serializers: KSerializer<T>, crossinline generator: () -> Array<T>) {
    println("Protobuf:")
    for (serializer in serializers) {
        protobufMeasurements(serializer, generator)
    }
    println("JSON:")
    for (serializer in serializers) {
        jsonMeasurements(serializer, generator)
    }
    println("Gzipped Protobuf:")
    for (serializer in serializers) {
        gzippedProtobufMeasurements(serializer, generator)
    }
    println("Gzipped JSON:")
    for (serializer in serializers) {
        gzippedJsonMeasurements(serializer, generator)
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    measurements(InstantSerializer, InstantISO8601Serializer) {
        Array(10_000) { Clock.System.now() }
    }
    measurements(LocalDateSerializer, LocalDateISO8601Serializer, LocalDateLongSerializer) {
        Array(10_000) { Clock.System.now().toLocalDateTime(TimeZone.UTC).date }
    }
    measurements(LocalDateTimeSerializer, LocalDateTimeISO8601Serializer, LocalDateTimeCompactSerializer) {
        Array(10_000) { Clock.System.now().toLocalDateTime(TimeZone.UTC) }
    }
    measurements(MonthIntSerializer) {
        Array(10_000) { Month(Random.nextInt(1, 13)) }
    }

    val period = DatePeriod(10, 15, 20)
    println(ProtoBuf.decodeFromByteArray(DateTimePeriod.serializer(), ProtoBuf.encodeToByteArray(period as DateTimePeriod)))
    println(Json.decodeFromString(DateTimePeriod.serializer(), """{}"""))
    println(Json.encodeToString(period))
    println(Json.encodeToString(period as DateTimePeriod))
    println(Json.encodeToString(period as DateTimePeriod))
    println(Json.decodeFromString(DateTimePeriod.serializer(), """{"years":10,"months":15,"days":20,"hours":0,"minutes":0,"seconds":0,"nanoseconds":0}"""))
    println(Json.decodeFromString(DatePeriod.serializer(), """{"years":10,"months":15,"days":20,"hours":0,"minutes":0,"seconds":0,"nanoseconds":0}"""))
    println(Json.decodeFromString(DateTimePeriod.serializer(), """{"years":10,"months":15,"days":20}"""))
    println(Json.decodeFromString(DatePeriod.serializer(), """{"years":10,"months":15,"days":20}"""))
    val unit = DateTimeUnit.MICROSECOND * 3
    println(Json.encodeToString(unit))
    println(Json.encodeToString(unit as DateTimeUnit))
    println(Json.decodeFromString(DateTimeUnit.TimeBased.serializer(), """{"nanoseconds":3000}"""))
    println(Json.decodeFromString(DateTimeUnit.serializer(), """{"type":"TimeBased","nanoseconds":3000}"""))
    val unit2 = DateTimeUnit.DAY * 2
    println(Json.encodeToString(unit2))
    println(Json.encodeToString(unit2 as DateTimeUnit))
    println(Json.decodeFromString(DateTimeUnit.serializer(), """{"type":"DayBased","days":2}"""))

    println(ProtoBuf.decodeFromByteArray(DateTimePeriod.serializer(), ProtoBuf.encodeToByteArray(period as DateTimePeriod)))
}
