/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("LocalDateTimeJvmKt")
package kotlinx.datetime


public actual typealias Month = java.time.Month
public actual typealias DayOfWeek = java.time.DayOfWeek

public actual class LocalDateTime internal constructor(internal val value: java.time.LocalDateTime) : Comparable<LocalDateTime> {

    public actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
        this(java.time.LocalDateTime.of(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond))

    public actual val year: Int get() = value.year
    public actual val monthNumber: Int get() = value.monthValue
    public actual val month: Month get() = value.month
    public actual val dayOfMonth: Int get() = value.dayOfMonth
    public actual val dayOfWeek: DayOfWeek get() = value.dayOfWeek
    public actual val dayOfYear: Int get() = value.dayOfYear

    public actual val hour: Int get() = value.hour
    public actual val minute: Int get() = value.minute
    public actual val second: Int get() = value.second
    public actual val nanosecond: Int get() = value.nano



    override fun equals(other: Any?): Boolean =
            (this === other) || (other is LocalDateTime && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalDateTime): Int = this.value.compareTo(other.value)

    actual companion object {
        public actual fun parse(isoString: String): LocalDateTime {
            return java.time.LocalDateTime.parse(isoString).let(::LocalDateTime)
        }
    }

}


public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
        java.time.LocalDateTime.ofInstant(this.value, timeZone.zoneId).let(::LocalDateTime)

public actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
        this.value.atZone(timeZone.zoneId).toInstant().let(::Instant)

