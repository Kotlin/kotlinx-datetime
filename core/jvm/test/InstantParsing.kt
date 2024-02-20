package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlin.test.*

class InstantParsing {
    @Test
    fun testParsingInvalidInstants() {
        fun parseInstantLikeJavaDoes(input: String): Instant =
            DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET.parse(input).apply {
                when {
                    hour == 24 && minute == 0 && second == 0 && nanosecond == 0 -> {
                        setDate(toLocalDate().plus(1, DateTimeUnit.DAY))
                        hour = 0
                    }
                    hour == 23 && minute == 59 && second == 60 -> second = 59
                }
            }.toInstantUsingOffset()
        fun formatTwoDigits(i: Int) = if (i < 10) "0$i" else "$i"
        for (hour in 23..25) {
            for (minute in listOf(0..5, 58..62).flatten()) {
                for (second in listOf(0..5, 58..62).flatten()) {
                    val input = "2020-03-16T${hour}:${formatTwoDigits(minute)}:${formatTwoDigits(second)}Z"
                    assertEquals(
                        runCatching { java.time.Instant.parse(input) }.getOrNull(),
                        runCatching { parseInstantLikeJavaDoes(input).toJavaInstant() }.getOrNull()
                    )
                }
            }
        }
    }

}
