package kotlinx.datetime.testing

import kotlinx.datetime.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.test.*
import kotlinx.datetime.LocalDateTimeOffsetInfo.*
import kotlinx.datetime.LocalDateTimeOffsetInfo.Companion.Transition
import kotlin.time.Instant

class TimeZoneObservableBehavior(
    val infoStartsAtFirstTransition: Boolean,
    val infoEndsAtLastTransition: Boolean,
    val types: List<Type>,
    /** Must be sorted by the Instant value */
    val transitions: List<Transition<Int /* index in `types` */>>
) {
    data class Type(val offset: UtcOffset, val standardOffset: UtcOffset)
    data class Transition<T>(val transitionInstant: Instant, val before: T, val after: T)

    fun limit(transitionRange: ClosedRange<Instant>): TimeZoneObservableBehavior {
        val cutBelow = transitions.first().transitionInstant < transitionRange.start
        val cutAbove = transitions.last().transitionInstant > transitionRange.endInclusive
        return if (!cutAbove && !cutBelow) {
            this
        } else {
            TimeZoneObservableBehavior(
                infoStartsAtFirstTransition = infoStartsAtFirstTransition && !cutBelow,
                infoEndsAtLastTransition = infoEndsAtLastTransition && !cutAbove,
                types = types, // don't bother filtering out types that we no longer need
                transitions = transitions
                    .dropWhile { it.transitionInstant >= transitionRange.start }
                    .takeWhile { it.transitionInstant <= transitionRange.endInclusive },
            )
        }
    }
}

val europeBerlinBehavior by lazy {
    TimeZoneObservableBehavior(
        infoStartsAtFirstTransition = false,
        infoEndsAtLastTransition = false,
        types = listOf(
            TimeZoneObservableBehavior.Type(UtcOffset(1), standardOffset = UtcOffset(1)),
            TimeZoneObservableBehavior.Type(UtcOffset(2), standardOffset = UtcOffset(1)),
        ),
        transitions = listOf(
            TimeZoneObservableBehavior.Transition(Instant.parse("2018-03-25T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2018-10-28T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2019-03-31T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2019-10-27T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2020-03-29T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2020-10-25T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2021-03-28T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2021-10-31T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2022-03-27T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2022-10-30T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2023-03-26T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2023-10-29T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2024-03-31T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2024-10-27T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2025-03-30T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2025-10-26T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2026-03-29T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2026-10-25T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2027-03-28T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2027-10-31T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2028-03-26T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2028-10-29T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2029-03-25T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2029-10-28T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2030-03-31T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2030-10-27T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2031-03-30T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2031-10-26T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2032-03-28T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2032-10-31T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2033-03-27T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2033-10-30T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2034-03-26T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2034-10-29T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2035-03-25T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2035-10-28T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2036-03-30T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2036-10-26T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2037-03-29T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2037-10-25T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2038-03-28T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2038-10-31T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2039-03-27T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2039-10-30T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2040-03-25T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2040-10-28T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2041-03-31T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2041-10-27T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2042-03-30T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2042-10-26T01:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2043-03-29T01:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2043-10-25T01:00:00Z"), 1, 0),
        )
    )
}

val americaNewYorkBehavior by lazy {
    TimeZoneObservableBehavior(
        infoStartsAtFirstTransition = false,
        infoEndsAtLastTransition = false,
        types = listOf(
            TimeZoneObservableBehavior.Type(UtcOffset(-5), standardOffset = UtcOffset(-5)),
            TimeZoneObservableBehavior.Type(UtcOffset(-4), standardOffset = UtcOffset(-5))
        ),
        transitions = listOf(
            TimeZoneObservableBehavior.Transition(Instant.parse("2017-03-12T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2017-11-05T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2018-03-11T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2018-11-04T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2019-03-10T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2019-11-03T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2020-03-08T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2020-11-01T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2021-03-14T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2021-11-07T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2022-03-13T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2022-11-06T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2023-03-12T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2023-11-05T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2024-03-10T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2024-11-03T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2025-03-09T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2025-11-02T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2026-03-08T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2026-11-01T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2027-03-14T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2027-11-07T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2028-03-12T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2028-11-05T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2029-03-11T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2029-11-04T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2030-03-10T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2030-11-03T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2031-03-09T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2031-11-02T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2032-03-14T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2032-11-07T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2033-03-13T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2033-11-06T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2034-03-12T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2034-11-05T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2035-03-11T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2035-11-04T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2036-03-09T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2036-11-02T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2037-03-08T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2037-11-01T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2038-03-14T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2038-11-07T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2039-03-13T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2039-11-06T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2040-03-11T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2040-11-04T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2041-03-10T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2041-11-03T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2042-03-09T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2042-11-02T06:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2043-03-08T07:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2043-11-01T06:00:00Z"), 1, 0),
        )
    )
}

val australiaSydneyBehavior by lazy {
    TimeZoneObservableBehavior(
        infoStartsAtFirstTransition = false,
        infoEndsAtLastTransition = false,
        types = listOf(
            TimeZoneObservableBehavior.Type(UtcOffset(10), standardOffset = UtcOffset(10)),
            TimeZoneObservableBehavior.Type(UtcOffset(11), standardOffset = UtcOffset(10))
        ),
        transitions = listOf(
            TimeZoneObservableBehavior.Transition(Instant.parse("2017-04-01T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2017-09-30T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2018-03-31T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2018-10-06T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2019-04-06T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2019-10-05T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2020-04-04T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2020-10-03T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2021-04-03T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2021-10-02T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2022-04-02T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2022-10-01T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2023-04-01T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2023-09-30T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2024-04-06T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2024-10-05T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2025-04-05T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2025-10-04T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2026-04-04T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2026-10-03T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2027-04-03T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2027-10-02T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2028-04-01T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2028-09-30T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2029-03-31T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2029-10-06T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2030-04-06T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2030-10-05T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2031-04-05T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2031-10-04T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2032-04-03T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2032-10-02T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2033-04-02T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2033-10-01T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2034-04-01T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2034-09-30T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2035-03-31T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2035-10-06T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2036-04-05T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2036-10-04T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2037-04-04T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2037-10-03T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2038-04-03T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2038-10-02T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2039-04-02T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2039-10-01T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2040-03-31T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2040-10-06T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2041-04-06T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2041-10-05T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2042-04-05T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2042-10-04T16:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2043-04-04T16:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2043-10-03T16:00:00Z"), 0, 1),
        )
    )
}

val europeMoscowBehavior by lazy {
    TimeZoneObservableBehavior(
        infoStartsAtFirstTransition = false,
        infoEndsAtLastTransition = true,
        types = listOf(
            TimeZoneObservableBehavior.Type(UtcOffset(3), standardOffset = UtcOffset(3)),
            TimeZoneObservableBehavior.Type(UtcOffset(4), standardOffset = UtcOffset(3)),
            TimeZoneObservableBehavior.Type(UtcOffset(4), standardOffset = UtcOffset(4)),
        ),
        transitions = listOf(
            TimeZoneObservableBehavior.Transition(Instant.parse("2010-03-27T23:00:00Z"), 0, 1),
            TimeZoneObservableBehavior.Transition(Instant.parse("2010-10-30T23:00:00Z"), 1, 0),
            TimeZoneObservableBehavior.Transition(Instant.parse("2011-03-26T23:00:00Z"), 0, 2),
            TimeZoneObservableBehavior.Transition(Instant.parse("2014-10-25T22:00:00Z"), 2, 0),
        )
    )
}

fun assertTimeZoneHasBehavior(
    timeZone: TimeZone,
    behavior: TimeZoneObservableBehavior,
    standardOffsetInfoPrecision: StandardOffsetInfoPrecision = StandardOffsetInfoPrecision.ACCURATE
) {
    val knownTransitions = behavior.transitions.map {
        Transition(it.transitionInstant, behavior.types[it.before].offset, behavior.types[it.after].offset)
    }
    // Check transitions:
    for (transition in knownTransitions) {
        val ldt = transition.transitionInstant.toLocalDateTime(transition.offsetBefore.asTimeZone())
        try {
            when (transition) {
                is Gap -> checkGap(timeZone, ldt)
                is Overlap -> checkOverlap(timeZone, ldt)
            }
        } catch (e: Throwable) {
            throw AssertionError(
                "Not recognized transition $transition in $timeZone: was ${timeZone.offsetInfoFor(ldt)}", e
            )
        }
    }
    // Check regular times and standard offsets in between transitions:
    for (index in 0..(knownTransitions.size - 2)) {
        val transitionBefore = knownTransitions[index]
        val transitionAfter = knownTransitions[index + 1]
        val expectedOffset = transitionBefore.offsetAfter
        val startInstant = transitionBefore.transitionInstant
        val endInstant = transitionAfter.transitionInstant
        val steps = 5
        val delta = (endInstant - startInstant) / (steps + 2)
        var instant = startInstant + delta
        repeat(steps) {
            checkRegular(timeZone, instant.toLocalDateTime(expectedOffset.asTimeZone()), expectedOffset)
            instant += delta
        }
    }
    run {
        if (standardOffsetInfoPrecision == StandardOffsetInfoPrecision.ABSENT) return@run
        // Check standard offsets in between transitions:
        for (index in 0..(behavior.transitions.size - 2)) {
            val startInstant = behavior.transitions[index].transitionInstant
            val endInstant = behavior.transitions[index + 1].transitionInstant
            val steps = 5
            val delta = (endInstant - startInstant) / (steps + 2)
            var instant = startInstant + delta
            if (standardOffsetInfoPrecision != StandardOffsetInfoPrecision.SPLIT_BY_YEAR_BOUNDARIES ||
                behavior.types[behavior.transitions[index].after].standardOffset ==
                behavior.types[behavior.transitions[index + 1].after].standardOffset
            ) {
                repeat(steps) {
                    assertEquals(
                        behavior.types[behavior.transitions[index].after].standardOffset,
                        timeZone.standardOffsetAt(instant)
                    )
                    instant += delta
                }
            }
        }
    }
}

enum class StandardOffsetInfoPrecision {
    ABSENT,
    ACCURATE,
    SPLIT_BY_YEAR_BOUNDARIES,
}

/**
 * [gapStart] is the first non-existent moment.
 */
fun checkGap(timeZone: TimeZone, gapStart: LocalDateTime) {
    val gap = assertIs<Gap>(timeZone.offsetInfoFor(gapStart))
    assertEquals(gap.transitionInstant, gapStart.toInstant(gap.offsetBefore))
    val before = assertIs<Regular>(
        timeZone.offsetInfoFor(gapStart.plusNominalSeconds(-1))
    )
    val after = assertIs<Regular>(
        timeZone.offsetInfoFor(gap.transitionInstant.toLocalDateTime(timeZone))
    )
    assertEquals(gap.offsetBefore, before.offset)
    assertEquals(gap.offsetAfter, after.offset)
}

/**
 * [overlapStart] is the first non-ambiguous date-time.
 */
fun checkOverlap(timeZone: TimeZone, overlapStart: LocalDateTime) {
    val after = assertIs<Regular>(timeZone.offsetInfoFor(overlapStart))
    val overlap = assertIs<Overlap>(
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
        after.offset,
    )) {
        assertEquals(overlap.offsetAfter, offsetAfter)
    }
}

fun checkRegular(timeZone: TimeZone, dateTime: LocalDateTime, offset: UtcOffset) {
    val regular = assertIs<Regular>(timeZone.offsetInfoFor(dateTime))
    assertEquals(offset, regular.offset)
}

private fun LocalDateTime.plusNominalSeconds(seconds: Int): LocalDateTime =
    toInstant(TimeZone.UTC).plus(seconds, DateTimeUnit.SECOND).toLocalDateTime(TimeZone.UTC)
