/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*

internal class FoundationTimeZoneRules : TimeZoneRules {
    override fun infoAtInstant(instant: Instant): UtcOffset {
        TODO("Not yet implemented")
    }

    override fun infoAtDatetime(localDateTime: LocalDateTime): OffsetInfo {
        TODO("Not yet implemented")
    }
}
