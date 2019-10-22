/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.time.Duration
import kotlin.time.nanoseconds
import kotlin.time.seconds

@UseExperimental(kotlin.time.ExperimentalTime::class)
public actual class Instant internal constructor(internal val value: java.time.Instant) : Comparable<Instant> {

    public actual fun toUnixMillis(): Long = value.toEpochMilli()


    actual operator fun plus(duration: Duration): Instant = duration.toComponents { seconds, nanoseconds ->
        Instant(value.plusSeconds(seconds).plusNanos(nanoseconds.toLong()))
    }

    actual operator fun minus(duration: Duration): Instant = plus(-duration)

    actual operator fun minus(other: Instant): Duration {
        val diff = java.time.Duration.between(other.value, this.value)
        return diff.seconds.seconds + diff.nano.nanoseconds
    }

    public actual override operator fun compareTo(other: Instant): Int = this.value.compareTo(other.value)

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is Instant && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    public actual companion object {
        actual fun now(): Instant =
                Instant(java.time.Clock.systemUTC().instant())

        actual fun fromUnixMillis(millis: Long): Instant =
                Instant(java.time.Instant.ofEpochMilli(millis))

        actual fun parse(isoString: String): Instant =
                Instant(java.time.Instant.parse(isoString))
    }

}