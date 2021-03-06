/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.serializers.UtcOffsetSerializer
import kotlinx.serialization.Serializable

@Serializable(with = UtcOffsetSerializer::class)
public expect class UtcOffset {
    public val totalSeconds: Int

    public companion object {
        public val ZERO: UtcOffset
        public fun parse(offsetString: String): UtcOffset
    }
}
public expect fun UtcOffset(hours: Int? = null, minutes: Int? = null, seconds: Int? = null): UtcOffset

@Deprecated("Use UtcOffset.ZERO instead", ReplaceWith("UtcOffset.ZERO"), DeprecationLevel.ERROR)
public fun UtcOffset(): UtcOffset = UtcOffset.ZERO

public fun UtcOffset.asTimeZone(): FixedOffsetTimeZone = FixedOffsetTimeZone(this)