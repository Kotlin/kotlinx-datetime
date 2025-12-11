/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.internal.format.parser.ConstantNumberConsumer
import kotlinx.datetime.internal.format.parser.NumberSpanParserOperation
import kotlinx.datetime.internal.format.parser.ParserStructure
import kotlinx.datetime.internal.format.parser.UnconditionalModification
import kotlinx.datetime.internal.format.parser.concat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParserStructureConcatenationTest {

    /*
     * Reproducer from https://github.com/Kotlin/kotlinx-datetime/pull/585
     *
     *                                --<N2>--            --<N1-N2>--<U>--
     *                              /                   /
     * --<N1>-- concat_with --<U>--             ==>  --
     *
     * <N1>, <N2> - NumberSpanParserOperations
     * <U> - UnconditionalModification
     */
    //
    @Test
    fun concatDistributesNumberSpanParserOperation() {
        val actual = listOf(
            ParserStructure<Int>(
                operations = listOf(
                    NumberSpanParserOperation(listOf(ConstantNumberConsumer("12")))
                ),
                followedBy = listOf()
            ),
            ParserStructure(
                operations = listOf(UnconditionalModification { }),
                followedBy = listOf(
                    ParserStructure(
                        operations = listOf(
                            NumberSpanParserOperation(listOf(ConstantNumberConsumer("34")))
                        ),
                        followedBy = listOf()
                    )
                )
            )
        ).concat()

        with(actual) {
            assertTrue(operations.isEmpty())
            with(followedBy) {
                assertEquals(1, size)
                with(this[0]) {
                    assertEquals(2, operations.size)
                    assertTrue(operations[0] is NumberSpanParserOperation)
                    assertEquals(2, (operations[0] as NumberSpanParserOperation).consumers.size)
                    assertTrue(operations[1] is UnconditionalModification)
                }
            }
        }
    }

    @Test
    fun concatDistributesUnconditionalModificationAfterNumberSpanParserOperation() {
        val actual = listOf(
            ParserStructure<Int>(
                operations = listOf(
                    UnconditionalModification { }
                ),
                followedBy = listOf(
                    ParserStructure(
                        operations = listOf(
                            NumberSpanParserOperation(listOf(ConstantNumberConsumer("34")))
                        ),
                        followedBy = listOf()
                    )
                )
            )
        ).concat()

        with(actual) {
            assertTrue(operations.isEmpty())
            with(followedBy) {
                assertEquals(1, size)
                with(followedBy[0]) {
                    assertEquals(2, operations.size)
                    assertTrue(operations[0] is NumberSpanParserOperation)
                    assertTrue(operations[1] is UnconditionalModification)
                    assertTrue(followedBy.isEmpty())
                }
            }
        }
    }

    @Test
    fun concatFlattensOperations() {
        val actual = listOf(
            ParserStructure<Int>(
                operations = listOf(),
                followedBy = listOf(
                    ParserStructure(
                        operations = listOf(),
                        followedBy = listOf(
                            ParserStructure(
                                operations = listOf(),
                                followedBy = listOf(
                                    ParserStructure(
                                        operations = listOf(),
                                        followedBy = listOf(
                                            ParserStructure(
                                                operations = listOf(),
                                                followedBy = listOf(
                                                    ParserStructure(
                                                        operations = listOf(
                                                            NumberSpanParserOperation(listOf(ConstantNumberConsumer("34")))
                                                        ),
                                                        followedBy = listOf()
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ).concat()

        with(actual) {
            assertTrue(operations.isEmpty())
            assertEquals(1, followedBy.size)
            with(followedBy[0]) {
                assertEquals(1, operations.size)
                assertTrue(operations[0] is NumberSpanParserOperation)
                assertTrue(followedBy.isEmpty())
            }
        }
    }

    @Test
    fun concatDistributesTopLevelNumberSpanParserOperationIntoBranches() {
        val actual = listOf(
            ParserStructure<Int>(
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
        ).concat()

        with(actual) {
            assertTrue(operations.isEmpty())
            assertEquals(2, followedBy.size)
            with(followedBy[0]) {
                assertTrue(followedBy.isEmpty())
                with(operations) {
                    assertEquals(1, size)
                    assertTrue(operations[0] is NumberSpanParserOperation)
                    assertEquals(2, (operations[0] as NumberSpanParserOperation).consumers.size)
                }
            }
            with(followedBy[1]) {
                assertTrue(followedBy.isEmpty())
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
