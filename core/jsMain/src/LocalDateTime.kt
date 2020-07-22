/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

import kotlinx.datetime.internal.JSJoda.LocalDateTime as jtLocalDateTime


public actual class LocalDateTime internal constructor(internal val value: jtLocalDateTime) : Comparable<LocalDateTime> {

    public actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(try {
                jtLocalDateTime.of(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond)
            } catch (e: Throwable) {
                if (e.isJodaDateTimeException()) throw IllegalArgumentException(e)
                throw e
            })

    public actual val year: Int get() = value.year().toInt()
    public actual val monthNumber: Int get() = value.monthValue().toInt()
    public actual val month: Month get() = value.month().toMonth()
    public actual val dayOfMonth: Int get() = value.dayOfMonth().toInt()
    public actual val dayOfWeek: DayOfWeek get() = value.dayOfWeek().toDayOfWeek()
    public actual val dayOfYear: Int get() = value.dayOfYear().toInt()

    public actual val hour: Int get() = value.hour().toInt()
    public actual val minute: Int get() = value.minute().toInt()
    public actual val second: Int get() = value.second().toInt()
    public actual val nanosecond: Int get() = value.nano().toInt()

    public actual val date: LocalDate get() = LocalDate(value.toLocalDate()) // cache?

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is LocalDateTime && this.value == other.value)

    override fun hashCode(): Int = value.hashCode().toInt()

    override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalDateTime): Int = this.value.compareTo(other.value).toInt()

    actual companion object {
        public actual fun parse(isoString: String): LocalDateTime = try {
            jtLocalDateTime.parse(isoString).let(::LocalDateTime)
        } catch (e: Throwable) {
            if (e.isJodaDateTimeParseException()) throw DateTimeFormatException(e)
            throw e
        }

        internal actual val MIN: LocalDateTime = LocalDateTime(jtLocalDateTime.MIN)
        internal actual val MAX: LocalDateTime = LocalDateTime(jtLocalDateTime.MAX)
    }

}


public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime = try {
    jtLocalDateTime.ofInstant(this.value, timeZone.zoneId).let(::LocalDateTime)
} catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e)
    throw e
}

public actual fun Instant.offsetAt(timeZone: TimeZone): ZoneOffset =
        timeZone.zoneId.rules().offsetOfInstant(this.value).let(::ZoneOffset)

public actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
        this.value.atZone(timeZone.zoneId).toInstant().let(::Instant)
