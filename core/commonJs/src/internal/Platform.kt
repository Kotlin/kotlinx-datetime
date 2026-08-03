/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*
import kotlinx.datetime.internal.JSJoda.ZoneId
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.js.*

private val jodaTzdb: Result<RuleBasedTimeZoneDatabase?> = runCatching {
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
    object : RuleBasedTimeZoneDatabase {
        override fun rulesForIdOrNull(id: String): TimeZoneRulesCommon? = zones[id]

        override fun availableZoneIds(): Set<String> = zones.keys
    }
}

private object SystemTimeZone: TimeZone() {
    override val id: String get() = "SYSTEM"

    /* https://github.com/js-joda/js-joda/blob/8c1a7448db92ca014417346049fb64b55f7b1ac1/packages/core/src/zone/SystemDefaultZoneRules.js#L21-L24 */
    override fun offsetAtImpl(instant: Instant): UtcOffset =
        UtcOffset(minutes = -Date(instant.toEpochMilliseconds().toDouble()).getTimezoneOffset().toInt())

    // Assuming there are not going to be multiple transitions on the same day or transitions of 24 hours or longer
    override fun offsetInfoForImpl(dateTime: LocalDateTime): LocalDateTimeOffsetInfo {
        val offsetGuess = Date(milliseconds = dateTime.toInstant(UTC).toEpochMilliseconds().toDouble())
            .getTimezoneOffset().toInt().let { UtcOffset(minutes = -it) }
        val instantGuess = dateTime.toInstant(offsetGuess)
        val offsetBefore = offsetAtImpl(instantGuess - 24.hours)
        val offsetAfter = offsetAtImpl(instantGuess + 24.hours)
        // No transitions (assuming no wild irregularities)
        if (offsetBefore == offsetAfter) return LocalDateTimeOffsetInfo.Regular(offsetGuess)
        // Binary search for the transition
        var l = -(24.hours.inWholeSeconds)
        var r = 24.hours.inWholeSeconds
        while (l != r) {
            val current = (l + r) / 2 // small values, no need for tricks
            if (offsetAtImpl(instantGuess + current.seconds) == offsetBefore) {
                l = current + 1
            } else {
                r = current
            }
        }
        val transitionStart = instantGuess + l.seconds // first moment with `offsetAfter`
        return if (offsetBefore.totalSeconds < offsetAfter.totalSeconds) {
            // Gap
            when {
                dateTime < transitionStart.toLocalDateTime(offsetBefore) -> LocalDateTimeOffsetInfo.Regular(offsetBefore)
                dateTime >= transitionStart.toLocalDateTime(offsetAfter) -> LocalDateTimeOffsetInfo.Regular(offsetAfter)
                else -> LocalDateTimeOffsetInfo.Gap(transitionStart, offsetBefore, offsetAfter)
            }
        } else {
            // Overlap
            when {
                dateTime < transitionStart.toLocalDateTime(offsetAfter) -> LocalDateTimeOffsetInfo.Regular(offsetAfter)
                dateTime >= transitionStart.toLocalDateTime(offsetBefore) -> LocalDateTimeOffsetInfo.Regular(offsetBefore)
                else -> LocalDateTimeOffsetInfo.Overlap(transitionStart, offsetBefore, offsetAfter)
            }
        }
    }

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = id.hashCode()
}

internal fun rulesForIdForTests(zoneId: String): TimeZoneRulesCommon? = jodaTzdb.getOrThrow()?.rulesForId(zoneId)

internal external class Date() {
    constructor(milliseconds: Double)
    fun getTimezoneOffset(): Double
}

internal actual fun currentSystemDefaultTimeZone(): TimeZone =
    ZoneId.systemDefault().id().let { name ->
        if (name == "SYSTEM") SystemTimeZone else systemTimezoneDatabase.get(name)
    }

internal actual val timeZoneDatabaseImpl: TimeZoneDatabase = object: TimeZoneDatabase {
    override fun get(id: String): TimeZone = if (id == "SYSTEM") {
        val name = ZoneId.systemDefault().id()
        if (name == "SYSTEM") SystemTimeZone
        else TimeZoneContext.System.get(name)
    } else {
        val tzdb = jodaTzdb.getOrThrow() ?: throw IllegalTimeZoneException("js-joda timezone database is not available")
        tzdb.get(id)
    }

    override fun getOrNull(id: String): TimeZone? = if (id == "SYSTEM") {
        val name = ZoneId.systemDefault().id()
        if (name == "SYSTEM") SystemTimeZone
        else TimeZoneContext.System.getOrNull(name)
    } else {
        jodaTzdb.getOrThrow()?.getOrNull(id)
    }

    override fun availableZoneIds(): Set<String> = jodaTzdb.getOrThrow()?.availableZoneIds() ?: setOf("UTC")

    override fun toString() = "TzdbJsJodaBased"
}

internal actual val systemTimeZoneIdProvider: TimeZoneIdProvider = object: TimeZoneIdProvider {
    override fun currentTimeZoneId(): String = currentTimeZoneIdImpl()
}

private fun currentTimeZoneIdImpl(): String = js("Intl.DateTimeFormat().resolvedOptions().timeZone")
