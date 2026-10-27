/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.serializers.*
import java.time.ZoneId
import kotlin.time.Instant

public actual class FixedOffsetTimeZone internal constructor(
    public actual val offset: UtcOffset,
    actual override val id: String,
    internal val actualZoneId: Lazy<ZoneId>
) : TimeZone() {
    internal actual constructor(offset: UtcOffset, id: String) : this(offset, id, lazy { ZoneId.of(id) })

    public actual constructor(offset: UtcOffset) : this(offset, offset.toString())

    @Deprecated("Use offset.totalSeconds", ReplaceWith("offset.totalSeconds"))
    public actual val totalSeconds: Int get() = offset.totalSeconds

    actual override fun offsetAt(instant: Instant): UtcOffset = offset

    actual override fun offsetInfoFor(dateTime: LocalDateTime): LocalDateTimeOffsetInfo =
        LocalDateTimeOffsetInfo.Regular(offset)

    actual override fun toString(): String = id

    actual override fun equals(other: Any?): Boolean =
        this === other || other is FixedOffsetTimeZone && this.id == other.id

    actual override fun hashCode(): Int = id.hashCode()

    public actual companion object {
        @Deprecated(
            "Serializing FixedOffsetTimeZone is discouraged, " +
                    "as deserialization can fail or return a non-fixed-offset zone depending on the configuration. " +
                    "Please serialize the string id instead.",
            level = DeprecationLevel.WARNING,
        )
        @Suppress("DEPRECATION")
        public actual fun serializer(): kotlinx.serialization.KSerializer<FixedOffsetTimeZone> =
            FixedOffsetTimeZoneSerializer

        internal actual fun withSpecificName(offset: UtcOffset, id: String): FixedOffsetTimeZone =
            FixedOffsetTimeZone(offset, id)

        internal actual fun withSpecificPrefix(offset: UtcOffset, prefix: String): FixedOffsetTimeZone =
            when (offset.totalSeconds) {
                0 -> withSpecificName(offset, prefix)
                else -> withSpecificName(offset, "$prefix$offset")
            }
    }
}