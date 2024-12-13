/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.time

internal actual fun currentTime(): Instant = if (javaTimeAvailable) {
    java.time.Instant.now().toKotlinInstant()
} else {
    /* After experimenting in Android Studio, it seems like on Android with API < 24, only millisecond precision
    is available in `Instant.now()` with core library desugaring enabled. Because of that, `currentTimeMillis`
    is good enough + suggesting that our users enable core library desugaring isn't going to bring any benefits,
    so the KDoc for [Clock] does not mention any of this. */
    Instant.fromEpochMilliseconds(System.currentTimeMillis())
}

/**
 * `false` for Android devices with API level < 24, where java.time is not available.
 */
private val javaTimeAvailable = try {
    java.time.Instant.now()
    true
} catch (e: NoClassDefFoundError) {
    false
}
