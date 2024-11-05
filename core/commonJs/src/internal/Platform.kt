/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.internal.JSJoda.ZoneId
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val tzdb: Result<TimeZoneDatabase?> = runCatching {
    /**
     * References:
     * - https://github.com/js-joda/js-joda/blob/8c1a7448db92ca014417346049fb64b55f7b1ac1/packages/timezone/src/MomentZoneRulesProvider.js#L78-L94
     * - https://github.com/js-joda/js-joda/blob/8c1a7448db92ca014417346049fb64b55f7b1ac1/packages/timezone/src/unpack.js
     * - <https://momentjs.com/timezone/docs/#/zone-object/>
     */
    fun unpackBase60(string: String): Double {
        var i = 0
        var parts = string.split('.')
        var whole = parts[0]
        var multiplier = 1.0
        var out = 0.0
        var sign = 1

        // handle negative numbers
        if (string.startsWith('-')) {
            i = 1
            sign = -1
        }

        fun charCodeToInt(char: Char): Int =
            when (char) {
                in '0'..'9' -> char - '0'
                in 'a'..'z' -> char - 'a' + 10
                in 'A'..'X' -> char - 'A' + 36
                else -> throw IllegalArgumentException("Invalid character: $char")
            }

        // handle digits before the decimal
        for (ix in i..whole.lastIndex) {
            out = 60 * out + charCodeToInt(whole[ix])
        }

        // handle digits after the decimal
        parts.getOrNull(1)?.let { fractional ->
            for (c in fractional) {
                multiplier = multiplier / 60.0
                out += charCodeToInt(c) * multiplier
            }
        }

        return out * sign
    }

    fun <T, R> List<T>.scanWithoutInitial(initial: R, operation: (acc: R, T) -> R): List<R> = buildList {
        var accumulator = initial
        for (element in this@scanWithoutInitial) {
            accumulator = operation(accumulator, element)
            add(accumulator)
        }
    }

    fun List<Long>.partialSums(): List<Long> = scanWithoutInitial(0, Long::plus)

    val zones = mutableMapOf<String, TimeZoneRules>()
    val (zonesPacked, linksPacked) = readTzdb() ?: return@runCatching null
    for (zone in zonesPacked) {
        val components = zone.split('|')
        val offsets = components[2].split(' ').map { unpackBase60(it) }
        val indices = components[3].map { it - '0' }
        val lengthsOfPeriodsWithOffsets = components[4].split(' ').map {
            (unpackBase60(it) * SECONDS_PER_MINUTE * MILLIS_PER_ONE).roundToLong() / // minutes to milliseconds
                    MILLIS_PER_ONE // but we only need seconds
        }
        println("Zone ${components[0]}: $offsets with ${components[2]} raw data")
        zones[components[0]] = TimeZoneRules(
            transitionEpochSeconds = lengthsOfPeriodsWithOffsets.partialSums().take<Long>(indices.size - 1),
            offsets = indices.map { UtcOffset(null, null, -(offsets[it] * 60).roundToInt()) },
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
        override fun rulesForId(id: String): TimeZoneRules =
            zones[id] ?: throw IllegalTimeZoneException("Unknown time zone: $id")

        override fun availableTimeZoneIds(): Set<String> = zones.keys
    }
}

private object SystemTimeZone: TimeZone() {
    override val id: String get() = "SYSTEM"

    /* https://github.com/js-joda/js-joda/blob/8c1a7448db92ca014417346049fb64b55f7b1ac1/packages/core/src/LocalDate.js#L1404-L1416 +
    * https://github.com/js-joda/js-joda/blob/8c1a7448db92ca014417346049fb64b55f7b1ac1/packages/core/src/zone/SystemDefaultZoneRules.js#L69-L71 */
    override fun atStartOfDay(date: LocalDate): Instant = atZone(date.atTime(LocalTime.MIN)).toInstant()

    /* https://github.com/js-joda/js-joda/blob/8c1a7448db92ca014417346049fb64b55f7b1ac1/packages/core/src/zone/SystemDefaultZoneRules.js#L21-L24 */
    override fun offsetAtImpl(instant: Instant): UtcOffset =
        UtcOffset(minutes = -Date(instant.toEpochMilliseconds().toDouble()).getTimezoneOffset().toInt())

    /* https://github.com/js-joda/js-joda/blob/8c1a7448db92ca014417346049fb64b55f7b1ac1/packages/core/src/zone/SystemDefaultZoneRules.js#L49-L55 */
    override fun atZone(dateTime: LocalDateTime, preferred: UtcOffset?): ZonedDateTime {
        val epochMilli = dateTime.toInstant(UTC).toEpochMilliseconds()
        val offsetInMinutesBeforePossibleTransition = Date(epochMilli.toDouble()).getTimezoneOffset().toInt()
        val epochMilliSystemZone = epochMilli +
                offsetInMinutesBeforePossibleTransition * SECONDS_PER_MINUTE * MILLIS_PER_ONE
        val offsetInMinutesAfterPossibleTransition = Date(epochMilliSystemZone.toDouble()).getTimezoneOffset().toInt()
        val offset = UtcOffset(minutes = -offsetInMinutesAfterPossibleTransition)
        return ZonedDateTime(dateTime, this, offset)
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

internal fun rulesForId(zoneId: String): TimeZoneRules? = tzdb.getOrThrow()?.rulesForId(zoneId)

internal actual fun getAvailableZoneIds(): Set<String> =
    tzdb.getOrThrow()?.availableTimeZoneIds() ?: setOf("UTC")

internal actual fun currentTime(): Instant = Instant.fromEpochMilliseconds(Date().getTime().toLong())

internal external class Date() {
    constructor(milliseconds: Double)
    fun getTime(): Double
    fun getTimezoneOffset(): Double
}