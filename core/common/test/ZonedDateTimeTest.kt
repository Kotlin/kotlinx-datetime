package kotlinx.datetime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class ZonedDateTimeTest {
    @Test
    fun parseZonedDateTime() {
        val offsetDates = listOf(
            "2021-12-29 17:32:01Z",
            "2021-12-29T17:32:01Z",
            "2021-12-29 17:32:01+03:00",
            "2021-12-29 17:32:01-03:00",
        )
        val regionDates = listOf(
            "2021-12-29 17:32:01[Europe/Berlin]",
            "2021-12-29 17:32:01+01:00[Europe/Berlin]",
        )
        for (isoString in offsetDates + regionDates) {
            val dateTime = ZonedDateTime.parse(isoString)
            assertEquals(isoString.replace(" ", "T"), dateTime.toString())
            if (isoString in offsetDates) {
                assertIs<OffsetDateTime>(dateTime)
            } else {
                assertIs<RegionDateTime>(dateTime)
            }
        }
    }
}
