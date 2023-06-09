/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class ValueBagFormatTest {
    @Test
    fun testRfc1123() {
        val bags = buildMap<ValueBag, Pair<String, Set<String>>> {
            put(valueBag(LocalDate(2008, 6, 3), LocalTime(11, 5, 30), UtcOffset.ZERO), ("Tue, 3 Jun 2008 11:05:30 GMT" to setOf()))
            put(valueBag(LocalDate(2008, 6, 30), LocalTime(11, 5, 30), UtcOffset.ZERO), ("Mon, 30 Jun 2008 11:05:30 GMT" to setOf()))
            put(valueBag(LocalDate(2008, 6, 3), LocalTime(11, 5, 30), UtcOffset(hours = 2)), ("Tue, 3 Jun 2008 11:05:30 +0200" to setOf()))
            put(valueBag(LocalDate(2008, 6, 30), LocalTime(11, 5, 30), UtcOffset(hours = -3)), ("Mon, 30 Jun 2008 11:05:30 -0300" to setOf()))
        }
        test(bags, ValueBag.Format.RFC_1123)
    }

    private fun valueBag(
        date: LocalDate? = null,
        time: LocalTime? = null,
        offset: UtcOffset? = null,
        zone: TimeZone? = null
    ) = ValueBag().apply {
        date?.let { populateFrom(it) }
        time?.let { populateFrom(it) }
        offset?.let { populateFrom(it) }
        timeZoneId = zone?.id
    }

    private fun assertValueBagsEqual(a: ValueBag, b: ValueBag, message: String? = null) {
        assertEquals(a.toLocaldate(), b.toLocaldate(), message)
        assertEquals(a.toLocalTime(), b.toLocalTime(), message)
        assertEquals(a.toUtcOffset(), b.toUtcOffset(), message)
        assertEquals(a.timeZoneId, b.timeZoneId, message)
    }

    private fun test(strings: Map<ValueBag, Pair<String, Set<String>>>, format: Format<ValueBag>) {
        for ((value, stringsForValue) in strings) {
            val (canonicalString, otherStrings) = stringsForValue
            assertEquals(canonicalString, format.format(value), "formatting $value with $format")
            assertValueBagsEqual(value, format.parse(canonicalString), "parsing '$canonicalString' with $format")
            for (otherString in otherStrings) {
                assertValueBagsEqual(value, format.parse(otherString), "parsing '$otherString' with $format")
            }
        }
    }
}
