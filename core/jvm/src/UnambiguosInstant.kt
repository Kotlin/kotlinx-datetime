/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

public sealed interface UnambiguousInstant {
    public val instants: List<Instant>

    public data object Impossible : UnambiguousInstant {
        override val instants: List<Instant> get() = emptyList()
    }

    public data class Unique(val instant: Instant) : UnambiguousInstant {
        override val instants: List<Instant> get() = listOf(instant)
    }

    public data class Duplicate(val instant0: Instant, val instant1: Instant) : UnambiguousInstant {
        override val instants: List<Instant> get() = listOf(instant0, instant1)
    }

    public companion object {
        public fun of(dateTime: LocalDateTime, zone: TimeZone): UnambiguousInstant {
            if(zone is FixedOffsetTimeZone) return Unique(dateTime.toInstant(zone.offset))
            val rules = zone.zoneId.rules
            val offsets = rules.getValidOffsets(dateTime.toJavaLocalDateTime())
            return when(offsets.size) {
                0 -> Impossible
                1 -> Unique(dateTime.toInstant(zone))
                else -> Duplicate(
                    dateTime.toInstant(TimeZone(offsets[0])),
                    dateTime.toInstant(TimeZone(offsets[1])),
                )
            }
        }
    }
}