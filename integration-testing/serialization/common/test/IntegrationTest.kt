/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serialization.test

import kotlinx.datetime.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.test.Test
import kotlin.test.assertEquals

class IntegrationTest {

    @Serializable
    data class Dummy(
        @Contextual val instant: Instant,
        @Contextual val date: LocalDate,
        @Contextual val dateTime: LocalDateTime,
        @Contextual val datePeriod: DatePeriod,
        @Contextual val dateTimePeriod: DateTimePeriod,
        // @Contextual val dayOfWeek: DayOfWeek, // doesn't compile on Native
        // @Contextual val month: Month,
        )

    private val module = SerializersModule {
        contextual(InstantComponentSerializer)
        contextual(LocalDateComponentSerializer)
        contextual(LocalDateTimeComponentSerializer)
        contextual(DatePeriodComponentSerializer)
        contextual(DateTimePeriodComponentSerializer)
        contextual(DayOfWeekSerializer)
        contextual(MonthSerializer)
    }

    private val format = Json { serializersModule = module }

    @Test
    fun testContextualSerialization() {
        val dummyValue = Dummy(
            Instant.parse("2021-03-24T01:29:30.123456789Z"),
            LocalDate.parse("2020-01-02"),
            LocalDateTime.parse("2020-01-03T12:59:58.010203045"),
            DatePeriod.parse("P20Y-2M-3D"),
            DateTimePeriod.parse("-P50Y-1M-2DT3H4M5.0123S"),
            // DayOfWeek.MONDAY, // doesn't compile on Native
            // Month.DECEMBER,
        )
        val json = """{"instant":{"epochSeconds":1616549370,"nanosecondsOfSecond":123456789},""" +
                """"date":{"year":2020,"month":1,"day":2},""" +
                """"dateTime":{"year":2020,"month":1,"day":3,"hour":12,"minute":59,"second":58,"nanosecond":10203045},""" +
                """"datePeriod":{"years":19,"months":10,"days":-3},""" +
                """"dateTimePeriod":{"years":-49,"months":-11,"days":2,"hours":-3,"minutes":-4,"seconds":-5,"nanoseconds":-12300000}}"""
        assertEquals(dummyValue, format.decodeFromString(json))
        assertEquals(json, format.encodeToString(dummyValue))
    }

    @Serializable
    data class Dummy2(
        val instant: Instant,
        val date: LocalDate,
        val dateTime: LocalDateTime,
        val datePeriod: DatePeriod,
        val dateTimePeriod: DateTimePeriod,
        // val dayOfWeek: DayOfWeek,
        // val month: Month,
    )

    @Test
    fun testDefaultSerialization() {
        val dummyValue = Dummy2(
            Instant.parse("2021-03-24T01:29:30.123456789Z"),
            LocalDate.parse("2020-01-02"),
            LocalDateTime.parse("2020-01-03T12:59:58.010203045"),
            DatePeriod.parse("P20Y-2M-3D"),
            DateTimePeriod.parse("-P50Y-1M-2DT3H4M5.0123S"),
            // DayOfWeek.MONDAY,
            // Month.DECEMBER,
        )
        val json = "{\"instant\":\"2021-03-24T01:29:30.123456789Z\"," +
                "\"date\":\"2020-01-02\"," +
                "\"dateTime\":\"2020-01-03T12:59:58.010203045\"," +
                "\"datePeriod\":\"P19Y10M-3D\"," +
                "\"dateTimePeriod\":\"P-49Y-11M2DT-3H-4M-5.012300000S\"" +
                "}"
        assertEquals(dummyValue, Json.decodeFromString(json))
        assertEquals(json, Json.encodeToString(dummyValue))
    }

    @Serializable
    data class Dummy3(
        @Serializable(with = InstantComponentSerializer::class) val instant: Instant,
        @Serializable(with = LocalDateComponentSerializer::class) val date: LocalDate,
        @Serializable(with = LocalDateTimeComponentSerializer::class) val dateTime: LocalDateTime,
        @Serializable(with = DatePeriodComponentSerializer::class) val datePeriod: DatePeriod,
        @Serializable(with = DateTimePeriodComponentSerializer::class) val dateTimePeriod: DateTimePeriod,
        @Serializable(with = DayOfWeekSerializer::class) val dayOfWeek: DayOfWeek,
        @Serializable(with = MonthSerializer::class) val month: Month,
    )

    @Test
    fun testExplicitSerializerSpecification() {
        val dummyValue = Dummy3(
            Instant.parse("2021-03-24T01:29:30.123456789Z"),
            LocalDate.parse("2020-01-02"),
            LocalDateTime.parse("2020-01-03T12:59:58.010203045"),
            DatePeriod.parse("P20Y-2M-3D"),
            DateTimePeriod.parse("-P50Y-1M-2DT3H4M5.0123S"),
            DayOfWeek.MONDAY,
            Month.DECEMBER,
        )
        val json = """{"instant":{"epochSeconds":1616549370,"nanosecondsOfSecond":123456789},""" +
                """"date":{"year":2020,"month":1,"day":2},""" +
                """"dateTime":{"year":2020,"month":1,"day":3,"hour":12,"minute":59,"second":58,"nanosecond":10203045},""" +
                """"datePeriod":{"years":19,"months":10,"days":-3},""" +
                """"dateTimePeriod":{"years":-49,"months":-11,"days":2,"hours":-3,"minutes":-4,"seconds":-5,"nanoseconds":-12300000},""" +
                """"dayOfWeek":"MONDAY",""" +
                """"month":"DECEMBER"""" +
                """}"""
        assertEquals(dummyValue, format.decodeFromString(json))
        assertEquals(json, format.encodeToString(dummyValue))
    }
}