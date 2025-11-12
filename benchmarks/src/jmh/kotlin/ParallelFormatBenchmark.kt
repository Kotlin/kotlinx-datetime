/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlinx.datetime

import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class ParallelFormatBenchmark {

    @Param("2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
    var n = 0

    @Benchmark
    fun formatCreationWithAlternativeParsing(blackhole: Blackhole) {
        val format = LocalDateTime.Format {
            repeat(n) {
                alternativeParsing(
                    { monthNumber() },
                    { day() },
                    primaryFormat = { hour() }
                )
                char('@')
                minute()
                char('#')
                second()
            }
        }
        blackhole.consume(format)
    }

    @Benchmark
    fun formatCreationWithNestedAlternativeParsing(blackhole: Blackhole) {
        val format = LocalDateTime.Format {
            repeat(n) { index ->
                alternativeParsing(
                    { monthNumber(); char('-'); day() },
                    { day(); char('/'); monthNumber() },
                    primaryFormat = { year(); char('-'); monthNumber(); char('-'); day() }
                )

                if (index and 1 == 0) {
                    alternativeParsing(
                        {
                            alternativeParsing(
                                { hour(); char(':'); minute() },
                                { minute(); char(':'); second() },
                                primaryFormat = { hour(); char(':'); minute(); char(':'); second() }
                            )
                        },
                        primaryFormat = {
                            year(); char('-'); monthNumber(); char('-'); day()
                            char('T')
                            hour(); char(':'); minute(); char(':'); second()
                        }
                    )
                }

                char('|')
                if (index % 3 == 0) {
                    char('|')
                }

                if (index and 2 == 0) {
                    alternativeParsing(
                        { char('Z') },
                        { char('+'); hour(); char(':'); minute() },
                        primaryFormat = { char('-'); hour(); char(':'); minute() }
                    )
                }
            }
        }
        blackhole.consume(format)
    }
}
