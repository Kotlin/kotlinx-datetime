/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

/**
 * Represents a locale, which is used to format date and time values according to cultural conventions.
 *
 * This is a platform-specific type:
 * - On JVM: [java.util.Locale]
 * - On iOS/macOS: NSLocale
 * - On other platforms: platform-specific locale representation
 *
 * @sample kotlinx.datetime.test.samples.LocaleSamples.usage
 */
public expect class Locale {
    public companion object {
        /**
         * Returns the default locale for the current system.
         */
        public fun getDefault(): Locale
    }
}
