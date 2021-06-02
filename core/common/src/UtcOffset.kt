/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

public expect class UtcOffset {
    public val totalSeconds: Int

    public companion object {
        public fun parse(offsetString: String): UtcOffset
    }
}

public fun UtcOffset.asTimeZone(): FixedOffsetTimeZone = FixedOffsetTimeZone(this)