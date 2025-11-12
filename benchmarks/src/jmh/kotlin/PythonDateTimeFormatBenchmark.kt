/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlinx.datetime

import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.*

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class PythonDateTimeFormatBenchmark {

    @Benchmark
    fun buildPythonDateTimeFormat(blackhole: Blackhole) {
        val v = LocalDateTime.Format {
            year()
            char('-')
            monthNumber()
            char('-')
            day()
            char(' ')
            hour()
            char(':')
            minute()
            optional {
                char(':')
                second()
                optional {
                    char('.')
                    secondFraction()
                }
            }
        }
        blackhole.consume(v)
    }
}
