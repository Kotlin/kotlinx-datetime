/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlin.time.Instant

internal interface TimeZoneRules {

    fun infoAtInstant(instant: Instant): UtcOffset

    fun infoAtDatetime(localDateTime: LocalDateTime): OffsetInfo
}