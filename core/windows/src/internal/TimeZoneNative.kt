/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal actual val systemTzdb: TimeZoneDatabase get() = tzdbInRegistry

internal actual fun currentSystemDefaultZone(): Pair<String, TimeZoneRules?> = tzdbInRegistry.currentSystemDefault()

private val tzdbInRegistry = TzdbInRegistry()
