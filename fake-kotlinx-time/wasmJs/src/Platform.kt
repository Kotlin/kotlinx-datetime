/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.time

internal actual fun currentTime(): Instant = Instant.fromEpochMilliseconds(Date().getTime().toLong())

private external class Date {
    fun getTime(): Double
}
