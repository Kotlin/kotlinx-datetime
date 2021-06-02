/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.JSJoda.ZoneOffset

public actual class UtcOffset(internal val zoneOffset: ZoneOffset) {
    public actual val totalSeconds: Int get() = zoneOffset.totalSeconds().toInt()

    override fun hashCode(): Int = zoneOffset.hashCode().toInt()
    override fun equals(other: Any?): Boolean = other is UtcOffset && this.zoneOffset == other.zoneOffset
    override fun toString(): String = zoneOffset.toString()

    public actual companion object {
        public actual fun parse(offsetString: String): UtcOffset = try {
            ZoneOffset.of(offsetString).let(::UtcOffset)
        } catch (e: Throwable) {
            if (e.isJodaDateTimeException()) throw DateTimeFormatException(e)
            throw e
        }
    }
}