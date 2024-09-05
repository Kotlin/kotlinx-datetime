package kotlinx.datetime.timezones

import kotlinx.datetime.timezones.tzData.*
import kotlin.test.Test
import kotlin.test.assertContains

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
        zoneDataByName("UTC")
        zoneDataByName("GMT")
        zoneDataByName("Europe/Amsterdam")
    }
}