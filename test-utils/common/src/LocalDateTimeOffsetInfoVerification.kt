package kotlinx.datetime.testing

import kotlinx.datetime.*
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.test.*

/**
 * [gapStart] is the first non-existent moment.
 */
@OptIn(ExperimentalTime::class)
fun checkGap(timeZone: TimeZone, gapStart: LocalDateTime) {
    val gap = assertIs<LocalDateTimeOffsetInfo.Gap>(timeZone.offsetInfoFor(gapStart))
    assertEquals(gap.transitionInstant, gapStart.toInstant(gap.offsetBefore))
    val before = assertIs<LocalDateTimeOffsetInfo.Regular>(
        timeZone.offsetInfoFor(gapStart.plusNominalSeconds(-1))
    )
    val after = assertIs<LocalDateTimeOffsetInfo.Regular>(
        timeZone.offsetInfoFor(gap.transitionInstant.toLocalDateTime(timeZone))
    )
    assertEquals(gap.offsetBefore, before.offset)
    assertEquals(gap.offsetAfter, after.offset)
}

/**
 * [overlapStart] is the first non-ambiguous date-time.
 */
@OptIn(ExperimentalTime::class)
fun checkOverlap(timeZone: TimeZone, overlapStart: LocalDateTime) {
    val after = assertIs<LocalDateTimeOffsetInfo.Regular>(timeZone.offsetInfoFor(overlapStart))
    val overlap = assertIs<LocalDateTimeOffsetInfo.Overlap>(
        timeZone.offsetInfoFor(overlapStart.plusNominalSeconds(-1))
    )
    val instantEnd = overlapStart.toInstant(timeZone, TransitionHandler.REJECT_TRANSITIONS)
    for (offsetBefore in listOf(
        (overlap.transitionInstant - 2.nanoseconds).offsetIn(timeZone),
        (overlap.transitionInstant - 1.nanoseconds).offsetIn(timeZone),
    )) {
        assertEquals(overlap.offsetBefore, offsetBefore)
    }
    for (offsetAfter in listOf(
        overlap.transitionInstant.offsetIn(timeZone),
        (overlap.transitionInstant + 1.nanoseconds).offsetIn(timeZone),
        instantEnd.offsetIn(timeZone),
    )) {
        assertEquals(overlap.offsetAfter, offsetAfter)
    }
}

@OptIn(ExperimentalTime::class)
fun checkRegular(timeZone: TimeZone, dateTime: LocalDateTime, offset: UtcOffset) {
    val regular = assertIs<LocalDateTimeOffsetInfo.Regular>(timeZone.offsetInfoFor(dateTime))
    assertEquals(offset, regular.offset)
}

private fun LocalDateTime.plusNominalSeconds(seconds: Int): LocalDateTime =
    toInstant(TimeZone.UTC).plus(seconds, DateTimeUnit.SECOND).toLocalDateTime(TimeZone.UTC)
