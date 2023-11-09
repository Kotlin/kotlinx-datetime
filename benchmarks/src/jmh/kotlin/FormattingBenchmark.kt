/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class FormattingBenchmark {

    private val localDateTime = LocalDateTime(2023, 11, 9, 12, 21, 31, 41)
    private val formatted = LocalDateTime.Formats.ISO.format(localDateTime)

    @Benchmark
    fun formatIso() = LocalDateTime.Formats.ISO.format(localDateTime)

    @Benchmark
    fun parseIso() = LocalDateTime.Formats.ISO.parse(formatted)
}
