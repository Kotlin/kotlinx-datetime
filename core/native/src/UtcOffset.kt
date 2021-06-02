/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.math.abs
import kotlin.native.concurrent.ThreadLocal

public actual class UtcOffset internal constructor(public actual val totalSeconds: Int, internal val id: String) {

    override fun hashCode(): Int = totalSeconds
    override fun equals(other: Any?): Boolean = other is UtcOffset && this.totalSeconds == other.totalSeconds
    override fun toString(): String = id

    public actual companion object {

        internal val ZERO: UtcOffset = UtcOffset(0, "Z")

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
                else -> throw IllegalTimeZoneException("Invalid ID for UtcOffset, invalid format: $offsetString")
            }
            val first: Char = offsetString[0]
            if (first != '+' && first != '-') {
                throw IllegalTimeZoneException(
                    "Invalid ID for UtcOffset, plus/minus not found when expected: $offsetString"
                )
            }
            return if (first == '-') {
                ofHoursMinutesSeconds(-hours, -minutes, -seconds)
            } else {
                ofHoursMinutesSeconds(hours, minutes, seconds)
            }
        }

        // org.threeten.bp.ZoneOffset#validate
        private fun validate(hours: Int, minutes: Int, seconds: Int) {
            if (hours < -18 || hours > 18) {
                throw IllegalTimeZoneException(
                    "Zone offset hours not in valid range: value " + hours +
                            " is not in the range -18 to 18"
                )
            }
            if (hours > 0) {
                if (minutes < 0 || seconds < 0) {
                    throw IllegalTimeZoneException("Zone offset minutes and seconds must be positive because hours is positive")
                }
            } else if (hours < 0) {
                if (minutes > 0 || seconds > 0) {
                    throw IllegalTimeZoneException("Zone offset minutes and seconds must be negative because hours is negative")
                }
            } else if (minutes > 0 && seconds < 0 || minutes < 0 && seconds > 0) {
                throw IllegalTimeZoneException("Zone offset minutes and seconds must have the same sign")
            }
            if (abs(minutes) > 59) {
                throw IllegalTimeZoneException(
                    "Zone offset minutes not in valid range: abs(value) " +
                            abs(minutes) + " is not in the range 0 to 59"
                )
            }
            if (abs(seconds) > 59) {
                throw IllegalTimeZoneException(
                    "Zone offset seconds not in valid range: abs(value) " +
                            abs(seconds) + " is not in the range 0 to 59"
                )
            }
            if (abs(hours) == 18 && (abs(minutes) > 0 || abs(seconds) > 0)) {
                throw IllegalTimeZoneException("Zone offset not in valid range: -18:00 to +18:00")
            }
        }

        // org.threeten.bp.ZoneOffset#ofHoursMinutesSeconds
        internal fun ofHoursMinutesSeconds(hours: Int, minutes: Int, seconds: Int): UtcOffset {
            validate(hours, minutes, seconds)
            return if (hours == 0 && minutes == 0 && seconds == 0) ZERO
            else ofSeconds(hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds)
        }

        // org.threeten.bp.ZoneOffset#ofTotalSeconds
        internal fun ofSeconds(seconds: Int): UtcOffset =
            if (seconds % (15 * SECONDS_PER_MINUTE) == 0) {
                utcOffsetCache[seconds] ?: UtcOffset(seconds, zoneIdByOffset(seconds)).also { utcOffsetCache[seconds] = it }
            } else {
                UtcOffset(seconds, zoneIdByOffset(seconds))
            }

        // org.threeten.bp.ZoneOffset#parseNumber
        private fun parseNumber(offsetId: CharSequence, pos: Int, precededByColon: Boolean): Int {
            if (precededByColon && offsetId[pos - 1] != ':') {
                throw IllegalTimeZoneException("Invalid ID for ZoneOffset, colon not found when expected: $offsetId")
            }
            val ch1 = offsetId[pos]
            val ch2 = offsetId[pos + 1]
            if (ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9') {
                throw IllegalTimeZoneException("Invalid ID for ZoneOffset, non numeric characters found: $offsetId")
            }
            return (ch1 - '0') * 10 + (ch2 - '0')
        }
    }
}

@ThreadLocal
private var utcOffsetCache: MutableMap<Int, UtcOffset> = mutableMapOf()
