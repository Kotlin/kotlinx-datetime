/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

/**
 * Platform-specific locale implementation for platforms without native locale support.
 * Uses a simple string-based representation.
 */
public actual class Locale internal constructor(internal val localeString: String) {
    public actual companion object {
        /**
         * Returns the default locale (English) for platforms without locale detection.
         */
        public actual fun getDefault(): Locale = Locale("en")
    }
}
