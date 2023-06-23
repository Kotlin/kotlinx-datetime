/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.cinterop.*
import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import platform.posix.*
import kotlin.io.encoding.*
import kotlin.test.*

class TimeZoneRulesCompleteTest {
    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun iterateOverAllTimezones() {
        val root = Path.fromString("/usr/share/zoneinfo")
        val tzdb = TzdbOnFilesystem(root)
        for (id in tzdb.availableTimeZoneIds()) {
            val file = root.resolve(Path.fromString(id))
            val rules = tzdb.rulesForId(id)
            runUnixCommand("env LOCALE=C zdump -V $file").windowed(size = 2, step = 2).forEach { (line1, line2) ->
                val beforeTransition = parseZdumpLine(line1)
                val afterTransition = parseZdumpLine(line2)
                try {
                    val infoAfter = rules.infoAtInstant(afterTransition.instant)
                    val infoBefore = rules.infoAtInstant(beforeTransition.instant)
                    assertEquals(beforeTransition.offset, infoBefore)
                    assertEquals(afterTransition.offset, infoAfter)
                    if (beforeTransition.localDateTime.plusSeconds(1) == afterTransition.localDateTime) {
                        // Regular
                        val infoAt1 = rules.infoAtDatetime(beforeTransition.localDateTime)
                        val infoAt2 = rules.infoAtDatetime(afterTransition.localDateTime)
                        assertEquals(infoAt1, infoAt2)
                        assertIs<OffsetInfo.Regular>(infoAt1)
                    } else if (afterTransition.localDateTime < beforeTransition.localDateTime) {
                        // Overlap
                        val infoAt1 = rules.infoAtDatetime(beforeTransition.localDateTime.plusSeconds(-1))
                        val infoAt2 = rules.infoAtDatetime(afterTransition.localDateTime.plusSeconds(1))
                        assertEquals(infoAt1, infoAt2)
                        assertIs<OffsetInfo.Overlap>(infoAt1)
                    } else if (afterTransition.localDateTime > beforeTransition.localDateTime) {
                        // Gap
                        val infoAt1 = rules.infoAtDatetime(afterTransition.localDateTime.plusSeconds(-1))
                        val infoAt2 = rules.infoAtDatetime(beforeTransition.localDateTime.plusSeconds(1))
                        assertIs<OffsetInfo.Gap>(infoAt1)
                        assertEquals(infoAt1, infoAt2)
                    }
                } catch (e: Throwable) {
                    println(beforeTransition)
                    println(afterTransition)
                    println(Base64.encode(file.readBytes()))
                    throw e
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private inline fun runUnixCommand(command: String): Sequence<String> = sequence {
    val pipe = popen(command, "r") ?: error("Failed to run command: $command")
    try {
        memScoped {
            // read line by line
            while (true) {
                val linePtr = alloc<CPointerVar<ByteVar>>()
                val nPtr = alloc<ULongVar>()
                try {
                    val result = getline(linePtr.ptr, nPtr.ptr, pipe)
                    if (result != (-1).convert<ssize_t>()) {
                        yield(linePtr.value!!.toKString())
                    } else {
                        break
                    }
                } finally {
                    free(linePtr.value)
                }
            }
        }
    } finally {
        pclose(pipe)
    }
}

private fun parseZdumpLine(line: String): ZdumpLine {
    val parts = line.indexOf("  ")
    val path = Path.fromString(line.substring(0, parts))
    val equalSign = line.indexOf(" = ")
    val firstDate = line.substring(parts + 2, equalSign)
    val isDstStart = line.indexOf(" isdst=")
    val secondDate = line.substring(equalSign + 3, isDstStart)
    val isDstEnd = line.indexOf(" gmtoff=")
    val isDst = line.substring(isDstStart + 7, isDstEnd).toInt() != 0
    val offset = line.substring(isDstEnd + 8, line.length - 1).toInt()
    val (firstLdt, firstAbbreviation) = parseRfc2822(firstDate)
    check(firstAbbreviation == "UT")
    val (secondLdt, secondAbbreviation) = parseRfc2822(secondDate)
    return ZdumpLine(
        path,
        firstLdt.toInstant(UtcOffset.ZERO),
        secondLdt,
        secondAbbreviation,
        isDst,
        UtcOffset(seconds = offset),
    )
}

// TODO: use the datetime formatting capabilities when they are available
fun parseRfc2822(input: String): Pair<LocalDateTime, String> {
    val abbreviation = input.substringAfterLast(" ")
    val dateTime = input.substringBeforeLast(" ")
    val components = dateTime.split(Regex(" +"))
    val dayOfWeek = components[0]
    val month = components[1]
    val dayOfMonth = components[2].toInt()
    val time = LocalTime.parse(components[3])
    val year = components[4].toInt()
    val monthNumber = when (month) {
        "Jan" -> 1
        "Feb" -> 2
        "Mar" -> 3
        "Apr" -> 4
        "May" -> 5
        "Jun" -> 6
        "Jul" -> 7
        "Aug" -> 8
        "Sep" -> 9
        "Oct" -> 10
        "Nov" -> 11
        "Dec" -> 12
        else -> error("Unknown month: $month")
    }
    return LocalDateTime(LocalDate(year, monthNumber, dayOfMonth), time) to abbreviation
}

private class ZdumpLine(
    val path: Path,
    val instant: Instant,
    val localDateTime: LocalDateTime,
    val abbreviation: String,
    val isDst: Boolean,
    val offset: UtcOffset,
) {
    override fun toString(): String = "$path $instant = $localDateTime $abbreviation isDst=$isDst offset=$offset"
}
