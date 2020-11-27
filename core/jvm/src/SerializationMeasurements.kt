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

@ExperimentalSerializationApi
inline fun<reified T: Any> protobufMeasurements(serializer: KSerializer<T>, generator: () -> Array<T>) {
    val instants = generator()
    var result: ByteArray? = null
    val time = measureTimeMillis {
        result = ProtoBuf.encodeToByteArray(ArraySerializer(serializer), instants)
    }
    val time2 = measureTimeMillis {
        ProtoBuf.decodeFromByteArray(ArraySerializer(serializer), result!!)
    }
    println("time (ser): $time\ttime (des): $time2\tbytes: ${result!!.size}\tfor $serializer")
}

@ExperimentalSerializationApi
inline fun<reified T: Any> jsonMeasurements(serializer: KSerializer<T>, generator: () -> Array<T>) {
    val instants = generator()
    var result: String? = null
    val time = measureTimeMillis {
        result = Json.encodeToString(ArraySerializer(serializer), instants)
    }
    val time2 = measureTimeMillis {
        Json.decodeFromString(ArraySerializer(serializer), result!!)
    }
    println("time (ser): $time\ttime (des): $time2\tbytes: ${result!!.toByteArray().size}\tfor $serializer")
}

@ExperimentalSerializationApi
inline fun<reified T: Any> gzippedJsonMeasurements(serializer: KSerializer<T>, generator: () -> Array<T>) {
    val instants = generator()
    var result: ByteArray? = null
    val time = measureTimeMillis {
        result = gzip(Json.encodeToString(ArraySerializer(serializer), instants))
    }
    val time2 = measureTimeMillis {
        Json.decodeFromString(ArraySerializer(serializer), ungzip(result!!))
    }
    println("time (ser): $time\ttime (des): $time2\tbytes: ${result!!.size}\tfor $serializer")
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
    println("Gzipped JSON:")
    for (serializer in serializers) {
        gzippedJsonMeasurements(serializer, generator)
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    measurements(InstantSerializer, InstantISO8601Serializer, InstantDoubleSerializer) {
        Array(10_000) { Clock.System.now() }
    }
    measurements(LocalDateSerializer, LocalDateISO8601Serializer) {
        Array(10_000) { Clock.System.now().toLocalDateTime(TimeZone.UTC).date }
    }
}

fun gzip(content: String): ByteArray {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(US_ASCII).use { it.write(content) }
    return bos.toByteArray()
}

fun ungzip(content: ByteArray): String =
    GZIPInputStream(content.inputStream()).bufferedReader(US_ASCII).use { it.readText() }
