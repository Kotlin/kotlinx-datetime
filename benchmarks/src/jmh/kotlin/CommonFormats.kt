/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlinx.datetime

import kotlinx.datetime.format.*
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.*

@Warmup(iterations = 20, time = 2)
@Measurement(iterations = 30, time = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(2)
open class CommonFormats {

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

    @Benchmark
    fun buildIsoDateTimeFormat(blackhole: Blackhole) {
        val format = LocalDateTime.Format {
            date(LocalDate.Format {
                year()
                char('-')
                monthNumber()
                char('-')
                day()
            })
            alternativeParsing({ char('t') }) { char('T') }
            time(LocalTime.Format {
                hour()
                char(':')
                minute()
                alternativeParsing({}) {
                    char(':')
                    second()
                    optional {
                        char('.')
                        secondFraction(1, 9)
                    }
                }
            })
        }
        blackhole.consume(format)
    }

    @Benchmark
    fun buildFourDigitsUtcOffsetFormat(blackhole: Blackhole) {
        val format = UtcOffset.Format {
            offsetHours()
            offsetMinutesOfHour()
        }
        blackhole.consume(format)
    }

    @Benchmark
    fun buildRfc1123DateTimeFormat(blackhole: Blackhole) {
        val format = DateTimeComponents.Format {
            alternativeParsing({
                // the day of week may be missing
            }) {
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                chars(", ")
            }
            day(Padding.NONE)
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            year()
            char(' ')
            hour()
            char(':')
            minute()
            optional {
                char(':')
                second()
            }
            chars(" ")
            alternativeParsing({
                chars("UT")
            }, {
                chars("Z")
            }) {
                optional("GMT") {
                    offset(UtcOffset.Format {
                        offsetHours()
                        offsetMinutesOfHour()
                    })
                }
            }
        }
        blackhole.consume(format)
    }

    @Benchmark
    fun buildIsoDateTimeOffsetFormat(blackhole: Blackhole) {
        val format = DateTimeComponents.Format {
            date(LocalDate.Format {
                year()
                char('-')
                monthNumber()
                char('-')
                day()
            })
            alternativeParsing({
                char('t')
            }) {
                char('T')
            }
            hour()
            char(':')
            minute()
            char(':')
            second()
            optional {
                char('.')
                secondFraction(1, 9)
            }
            alternativeParsing({
                offsetHours()
            }) {
                offset(UtcOffset.Format {
                    alternativeParsing({ chars("z") }) {
                        optional("Z") {
                            offsetHours()
                            char(':')
                            offsetMinutesOfHour()
                            optional {
                                char(':')
                                offsetSecondsOfMinute()
                            }
                        }
                    }
                })
            }
        }
        blackhole.consume(format)
    }
}
