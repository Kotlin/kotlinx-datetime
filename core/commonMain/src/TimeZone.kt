/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

public expect open class TimeZone {
    public val id: String

    companion object {
        /**
         * @throws RuntimeException if the name of the system time-zone is invalid or the time-zone specified as the
         * system one cannot be found.
         */
        fun currentSystemDefault(): TimeZone
        val UTC: TimeZone

        /**
         * @throws IllegalTimeZoneException if [zoneId] has an invalid format or a time-zone with the name [zoneId]
         * could not be found.
         */
        fun of(zoneId: String): TimeZone
        val availableZoneIds: Set<String>
    }

    /**
     * @throws DateTimeArithmeticException if this value is too large to fit in [LocalDateTime].
     */
    public fun Instant.toLocalDateTime(): LocalDateTime

    /** */
    public val Instant.offset: ZoneOffset

    /** */
    public fun LocalDateTime.toInstant(): Instant
}

public expect class ZoneOffset : TimeZone {
    val totalSeconds: Int
}