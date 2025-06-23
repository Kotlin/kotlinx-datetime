/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("LocalDateTimeKt")
@file:JvmMultifileClass
package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.removeLeadingZerosFromLongYearFormLocalDateTime
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.format.DateTimeParseException
import java.time.LocalDateTime as jtLocalDateTime

@Serializable(with = LocalDateTimeSerializer::class)
public actual class LocalDateTime internal constructor(
    internal val value: jtLocalDateTime
) : Comparable<LocalDateTime>, java.io.Serializable {

    public actual constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(try {
                jtLocalDateTime.of(year, month, day, hour, minute, second, nanosecond)
            } catch (e: DateTimeException) {
                throw IllegalArgumentException(e)
            })

    public actual constructor(year: Int, month: Month, day: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(year, month.number, day, hour, minute, second, nanosecond)

    public actual constructor(date: LocalDate, time: LocalTime) :
            this(jtLocalDateTime.of(date.value, time.value))

    @Deprecated(
        "Use kotlinx.datetime.Month",
        ReplaceWith("LocalDateTime(year, month.toKotlinMonth(), dayOfMonth, hour, minute, second, nanosecond)")
    )
    public constructor(
        year: Int,
        month: java.time.Month,
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
        nanosecond: Int = 0
    ) : this(
        year,
        month.toKotlinMonth(),
        dayOfMonth,
        hour,
        minute,
        second,
        nanosecond
    )

    public actual val year: Int get() = value.year
    @Deprecated("Use the 'month' property instead", ReplaceWith("this.month.number"), level = DeprecationLevel.WARNING)
    public actual val monthNumber: Int get() = value.monthValue
    public actual val month: Month get() = value.month.toKotlinMonth()
    @PublishedApi internal fun getMonth(): java.time.Month = value.month
    @Deprecated("Use the 'day' property instead", ReplaceWith("this.day"), level = DeprecationLevel.WARNING)
    public actual val dayOfMonth: Int get() = value.dayOfMonth
    public actual val day: Int get() = value.dayOfMonth
    public actual val dayOfWeek: DayOfWeek get() = value.dayOfWeek.toKotlinDayOfWeek()
    @PublishedApi internal fun getDayOfWeek(): java.time.DayOfWeek = value.dayOfWeek
    public actual val dayOfYear: Int get() = value.dayOfYear

    public actual val hour: Int get() = value.hour
    public actual val minute: Int get() = value.minute
    public actual val second: Int get() = value.second
    public actual val nanosecond: Int get() = value.nano

    public actual val date: LocalDate get() = LocalDate(value.toLocalDate()) // cache?

    public actual val time: LocalTime get() = LocalTime(value.toLocalTime())

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is LocalDateTime && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalDateTime): Int = this.value.compareTo(other.value)

    public actual companion object {
        public actual fun parse(input: CharSequence, format: DateTimeFormat<LocalDateTime>): LocalDateTime =
            if (format === Formats.ISO) {
                try {
                    val sanitizedInput = removeLeadingZerosFromLongYearFormLocalDateTime(input.toString())
                    jtLocalDateTime.parse(sanitizedInput).let(::LocalDateTime)
                } catch (e: DateTimeParseException) {
                    throw DateTimeFormatException(e)
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

        // Even though this class uses writeReplace (so serialVersionUID is not needed for a stable serialized form), a
        // stable serialVersionUID is useful for testing, see MaliciousJvmSerializationTest.
        private const val serialVersionUID: Long = 0L
    }

    public actual object Formats {
        public actual val ISO: DateTimeFormat<LocalDateTime> = ISO_DATETIME
    }

    private fun readObject(ois: java.io.ObjectInputStream): Unit = throw java.io.InvalidObjectException(
        "kotlinx.datetime.LocalDateTime must be deserialized via kotlinx.datetime.Ser"
    )

    private fun writeReplace(): Any = Ser(Ser.DATE_TIME_TAG, this)
}

/**
 * @suppress
 */
@Deprecated(
    "Use the constructor that accepts a 'day'",
    ReplaceWith("LocalDateTime(year = year, month = month.toKotlinMonth(), day = dayOfMonth, hour = hour, minute = minute, second = second, nanosecond = nanosecond)"),
    level = DeprecationLevel.WARNING
)
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
public fun LocalDateTime(
    year: Int,
    month: java.time.Month,
    dayOfMonth: Int,
    hour: Int,
    minute: Int,
    second: Int = 0,
    nanosecond: Int = 0,
): LocalDateTime = LocalDateTime(
    year = year, month = month.toKotlinMonth(), day = dayOfMonth,
    hour = hour, minute = minute, second = second, nanosecond = nanosecond
)
