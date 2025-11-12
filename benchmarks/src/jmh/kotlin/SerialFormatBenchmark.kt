/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlinx.datetime

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
open class SerialFormatBenchmark {

    @Param("1", "2", "4", "8", "16", "32", "64", "128", "256", "512", "1024")
    var n = 0

    @Benchmark
    fun largeSerialFormat(blackhole: Blackhole) {
        val format = LocalDateTime.Format {
            repeat(n) {
                char('^')
                monthNumber()
                char('&')
                day()
                char('!')
                hour()
                char('$')
                minute()
                char('#')
                second()
                char('@')
            }
        }
        blackhole.consume(format)
    }
}
