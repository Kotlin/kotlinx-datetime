/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
@file:JvmMultifileClass
@file:JvmName("ClockKt")
package kotlinx.datetime

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Creates a [kotlin.time.Clock] (the standard library version of `Clock`) delegating to `this`.
 */
public fun Clock.toStdlibClock(): kotlin.time.Clock =
    (this as? DateTimeClock)?.clock ?: StdlibClock(this)

/**
 * Creates a [kotlinx.datetime.Clock] delegating to the version of `Clock` from the standard library.
 */
public fun kotlin.time.Clock.toDeprecatedClock(): Clock =
    (this as? StdlibClock)?.clock ?: DateTimeClock(this)

private class DateTimeClock(val clock: kotlin.time.Clock): kotlinx.datetime.Clock {
    override fun now(): Instant = clock.now().toDeprecatedInstant()
}

private class StdlibClock(val clock: Clock): kotlin.time.Clock {
    override fun now(): kotlin.time.Instant = clock.now().toStdlibInstant()
}

/**
 * The `kotlinx-datetime` version of [kotlin.time.Clock].
 *
 * Using this interface is discouraged in favor of the standard library version, [kotlin.time.Clock].
 * Initially, `Instant` and `Clock` were introduced in `kotlinx-datetime`,
 * but it turned out they were useful even in contexts where no datetime processing was needed.
 * As a result, starting from 2.1.20, Kotlin's standard library includes its own `Instant` and `Clock` classes.
 *
 * `kotlinx.datetime.Clock` is still available for compatibility reasons in the compatibility artifact of
 * `kotlinx-datetime`, but it is deprecated.
 * In the normal release artifact, it is replaced by `kotlin.time.Clock`.
 * See [https://github.com/Kotlin/kotlinx-datetime?tab=readme-ov-file#deprecation-of-instant] for more information.
 */
@Deprecated(
    "Use kotlin.time.Clock instead",
    ReplaceWith("kotlin.time.Clock", "kotlin.time.Clock"),
    level = DeprecationLevel.WARNING
)
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
        override fun now(): Instant = kotlin.time.Clock.System.now().toDeprecatedInstant()
    }

    /** A companion object used purely for namespacing. */
    public companion object {

    }
}

/**
 * Returns the current date at the given [time zone][timeZone], according to [this Clock][this].
 *
 * The time zone is important because the current date is not the same in all time zones at the same instant.
 *
 * @sample kotlinx.datetime.test.samples.ClockSamples.todayIn
 */
@Deprecated("kotlin.datetime.Clock is superseded by kotlin.time.Clock",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibClock().todayIn(timeZone)")
)
public fun Clock.todayIn(timeZone: TimeZone): LocalDate =
    toStdlibClock().todayIn(timeZone)

/**
 * Returns a [TimeSource] that uses this [Clock] to mark a time instant and to find the amount of time elapsed since that mark.
 *
 * **Pitfall**: using this function with [Clock.System] is error-prone
 * because [Clock.System] is not well suited for measuring time intervals.
 * Please only use this conversion function on the [Clock] instances that are fully controlled programmatically.
 */
@ExperimentalTime
@Deprecated("kotlin.datetime.Clock is superseded by kotlin.time.Clock",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibClock().asTimeSource()")
)
public fun Clock.asTimeSource(): TimeSource.WithComparableMarks = toStdlibClock().asTimeSource()

@Deprecated("Use Clock.todayIn instead", ReplaceWith("this.toStdlibClock().todayIn(timeZone)"), DeprecationLevel.WARNING)
public fun Clock.todayAt(timeZone: TimeZone): LocalDate = todayIn(timeZone)

@Deprecated(
    "kotlin.datetime.Clock is superseded by kotlin.time.Clock",
    ReplaceWith("this.asClock(origin.toStdlibInstant()).toDeprecatedClock()"),
    level = DeprecationLevel.WARNING
)
public fun TimeSource.asClock(origin: kotlinx.datetime.Instant): kotlinx.datetime.Clock = object : Clock {
    private val startMark: TimeMark = markNow()
    override fun now() = origin + startMark.elapsedNow()
}