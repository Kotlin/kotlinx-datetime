/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class FormatTest {
    @Test
    fun testFindGreediness() {
        val format = LocalTimeFormat.fromFormatString("hh:mm(|:ss)")
        val text = """
            Today at 13:40, I am going to have dinner. Looking at my watch, I see that it is 13:39:30 already.
            I'm afraid there is a very little time left for me to finish this text. Whoops, it's 13:40:15 already!
            No dinner for me today.
        """.trimIndent()
        val matches = format.findAll(text).toList()
        assertEquals(listOf(LocalTime(13, 40), LocalTime(13, 39, 30), LocalTime(13, 40, 15)), matches)
        assertEquals(LocalTime(13, 39, 30), format.find(text, 10))
    }

    @Test
    fun testFindAllNonIntersection() {
        val format = LocalTimeFormat.fromFormatString("hh:mm")
        val text = """
            Then again, what kind of a person eats dinner at 13:40? I'm not a barbarian, I dine at 19:15:00.
            13:40 is a time for a snack, not a dinner.
        """.trimIndent()
        val matches = format.findAll(text).toList()
        assertEquals(listOf(LocalTime(13, 40), LocalTime(19, 15), LocalTime(13, 40)), matches)
    }

    @Test
    fun testFindNotInMiddleOfNumbers() {
        val format = LocalTimeFormat.fromFormatString("hh:m")
        val text = """
            hey can I order a dinner for 102:1: me, 2: my best friend, the other 100: for my other friends?
            at 02:2 please
        """.trimIndent()
        val matches = format.findAll(text).toList()
        assertEquals(listOf(LocalTime(2, 2)), matches)
        assertEquals(LocalTime(2, 2), format.find(text))
    }

    @Test
    fun testFindIncorrectValues() {
        val format = LocalTimeFormat.fromFormatString("hh:mm")
        val text = """
            I know that 25:12:34 is not a valid time, but I'm still going to try to order a dinner at that time.
        """.trimIndent()
        val matches = format.findAll(text).toList()
        assertEquals(emptyList(), matches)
        assertNull(format.find(text))
    }
}
