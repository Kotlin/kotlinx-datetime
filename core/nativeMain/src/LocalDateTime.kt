/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

public actual class LocalDateTime : Comparable<LocalDateTime> {
    actual companion object {
        actual fun parse(isoString: String): LocalDateTime {
            TODO("Not yet implemented")
        }
    }

    actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) {
        TODO("Not yet implemented")
    }

    actual val year: Int
        get() = TODO("Not yet implemented")
    actual val monthNumber: Int
        get() = TODO("Not yet implemented")
    actual val month: Month
        get() = TODO("Not yet implemented")
    actual val dayOfMonth: Int
        get() = TODO("Not yet implemented")
    actual val dayOfWeek: DayOfWeek
        get() = TODO("Not yet implemented")
    actual val dayOfYear: Int
        get() = TODO("Not yet implemented")
    actual val hour: Int
        get() = TODO("Not yet implemented")
    actual val minute: Int
        get() = TODO("Not yet implemented")
    actual val second: Int
        get() = TODO("Not yet implemented")
    actual val nanosecond: Int
        get() = TODO("Not yet implemented")
    actual val date: LocalDate
        get() = TODO("Not yet implemented")

    actual override fun compareTo(other: LocalDateTime): Int {
        TODO("Not yet implemented")
    }

}

actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime {
    TODO("Not yet implemented")
}

actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant {
    TODO("Not yet implemented")
}

actual fun Instant.offsetAt(timeZone: TimeZone): ZoneOffset {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

actual fun LocalDate.periodUntil(other: LocalDate): CalendarPeriod {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}
