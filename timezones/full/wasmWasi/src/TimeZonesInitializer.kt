/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.timezones

import kotlinx.datetime.zoneinfo.RawBundledTimeZoneDatabase

@Suppress("DEPRECATION", "UNUSED")
@OptIn(ExperimentalStdlibApi::class)
@EagerInitialization
private val initializeTimeZones = kotlinx.datetime.internal.initializeTimeZonesProvider(RawBundledTimeZoneDatabase)
