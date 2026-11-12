/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.pinned

import kotlinx.datetime.*
import kotlin.test.*
import kotlin.time.Instant

/**
 * Oracle: not really important
 */
class SystemTimeZoneFuzzFindings {

    // ===============================================================================================================
    // F16 [LOW] offsetAt throws at the edges of the Instant range (JDK 8 variant)
    //
    // What happens: on JDK 8, `ZoneRules.getOffset` fails for instants near the end of the supported range, and the
    //               raw `java.time.DateTimeException` escapes from `TimeZone.offsetAt`; for the same reason
    //               `Instant.toLocalDateTime(tz)` leaks it instead of DateTimeArithmeticException.
    //               Newer JDKs return +01:00. The bundled database fails on every JDK.
    // Contract:     `TimeZone.offsetAt` documents no exceptions: every instant has an offset.
    // Root cause:   a defect of `ZoneRules.getOffset` in JDK 8 (it looks like JDK-8236903; not cross-checked);
    //               core/jvm/src/TimeZoneJvm.kt, JvmTimeZone.offsetAt does not wrap the exception.
    // Found by:     slice tz.extreme*; oracle: totality on extreme values.
    // Property:     BundledTimeZoneFuzzTest.everyInstantHasAnOffset (for the bundled database)
    // ===============================================================================================================

    @Test
    fun f16_offsetAtLeaksJavaTimeExceptionForTheMaximumInstant() {
        val max = Instant.fromEpochSeconds(Long.MAX_VALUE)
        assertEquals(UtcOffset(hours = 1), TimeZoneContext.System.get("Europe/Berlin").offsetAt(max))
    }
}
