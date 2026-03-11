package kotlinx.datetime

import kotlin.test.*

class LocalIsoWeekDateTestJvm {
    // Check that we agree with Java.Time regarding which dates correspond to which LocalIsoWeekDates
    @Test
    fun randomizedJavaCompatibilityTest() {
        val maxDay = -java.time.LocalDate.MAX.until(java.time.LocalDate.ofEpochDay(0), java.time.temporal.ChronoUnit.DAYS)
        val minDay = -java.time.LocalDate.MIN.until(java.time.LocalDate.ofEpochDay(0), java.time.temporal.ChronoUnit.DAYS)
        repeat(100000) {
            val randomDay = kotlin.random.Random.nextLong(minDay, maxDay)
            val jvmDate = java.time.LocalDate.ofEpochDay(randomDay)
            val ktDate = LocalDate.fromEpochDays(randomDay)
            val weekDateString = java.time.format.DateTimeFormatter.ISO_WEEK_DATE.format(jvmDate)
            val yearWeekDate = LocalIsoWeekDate.parse(weekDateString)
            assertEquals(weekDateString, yearWeekDate.toString())
            assertEquals(yearWeekDate, ktDate.toLocalIsoWeekDate())
            assertEquals(ktDate, yearWeekDate.toLocalDate())
        }
    }
}
