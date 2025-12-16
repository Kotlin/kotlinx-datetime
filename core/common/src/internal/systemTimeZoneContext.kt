/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*

internal expect fun currentSystemDefaultTimeZone(): TimeZone
internal expect val systemTimezoneDatabase: TimeZoneDatabase
internal expect val systemTimeZoneIdProvider: TimeZoneIdProvider
