package kotlinx.datetime

import kotlin.test.*

class LocalIsoWeekDateTestJvm {
    private val maxDay =
        -java.time.LocalDate.MAX.until(java.time.LocalDate.ofEpochDay(0), java.time.temporal.ChronoUnit.DAYS)
    private val minDay =
        -java.time.LocalDate.MIN.until(java.time.LocalDate.ofEpochDay(0), java.time.temporal.ChronoUnit.DAYS)

    private fun assertSameAsJavaTime(epochDay: Long) {
        val jvmDate = java.time.LocalDate.ofEpochDay(epochDay)
        val ktDate = LocalDate.fromEpochDays(epochDay)
        val weekDateString = java.time.format.DateTimeFormatter.ISO_WEEK_DATE.format(jvmDate)
        val yearWeekDate = LocalIsoWeekDate.parse(weekDateString)
        assertEquals(weekDateString, yearWeekDate.toString())
        assertEquals(yearWeekDate, ktDate.toLocalIsoWeekDate())
        assertEquals(ktDate, yearWeekDate.toLocalDate())
    }

    // Check that we agree with Java.Time regarding which dates correspond to which LocalIsoWeekDates
    @Test
    fun randomizedJavaCompatibilityTest() {
        repeat(100000) {
            assertSameAsJavaTime(kotlin.random.Random.nextLong(minDay, maxDay + 1))
        }
    }

    @Test
    fun boundaryJavaCompatibilityTest() {
        for (epochDay in minDay..minDay + 800) assertSameAsJavaTime(epochDay)
        for (epochDay in maxDay - 800..maxDay) assertSameAsJavaTime(epochDay)
    }
}
