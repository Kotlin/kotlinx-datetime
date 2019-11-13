/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test
import kotlinx.datetime.*
import kotlin.random.Random
import kotlin.test.*
import kotlin.time.*

class InstantTest {
    @Test
    fun testNow() {
        val instant = Instant.now()
        val millis = instant.toUnixMillis()

        assertTrue(millis > 1_500_000_000_000L)

        println(instant)
        println(instant.toUnixMillis())

        val millisInstant = Instant.fromUnixMillis(millis)

        assertEquals(millis, millisInstant.toUnixMillis())

        val notEqualInstant = Instant.fromUnixMillis(millis + 1)
        assertNotEquals(notEqualInstant, instant)
    }

    @UseExperimental(ExperimentalTime::class)
    @Test
    fun instantArithmetic() {
        val instant = Instant.now().toUnixMillis().let { Instant.fromUnixMillis(it) } // round to millis
        val diffMillis = Random.nextLong(1000, 1_000_000_000)
        val diff = diffMillis.milliseconds

        val nextInstant = (instant.toUnixMillis() + diffMillis).let { Instant.fromUnixMillis(it) }

        assertEquals(diff, nextInstant - instant)
        assertEquals(nextInstant, instant + diff)
        assertEquals(instant, nextInstant - diff)

        println("this: $instant, next: $nextInstant, diff: ${diff.toIsoString()}")
    }

    @Test
    fun instantToLocalDTConversion() {
        val now = Instant.now()
        println(now.toLocalDateTime(TimeZone.UTC))
        println(now.toLocalDateTime(TimeZone.SYSTEM))
    }



    @Test
    fun instantParsing() {
        val instant = Instant.parse("2019-10-09T09:02:00.123Z")

        assertEquals(1570611720_123L, instant.toUnixMillis())
    }

}