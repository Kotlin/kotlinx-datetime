/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.time.Duration
import kotlin.time.nanoseconds
import kotlin.time.seconds
import kotlinx.datetime.internal.JSJoda.Instant as jtInstant
import kotlinx.datetime.internal.JSJoda.Duration as jtDuration
import kotlinx.datetime.internal.JSJoda.Clock as jtClock

@UseExperimental(kotlin.time.ExperimentalTime::class)
public actual class Instant internal constructor(internal val value: jtInstant) : Comparable<Instant> {

    public actual fun toUnixMillis(): Long = value.toEpochMilli().toLong()


    actual operator fun plus(duration: Duration): Instant = duration.toComponents { seconds, nanoseconds ->
        Instant(value.plusSeconds(seconds).plusNanos(nanoseconds.toLong()))
    }

    actual operator fun minus(duration: Duration): Instant = plus(-duration)

    actual operator fun minus(other: Instant): Duration {
        val diff = jtDuration.between(other.value, this.value)
        return diff.seconds().toDouble().seconds + diff.nano().toDouble().nanoseconds
    }

    public actual override operator fun compareTo(other: Instant): Int = this.value.compareTo(other.value).toInt()

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is Instant && this.value == other.value)

    override fun hashCode(): Int = value.hashCode().toInt()

    override fun toString(): String = value.toString()

    public actual companion object {
        actual fun now(): Instant =
                Instant(jtClock.systemUTC().instant())

        actual fun fromUnixMillis(millis: Long): Instant =
                Instant(jtInstant.ofEpochMilli(millis.toDouble()))

        actual fun parse(isoString: String): Instant =
                Instant(jtInstant.parse(isoString))
    }

}