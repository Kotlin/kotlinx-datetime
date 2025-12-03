/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.internal.format.parser.ConstantNumberConsumer
import kotlinx.datetime.internal.format.parser.NumberSpanParserOperation
import kotlinx.datetime.internal.format.parser.ParserStructure
import kotlinx.datetime.internal.format.parser.concat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParserStructureConcatenationTest {

    @Test
    fun concatDistributesTopLevelNumberSpanParserOperationIntoBranches() {
        val parser = ParserStructure<Int>(
            operations = listOf(
                NumberSpanParserOperation(listOf(ConstantNumberConsumer("12")))
            ),
            followedBy = listOf(
                ParserStructure(
                    operations = listOf(
                        NumberSpanParserOperation(listOf(ConstantNumberConsumer("34")))
                    ),
                    followedBy = listOf()
                ),
                ParserStructure(
                    operations = listOf(),
                    followedBy = listOf()
                )
            )
        )

        val actual = listOf(parser).concat()

        with(actual) {
            assertEquals(0, operations.size)
            assertEquals(2, followedBy.size)
            with(followedBy[0]) {
                assertEquals(0, followedBy.size)
                with(operations) {
                    assertEquals(1, size)
                    assertTrue(operations[0] is NumberSpanParserOperation)
                    assertEquals(2, (operations[0] as NumberSpanParserOperation).consumers.size)
                }
            }
            with(followedBy[1]) {
                assertEquals(0, followedBy.size)
                with(operations) {
                    assertEquals(1, size)
                    with(operations) {
                        assertEquals(1, size)
                        assertTrue(operations[0] is NumberSpanParserOperation)
                        assertEquals(1, (operations[0] as NumberSpanParserOperation).consumers.size)
                    }
                }
            }
        }
    }
}
