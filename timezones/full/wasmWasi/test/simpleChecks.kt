package kotlinx.datetime.timezones

import kotlinx.datetime.timezones.tzData.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull

class SimpleChecks {
    @Test
    fun getTimeZonesTest() {
        val timezones = timeZones
        assertContains(timezones, "UTC")
        assertContains(timezones, "GMT")
        assertContains(timezones, "Europe/Amsterdam")
    }

    @Test
    fun checkZonesData() {
        assertNotNull(zoneDataByNameOrNull("UTC"))
        assertNotNull(zoneDataByNameOrNull("GMT"))
        assertNotNull(zoneDataByNameOrNull("Europe/Amsterdam"))
    }
}
