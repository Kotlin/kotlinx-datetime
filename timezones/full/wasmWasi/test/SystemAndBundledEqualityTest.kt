package kotlinx.datetime.zoneinfo

import kotlinx.datetime.*
import kotlin.test.*

class SystemAndBundledEqualityTest {
    @Test
    fun testBundledEqualsSystem() {
        for (id in TimeZoneContext.Bundled.availableZoneIds()) {
            assertEquals(TimeZoneContext.Bundled.get(id), TimeZoneContext.System.get(id))
        }
    }
}