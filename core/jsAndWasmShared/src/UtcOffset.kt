/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.JodaTimeZoneOffset
import kotlinx.datetime.serializers.UtcOffsetSerializer
import kotlinx.serialization.Serializable

@Serializable(with = UtcOffsetSerializer::class)
public actual class UtcOffset internal constructor(internal val zoneOffset: JodaTimeZoneOffset) {
    public actual val totalSeconds: Int get() = zoneOffset.totalSeconds()

    override fun hashCode(): Int = zoneOffset.hashCode()

    override fun equals(other: Any?): Boolean =
        this === other || (other is UtcOffset && (this.zoneOffset === other.zoneOffset || this.zoneOffset.equals(other.zoneOffset)))

    override fun toString(): String = zoneOffset.toString()

    public actual companion object {

        public actual val ZERO: UtcOffset = UtcOffset(JodaTimeZoneOffset.UTC)

        public actual fun parse(offsetString: String): UtcOffset = try {
            JodaTimeZoneOffset.of(offsetString).let(::UtcOffset)
        } catch (e: Throwable) {
            if (e.isJodaDateTimeException()) throw DateTimeFormatException(e)
            throw e
        }
    }
}

@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun UtcOffset(hours: Int? = null, minutes: Int? = null, seconds: Int? = null): UtcOffset =
    try {
        when {
            hours != null ->
                UtcOffset(JodaTimeZoneOffset.ofHoursMinutesSeconds(hours, minutes ?: 0, seconds ?: 0))

            minutes != null ->
                UtcOffset(JodaTimeZoneOffset.ofHoursMinutesSeconds(minutes / 60, minutes % 60, seconds ?: 0))

            else -> {
                UtcOffset(JodaTimeZoneOffset.ofTotalSeconds(seconds ?: 0))
            }
        }
    } catch (e: Throwable) {
        if (e.isJodaDateTimeException()) throw IllegalArgumentException(e) else throw e
    }
