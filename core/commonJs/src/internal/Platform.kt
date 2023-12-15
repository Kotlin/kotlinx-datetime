/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.internal.JSJoda.ZoneId
import kotlin.time.Instant

private val tzdb: Result<TimeZoneDatabase?> = runCatching {
    /**
     * References:
     * - <https://momentjs.com/timezone/docs/#/data-formats/packed-format/>
     * - https://github.com/js-joda/js-joda/blob/8c1a7448db92ca014417346049fb64b55f7b1ac1/packages/timezone/src/MomentZoneRulesProvider.js#L78-L94
     * - https://github.com/js-joda/js-joda/blob/8c1a7448db92ca014417346049fb64b55f7b1ac1/packages/timezone/src/unpack.js
     * - <https://momentjs.com/timezone/docs/#/zone-object/>
     */
    fun charCodeToInt(char: Char): Int = when (char) {
        in '0'..'9' -> char - '0'
        in 'a'..'z' -> char - 'a' + 10
        in 'A'..'X' -> char - 'A' + 36
        else -> throw IllegalArgumentException("Invalid character: $char")
    }
    /** converts a base60 number of minutes to a whole number of seconds */
    fun base60MinutesInSeconds(string: String): Long {
        val parts = string.split('.')

        // handle negative numbers
        val sign: Int
        val minuteNumberStart: Int
        if (string.startsWith('-')) {
            minuteNumberStart = 1
            sign = -1
        } else {
            minuteNumberStart = 0
            sign = 1
        }

        // handle digits before the decimal (whole minutes)
        val whole = parts[0]
        val wholeMinutes: Long = (minuteNumberStart..whole.lastIndex).map { charCodeToInt(whole[it]) }.fold(0L) {
            acc, digit -> 60 * acc + digit
        }

        // handle digits after the decimal (seconds and less)
        val seconds = parts.getOrNull(1)?.let { fractional ->
            when (fractional.length) {
                1 -> charCodeToInt(fractional[0]) // single digit, representing seconds
                0 -> 0 // actually no fractional part
                else -> {
                    charCodeToInt(fractional[0]) + charCodeToInt(fractional[1]).let {
                        if (it >= 30) 1 else 0 // rounding the seconds digit
                    }
                }
            }
        } ?: 0

        return (wholeMinutes * SECONDS_PER_MINUTE + seconds) * sign
    }

    val zones = mutableMapOf<String, TimeZoneRulesCommon>()
    val (zonesPacked, linksPacked) = readTzdb() ?: return@runCatching null
    for (zone in zonesPacked) {
        val components = zone.split('|')
        val offsets = components[2].split(' ').map {
            UtcOffset(null, null, -base60MinutesInSeconds(it).toInt())
        }
        val indices = components[3].map { charCodeToInt(it) }
        val lengthsOfPeriodsWithOffsets = components[4].split(' ').map(::base60MinutesInSeconds)
        zones[components[0]] = TimeZoneRulesCommon(
            transitionEpochSeconds = lengthsOfPeriodsWithOffsets.runningReduce(Long::plus).let {
                if (it.size == indices.size - 1) it else it.take<Long>(indices.size - 1)
            },
            offsets = indices.map { offsets[it] },
            recurringZoneRules = null
        )
    }
    for (link in linksPacked) {
        val components = link.split('|')
        zones[components[0]]?.let { rules ->
            zones[components[1]] = rules
        }
    }
    object : TimeZoneDatabase {
        override fun rulesForId(id: String): TimeZoneRulesCommon =
            zones[id] ?: throw IllegalTimeZoneException("Unknown time zone: $id")

        override fun availableTimeZoneIds(): Set<String> = zones.keys
    }
}

private object SystemTimeZone: TimeZone() {
    override val id: String get() = "SYSTEM"

    /* https://github.com/js-joda/js-joda/blob/8c1a7448db92ca014417346049fb64b55f7b1ac1/packages/core/src/LocalDate.js#L1404-L1416 +
    * https://github.com/js-joda/js-joda/blob/8c1a7448db92ca014417346049fb64b55f7b1ac1/packages/core/src/zone/SystemDefaultZoneRules.js#L69-L71 */
    override fun atStartOfDay(date: LocalDate): Instant = localDateTimeToInstant(date.atTime(LocalTime.MIN))

    /* https://github.com/js-joda/js-joda/blob/8c1a7448db92ca014417346049fb64b55f7b1ac1/packages/core/src/zone/SystemDefaultZoneRules.js#L21-L24 */
    override fun offsetAtImpl(instant: Instant): UtcOffset =
        UtcOffset(minutes = -Date(instant.toEpochMilliseconds().toDouble()).getTimezoneOffset().toInt())

    /* https://github.com/js-joda/js-joda/blob/8c1a7448db92ca014417346049fb64b55f7b1ac1/packages/core/src/zone/SystemDefaultZoneRules.js#L49-L55 */
    override fun localDateTimeToInstant(dateTime: LocalDateTime, preferred: UtcOffset?): Instant {
        val epochMilli = dateTime.toInstant(UTC).toEpochMilliseconds()
        val offsetInMinutesBeforePossibleTransition = Date(epochMilli.toDouble()).getTimezoneOffset().toInt()
        val epochMilliSystemZone = epochMilli +
                offsetInMinutesBeforePossibleTransition * SECONDS_PER_MINUTE * MILLIS_PER_ONE
        val offsetInMinutesAfterPossibleTransition = Date(epochMilliSystemZone.toDouble()).getTimezoneOffset().toInt()
        val offset = UtcOffset(minutes = -offsetInMinutesAfterPossibleTransition)
        return dateTime.toInstant(offset)
    }

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = id.hashCode()
}

internal actual fun currentSystemDefaultZone(): Pair<String, TimeZone?> {
    val id = ZoneId.systemDefault().id()
    return if (id == "SYSTEM") id to SystemTimeZone
    else id to null
}

internal actual fun timeZoneById(zoneId: String): TimeZone {
    val id = if (zoneId == "SYSTEM") {
        val (name, zone) = currentSystemDefaultZone()
        zone?.let { return it }
        name
    } else zoneId
    rulesForId(id)?.let { return RegionTimeZone(it, id) }
    throw IllegalTimeZoneException("js-joda timezone database is not available")
}

internal fun rulesForId(zoneId: String): TimeZoneRulesCommon? = tzdb.getOrThrow()?.rulesForId(zoneId)

internal actual fun getAvailableZoneIds(): Set<String> =
    tzdb.getOrThrow()?.availableTimeZoneIds() ?: setOf("UTC")

internal external class Date() {
    constructor(milliseconds: Double)
    fun getTimezoneOffset(): Double
}
