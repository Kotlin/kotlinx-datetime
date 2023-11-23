/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE")

package kotlinx.datetime.internal

import kotlinx.datetime.internal.JSJoda.*

internal actual fun JodaTimeZoneId.toZoneOffset(): JodaTimeZoneOffset? = this as? JodaTimeZoneOffset

public actual typealias JodaTimeTemporalAccessor = TemporalAccessor
public actual typealias JodaTimeTemporal = Temporal
public actual typealias JodaTimeTemporalUnit = TemporalUnit
public actual typealias JodaTimeTemporalAmount = TemporalAmount
public actual typealias JodaTimeChronoLocalDate = ChronoLocalDate
public actual typealias JodaTimeChronoLocalDateTime = ChronoLocalDateTime
public actual typealias JodaTimeChronoZonedDateTime = ChronoZonedDateTime
public actual typealias JodaTimeChronoUnit = ChronoUnit
public actual typealias JodaTimeClock = Clock
public actual typealias JodaTimeDuration = Duration
public actual typealias JodaTimeInstant = Instant
public actual typealias JodaTimeLocalDate = LocalDate
public actual typealias JodaTimeLocalDateTime = LocalDateTime
public actual typealias JodaTimeLocalTime = LocalTime
public actual typealias JodaTimeOffsetDateTime = OffsetDateTime
public actual typealias JodaTimeZonedDateTime = ZonedDateTime
public actual typealias JodaTimeZoneId = ZoneId
public actual typealias JodaTimeZoneOffset = ZoneOffset
public actual typealias JodaTimeDayOfWeek = DayOfWeek
public actual typealias JodaTimeMonth = Month
public actual typealias JodaTimeZoneRules = ZoneRules