/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.serializers.UtcOffsetSerializer
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.ZoneOffset

@Serializable(with = UtcOffsetSerializer::class)
public actual class UtcOffset(internal val zoneOffset: ZoneOffset) {
    public actual val totalSeconds: Int get() = zoneOffset.totalSeconds

    override fun hashCode(): Int = zoneOffset.hashCode()
    override fun equals(other: Any?): Boolean = other is UtcOffset && this.zoneOffset == other.zoneOffset
    override fun toString(): String = zoneOffset.toString()

    public actual companion object {

        public actual val ZERO: UtcOffset = UtcOffset(ZoneOffset.UTC)

        public actual fun parse(offsetString: String): UtcOffset = try {
            ZoneOffset.of(offsetString).let(::UtcOffset)
        } catch (e: DateTimeException) {
            throw DateTimeFormatException(e)
        }
    }
}