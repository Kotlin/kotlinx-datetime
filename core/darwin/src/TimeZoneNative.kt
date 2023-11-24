/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(kotlinx.cinterop.UnsafeNumber::class)

package kotlinx.datetime

import kotlinx.datetime.internal.Path
import kotlinx.datetime.internal.TzdbOnFilesystem
import platform.Foundation.*

internal actual fun currentTime(): Instant = NSDate.date().toKotlinInstant()

internal actual val tzdbOnFilesystem = TzdbOnFilesystem(Path.fromString("/var/db/timezone/zoneinfo"))
