/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*

internal sealed interface OffsetInfo {
    data class Gap(
        val start: Instant,
        val offsetBefore: UtcOffset,
        val offsetAfter: UtcOffset
    ): OffsetInfo {
        init {
            check(offsetBefore.totalSeconds < offsetAfter.totalSeconds)
        }

        val transitionDurationSeconds: Int get() = offsetAfter.totalSeconds - offsetBefore.totalSeconds
    }

    data class Overlap(
        val start: Instant,
        val offsetBefore: UtcOffset,
        val offsetAfter: UtcOffset
    ): OffsetInfo {
        init {
            check(offsetBefore.totalSeconds > offsetAfter.totalSeconds)
        }
    }

    data class Regular(
        val offset: UtcOffset
    ) : OffsetInfo
}

internal fun OffsetInfo(transitionInstant: Instant, offsetBefore: UtcOffset, offsetAfter: UtcOffset): OffsetInfo =
    if (offsetBefore == offsetAfter) {
        OffsetInfo.Regular(offsetBefore)
    } else if (offsetBefore.totalSeconds < offsetAfter.totalSeconds) {
        OffsetInfo.Gap(transitionInstant, offsetBefore, offsetAfter)
    } else {
        OffsetInfo.Overlap(transitionInstant, offsetBefore, offsetAfter)
    }

