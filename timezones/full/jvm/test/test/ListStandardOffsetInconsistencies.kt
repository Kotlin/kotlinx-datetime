/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.zoneinfo

import java.time.*
import kotlin.ranges.rangeTo
import kotlin.test.*
import kotlin.time.*
import kotlinx.datetime.TimeZoneContext
import kotlinx.datetime.toJavaZoneOffset
import java.time.zone.ZoneRulesProvider

class ListStandardOffsetInconsistencies {

    /**
     * Run manually to determine the differences between the handling of standard offsets on the JVM
     * (which supplies the information about standard offsets directly)
     * and our approximation based on isDst/isStd flags in a `tzfile`.
     */
    @Ignore
    @Test
    fun listInconsistenciesWithJvmStandardOffsets() {
        val inconsistencies = mutableListOf<Inconsistency<LocalDate>>()
        for (id in TimeZoneContext.Bundled.availableZoneIds()) {
            val ourTimeZone = TimeZoneContext.Bundled.get(id)
            val zi = try {
                ZoneId.of(id).rules
            } catch (_: Throwable) {
                continue
            }
            var firstError: LocalDate? = null
            var date = LocalDate.of(1900, 1, 1)
            while (date < LocalDate.of(2040, 1, 1)) {
                val dateTime = date.atTime(12, 0)
                val moment = dateTime.atOffset(zi.getOffset(dateTime)).toInstant()
                val expected = zi.getStandardOffset(moment)
                val actual = ourTimeZone.standardOffsetAt(moment.toKotlinInstant()).toJavaZoneOffset()
                when {
                    firstError == null -> {
                        if (expected != actual) {
                            firstError = date
                        }
                    }
                    expected == actual -> {
                        inconsistencies.add(Inconsistency(firstError..date, id))
                        firstError = null
                    }
                }
                date = date.plusDays(1)
            }
            if (firstError != null) {
                inconsistencies.add(Inconsistency(firstError..LocalDate.MAX, id))
            }
        }
        println("JVM timezone data version: ${ZoneRulesProvider.getVersions("America/New_York").keys}")
        println(inconsistencies)
    }

    data class Inconsistency<T: Comparable<T>>(
        val dates: ClosedRange<T>,
        val id: String,
    )

}
