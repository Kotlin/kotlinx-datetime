/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*
import kotlinx.datetime.format.*

internal class FixedOffsetTimeZoneDatabase(val inner: TimeZoneDatabase): TimeZoneDatabase {
    override fun get(id: String): TimeZone {
        return parseFixedOffsetTimeZone(
            id,
            onParsed = { it },
            onFailure = {
                inner.getOrNull(id) ?: throw IllegalTimeZoneException("Malformed UTC offset '$it' in zone ID '$id'")
            },
        ) ?: inner.get(id)
    }

    override fun getOrNull(id: String): TimeZone? =
        parseFixedOffsetTimeZone(id, onParsed = { it }, onFailure = { null }) ?: inner.getOrNull(id)

    override fun availableZoneIds(): Set<String> = inner.availableZoneIds()
}

private fun <T> parseFixedOffsetTimeZone(
    id: String, onParsed: (TimeZone) -> T, onFailure: (String) -> T
): T? {
    fun parse(offset: String, prefix: String): T = when (val parsedOffset = lenientOffsetFormat.parseOrNull(offset)) {
        null -> onFailure(prefix)
        else -> onParsed(parsedOffset.asTimeZone(prefix))
    }
    return when {
        id == "UTC" -> onParsed(TimeZone.UTC)
        id == "Z" || id == "z" -> onParsed(UtcOffset.ZERO.asTimeZone())
        id.length == 1 -> null
        id.startsWith("+") || id.startsWith("-") -> parse(id, "")
        id == "UTC" || id == "GMT" || id == "UT" -> onParsed(FixedOffsetTimeZone(UtcOffset.ZERO, id))
        id.startsWith("UTC+") || id.startsWith("GMT+") || id.startsWith("UTC-") || id.startsWith("GMT-") ->
            parse(id.substring(3), id.take(3))
        id.startsWith("UT+") || id.startsWith("UT-") ->
            parse(id.substring(2), "UT")
        else -> null
    }
}

private val lenientOffsetFormat = UtcOffset.Format {
    alternativeParsing(
        {
            offsetHours(Padding.NONE)
        },
        {
            isoOffset(
                zOnZero = false,
                useSeparator = false,
                outputMinute = WhenToOutput.IF_NONZERO,
                outputSecond = WhenToOutput.IF_NONZERO
            )
        }
    ) {
        isoOffset(
            zOnZero = true,
            useSeparator = true,
            outputMinute = WhenToOutput.ALWAYS,
            outputSecond = WhenToOutput.IF_NONZERO
        )
    }
}
