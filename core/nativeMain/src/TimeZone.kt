/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

public actual open class TimeZone(actual val id: String) {

    actual companion object {
        actual val SYSTEM: TimeZone
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        actual val UTC: TimeZone = ZoneOffset(0)

        actual fun of(zoneId: String): TimeZone = TimeZone(zoneId)

        actual val availableZoneIds: Set<String>
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    }

    actual fun Instant.toLocalDateTime(): LocalDateTime {
        val localSecond: Long = epochSeconds + offset.totalSeconds // overflow caught later
        val localEpochDay: Long = floorDiv(localSecond, SECONDS_PER_DAY.toLong())
        val secsOfDay: Long = floorMod(localSecond, SECONDS_PER_DAY.toLong())
        val date: LocalDate = LocalDate.ofEpochDay(localEpochDay)
        val time: LocalTime = LocalTime.ofSecondOfDay(secsOfDay, nanos)
        return LocalDateTime(date, time)
    }

    actual val Instant.offset: ZoneOffset
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    actual fun LocalDateTime.toInstant(): Instant {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is TimeZone && this.id == other.id)

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id

}

public actual class ZoneOffset(actual val totalSeconds: Int) : TimeZone(zoneIdByOffset(totalSeconds))