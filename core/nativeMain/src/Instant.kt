/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.time.*
import kotlin.test.*

public actual enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;
}

@UseExperimental(kotlin.time.ExperimentalTime::class)
public actual data class Instant internal constructor (private val epochSeconds: Long, private val nanos: Int) : Comparable<Instant> {

    actual fun toUnixMillis(): Long = epochSeconds * MILLIS_PER_ONE + nanos / NANOS_PER_MILLI

    private fun plus(secondsToAdd: Long, nanosToAdd: Int): Instant {
        var nanosToAdd = nanosToAdd.toLong()
        if ((secondsToAdd or nanosToAdd) == 0L) {
            return this
        }
        var epochSec: Long = safeAdd(epochSeconds, secondsToAdd)
        epochSec = safeAdd(epochSec, nanosToAdd / NANOS_PER_ONE)
        nanosToAdd = nanosToAdd % NANOS_PER_ONE
        val nanoAdjustment: Long = nanos + nanosToAdd

        return Instant(epochSec, nanoAdjustment.toInt())
    }

    actual operator fun plus(duration: Duration): Instant = duration.toComponents {epochSeconds, nanos ->
        plus(epochSeconds, nanos)
    }

    actual operator fun minus(duration: Duration): Instant = plus(-duration)

    actual operator fun minus(other: Instant): Duration =
        (this.epochSeconds - other.epochSeconds).seconds + // won't overflow given the instant bounds
        (this.nanos - other.nanos).nanoseconds

    actual override fun compareTo(other: Instant): Int =
        compareBy<Instant>({ it.epochSeconds }, { it.nanos }).compare(this, other)

    actual companion object {
        actual fun now(): Instant {
            return memScoped {
                val timespecBuf = alloc<timespec>()
                val error = clock_gettime(CLOCK_REALTIME, timespecBuf.ptr)
                assertEquals(0, error)
                Instant(timespecBuf.tv_sec, timespecBuf.tv_nsec.toInt())
            }
        }

        actual fun fromUnixMillis(millis: Long): Instant =
            Instant(floorDiv(millis, MILLIS_PER_ONE.toLong()),
                (floorMod(millis, MILLIS_PER_ONE.toLong()) * NANOS_PER_MILLI).toInt())

        actual fun parse(isoString: String): Instant {
            TODO("Not yet implemented")
        }
    }

}

actual fun Instant.plus(period: CalendarPeriod, zone: TimeZone): Instant {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

actual fun Instant.plus(value: Int, unit: CalendarUnit, zone: TimeZone): Instant =
    plus(value.toLong(), unit, zone)

actual fun Instant.plus(value: Long, unit: CalendarUnit, zone: TimeZone): Instant {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

actual fun Instant.periodUntil(other: Instant, zone: TimeZone): CalendarPeriod {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

actual fun Instant.until(other: Instant, unit: CalendarUnit, zone: TimeZone): Long {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

actual fun Instant.daysUntil(other: Instant, zone: TimeZone): Int {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

actual fun Instant.monthsUntil(other: Instant, zone: TimeZone): Int {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

actual fun Instant.yearsUntil(other: Instant, zone: TimeZone): Int {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}
