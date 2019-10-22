/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.time.Duration


@UseExperimental(kotlin.time.ExperimentalTime::class)
public expect class Instant : Comparable<Instant> {

    // TODO: primary value properties

    public fun toUnixMillis(): Long

    public operator fun plus(duration: Duration): Instant
    public operator fun minus(duration: Duration): Instant

    // questionable
    public operator fun minus(other: Instant): Duration

    public override operator fun compareTo(other: Instant): Int

    companion object {
        fun now(): Instant
        fun fromUnixMillis(millis: Long): Instant
        fun parse(isoString: String): Instant
    }
}

public fun String.toInstant(): Instant = Instant.parse(this)