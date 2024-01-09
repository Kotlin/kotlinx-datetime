/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.UtcOffsetSerializer
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.native.concurrent.ThreadLocal

@Serializable(with = UtcOffsetSerializer::class)
public actual class UtcOffset private constructor(public actual val totalSeconds: Int) {
    private val id: String = zoneIdByOffset(totalSeconds)

    override fun hashCode(): Int = totalSeconds
    override fun equals(other: Any?): Boolean = other is UtcOffset && this.totalSeconds == other.totalSeconds
    override fun toString(): String = id

    public actual companion object {

        public actual val ZERO: UtcOffset = UtcOffset(totalSeconds = 0)

        public actual fun parse(offsetString: String): UtcOffset {
            if (offsetString == "Z") {
                return ZERO
            }

            // parse - +h, +hh, +hhmm, +hh:mm, +hhmmss, +hh:mm:ss
            val hours: Int
            val minutes: Int
            val seconds: Int
            when (offsetString.length) {
                2 -> return parse(offsetString[0].toString() + "0" + offsetString[1])
                3 -> {
                    hours = parseNumber(offsetString, 1, false)
                    minutes = 0
                    seconds = 0
                }
                5 -> {
                    hours = parseNumber(offsetString, 1, false)
                    minutes = parseNumber(offsetString, 3, false)
                    seconds = 0
                }
                6 -> {
                    hours = parseNumber(offsetString, 1, false)
                    minutes = parseNumber(offsetString, 4, true)
                    seconds = 0
                }
                7 -> {
                    hours = parseNumber(offsetString, 1, false)
                    minutes = parseNumber(offsetString, 3, false)
                    seconds = parseNumber(offsetString, 5, false)
                }
                9 -> {
                    hours = parseNumber(offsetString, 1, false)
                    minutes = parseNumber(offsetString, 4, true)
                    seconds = parseNumber(offsetString, 7, true)
                }
                else -> throw DateTimeFormatException("Invalid ID for UtcOffset, invalid format: $offsetString")
            }
            val first: Char = offsetString[0]
            if (first != '+' && first != '-') {
                throw DateTimeFormatException(
                    "Invalid ID for UtcOffset, plus/minus not found when expected: $offsetString")
            }
            try {
                return if (first == '-') {
                    ofHoursMinutesSeconds(-hours, -minutes, -seconds)
                } else {
                    ofHoursMinutesSeconds(hours, minutes, seconds)
                }
            } catch (e: IllegalArgumentException) {
                throw DateTimeFormatException(e)
            }
        }

        private fun validateTotal(totalSeconds: Int) {
            if (totalSeconds !in -18 * SECONDS_PER_HOUR .. 18 * SECONDS_PER_HOUR) {
                throw IllegalArgumentException("Total seconds value is out of range: $totalSeconds")
            }
        }

        // org.threeten.bp.ZoneOffset#validate
        private fun validate(hours: Int, minutes: Int, seconds: Int) {
            if (hours < -18 || hours > 18) {
                throw IllegalArgumentException("Zone offset hours not in valid range: value " + hours +
                        " is not in the range -18 to 18")
            }
            if (hours > 0) {
                if (minutes < 0 || seconds < 0) {
                    throw IllegalArgumentException("Zone offset minutes and seconds must be positive because hours is positive")
                }
            } else if (hours < 0) {
                if (minutes > 0 || seconds > 0) {
                    throw IllegalArgumentException("Zone offset minutes and seconds must be negative because hours is negative")
                }
            } else if (minutes > 0 && seconds < 0 || minutes < 0 && seconds > 0) {
                throw IllegalArgumentException("Zone offset minutes and seconds must have the same sign")
            }
            if (abs(minutes) > 59) {
                throw IllegalArgumentException("Zone offset minutes not in valid range: abs(value) " +
                        abs(minutes) + " is not in the range 0 to 59")
            }
            if (abs(seconds) > 59) {
                throw IllegalArgumentException("Zone offset seconds not in valid range: abs(value) " +
                        abs(seconds) + " is not in the range 0 to 59")
            }
            if (abs(hours) == 18 && (abs(minutes) > 0 || abs(seconds) > 0)) {
                throw IllegalArgumentException("Utc offset not in valid range: -18:00 to +18:00")
            }
        }

        // org.threeten.bp.ZoneOffset#ofHoursMinutesSeconds
        internal fun ofHoursMinutesSeconds(hours: Int, minutes: Int, seconds: Int): UtcOffset {
            validate(hours, minutes, seconds)
            return if (hours == 0 && minutes == 0 && seconds == 0) ZERO
            else ofSeconds(hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds)
        }

        // org.threeten.bp.ZoneOffset#ofTotalSeconds
        internal fun ofSeconds(seconds: Int): UtcOffset {
            validateTotal(seconds)
            return if (seconds % (15 * SECONDS_PER_MINUTE) == 0) {
                utcOffsetCache[seconds] ?: UtcOffset(totalSeconds = seconds).also { utcOffsetCache[seconds] = it }
            } else {
                UtcOffset(totalSeconds = seconds)
            }
        }

        // org.threeten.bp.ZoneOffset#parseNumber
        private fun parseNumber(offsetId: CharSequence, pos: Int, precededByColon: Boolean): Int {
            if (precededByColon && offsetId[pos - 1] != ':') {
                throw DateTimeFormatException("Invalid ID for UtcOffset, colon not found when expected: $offsetId")
            }
            val ch1 = offsetId[pos]
            val ch2 = offsetId[pos + 1]
            if (ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9') {
                throw DateTimeFormatException("Invalid ID for UtcOffset, non numeric characters found: $offsetId")
            }
            return (ch1 - '0') * 10 + (ch2 - '0')
        }
    }
}

@ThreadLocal
private var utcOffsetCache: MutableMap<Int, UtcOffset> = mutableMapOf()

@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun UtcOffset(hours: Int? = null, minutes: Int? = null, seconds: Int? = null): UtcOffset =
    when {
        hours != null ->
            UtcOffset.ofHoursMinutesSeconds(hours, minutes ?: 0, seconds ?: 0)
        minutes != null ->
            UtcOffset.ofHoursMinutesSeconds(minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR, seconds ?: 0)
        else -> {
            UtcOffset.ofSeconds(seconds ?: 0)
        }
    }

