/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.time

import kotlin.time.*

/**
 * A source of [Instant] values.
 *
 * See [Clock.System][Clock.System] for the clock instance that queries the operating system.
 *
 * It is not recommended to use [Clock.System] directly in the implementation. Instead, you can pass a
 * [Clock] explicitly to the necessary functions or classes.
 * This way, tests can be written deterministically by providing custom [Clock] implementations
 * to the system under test.
 */
public interface Clock {
    /**
     * Returns the [Instant] corresponding to the current time, according to this clock.
     *
     * Calling [now] later is not guaranteed to return a larger [Instant].
     * In particular, for [Clock.System], the opposite is completely expected,
     * and it must be taken into account.
     * See the [System] documentation for details.
     *
     * Even though [Instant] is defined to be on the UTC-SLS time scale, which enforces a specific way of handling
     * leap seconds, [now] is not guaranteed to handle leap seconds in any specific way.
     */
    public fun now(): Instant

    /**
     * The [Clock] instance that queries the platform-specific system clock as its source of time knowledge.
     *
     * Successive calls to [now] will not necessarily return increasing [Instant] values, and when they do,
     * these increases will not necessarily correspond to the elapsed time.
     *
     * For example, when using [Clock.System], the following could happen:
     * - [now] returns `2023-01-02T22:35:01Z`.
     * - The system queries the Internet and recognizes that its clock needs adjusting.
     * - [now] returns `2023-01-02T22:32:05Z`.
     *
     * When you need predictable intervals between successive measurements, consider using [TimeSource.Monotonic].
     *
     * For improved testability, you should avoid using [Clock.System] directly in the implementation
     * and pass a [Clock] explicitly instead. For example:
     *
     * @sample kotlinx.datetime.test.samples.ClockSamples.system
     * @sample kotlinx.datetime.test.samples.ClockSamples.dependencyInjection
     */
    public object System : Clock {
        override fun now(): Instant = @Suppress("DEPRECATION_ERROR") Instant.now()
    }

    /** A companion object used purely for namespacing. */
    public companion object {

    }
}
