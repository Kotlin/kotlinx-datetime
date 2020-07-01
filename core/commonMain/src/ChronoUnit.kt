/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime


enum class TimeComponent {
    MONTH,
    DAY,
    NANOSECOND
}

class ChronoUnit(val scale: Long, val component: TimeComponent) {
    init {
        require(scale > 0) { "Unit scale must be positive, but was $scale" }
    }
    constructor(number: Long, unit: ChronoUnit) : this(number * unit.scale, unit.component)
    // it seems possible to provide 'times' operation
    companion object {
        val NANOSECOND = ChronoUnit(1, TimeComponent.NANOSECOND)
        val MICROSECOND = ChronoUnit(1000, NANOSECOND)
        val MILLISECOND = ChronoUnit(1000, MICROSECOND)
        val SECOND = ChronoUnit(1000, MILLISECOND)
        val MINUTE = ChronoUnit(60, SECOND)
        val HOUR = ChronoUnit(60, MINUTE)
        val DAY = ChronoUnit(1, TimeComponent.DAY)
        val WEEK = ChronoUnit(7, DAY)
        val MONTH = ChronoUnit(1, TimeComponent.MONTH)
        val QUARTER = ChronoUnit(3, MONTH)
        val YEAR = ChronoUnit(12, MONTH)
        val CENTURY = ChronoUnit(100, YEAR)
    }
}

internal fun TimeComponent.toCalendarUnit(): CalendarUnit = when(this) {
    TimeComponent.MONTH -> CalendarUnit.MONTH
    TimeComponent.DAY -> CalendarUnit.DAY
    TimeComponent.NANOSECOND -> CalendarUnit.NANOSECOND
}
