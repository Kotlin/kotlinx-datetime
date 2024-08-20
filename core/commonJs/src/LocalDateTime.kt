/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.format.ISO_DATETIME
import kotlinx.datetime.format.LocalDateTimeFormat
import kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer
import kotlinx.serialization.Serializable
import kotlinx.datetime.internal.JSJoda.LocalDateTime as jtLocalDateTime

@Serializable(with = LocalDateTimeIso8601Serializer::class)
public actual class LocalDateTime internal constructor(internal val value: jtLocalDateTime) : Comparable<LocalDateTime> {

    public actual constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(try {
                jsTry { jtLocalDateTime.of(year, month, day, hour, minute, second, nanosecond) }
            } catch (e: Throwable) {
                if (e.isJodaDateTimeException()) throw IllegalArgumentException(e)
                throw e
            })

    public actual constructor(year: Int, month: Month, day: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(year, month.number, day, hour, minute, second, nanosecond)

    public actual constructor(date: LocalDate, time: LocalTime) :
            this(jsTry { jtLocalDateTime.of(date.value, time.value) })

    public actual val year: Int get() = value.year()
    @Deprecated("Use the 'month' property instead", ReplaceWith("this.month.number"), level = DeprecationLevel.WARNING)
    public actual val monthNumber: Int get() = value.monthValue()
    public actual val month: Month get() = value.month().toMonth()
    @Deprecated("Use the 'day' property instead", level = DeprecationLevel.WARNING)
    public actual val dayOfMonth: Int get() = value.dayOfMonth()
    public actual val day: Int get() = value.dayOfMonth()
    public actual val dayOfWeek: DayOfWeek get() = value.dayOfWeek().toDayOfWeek()
    public actual val dayOfYear: Int get() = value.dayOfYear()

    public actual val hour: Int get() = value.hour()
    public actual val minute: Int get() = value.minute()
    public actual val second: Int get() = value.second()
    public actual val nanosecond: Int get() = value.nano().toInt()

    public actual val date: LocalDate get() = LocalDate(value.toLocalDate()) // cache?

    public actual val time: LocalTime get() = LocalTime(value.toLocalTime())

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is LocalDateTime && (this.value === other.value || this.value.equals(other.value)))

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalDateTime): Int = this.value.compareTo(other.value)

    public actual companion object {
        public actual fun parse(input: CharSequence, format: DateTimeFormat<LocalDateTime>): LocalDateTime =
            if (format === Formats.ISO) {
                try {
                    jsTry { jtLocalDateTime.parse(input.toString()) }.let(::LocalDateTime)
                } catch (e: Throwable) {
                    if (e.isJodaDateTimeParseException()) throw DateTimeFormatException(e)
                    throw e
                }
            } else {
                format.parse(input)
            }

        @Deprecated("This overload is only kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        public fun parse(isoString: String): LocalDateTime = parse(input = isoString)

        internal actual val MIN: LocalDateTime = LocalDateTime(jtLocalDateTime.MIN)
        internal actual val MAX: LocalDateTime = LocalDateTime(jtLocalDateTime.MAX)

        @Suppress("FunctionName")
        public actual fun Format(builder: DateTimeFormatBuilder.WithDateTime.() -> Unit): DateTimeFormat<LocalDateTime> =
            LocalDateTimeFormat.build(builder)
    }

    public actual object Formats {
        public actual val ISO: DateTimeFormat<LocalDateTime> = ISO_DATETIME
    }

}
