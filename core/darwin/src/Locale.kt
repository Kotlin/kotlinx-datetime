/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

/**
 * Platform-specific locale implementation for Darwin platforms (iOS, macOS, watchOS, tvOS).
 * Wraps [NSLocale].
 */
public actual class Locale internal constructor(internal val value: NSLocale) {
    public actual companion object {
        /**
         * Returns the default locale for the current system.
         */
        public actual fun getDefault(): Locale = Locale(NSLocale.currentLocale)
    }
}

/**
 * Converts this [NSLocale] to a [Locale].
 */
public fun NSLocale.toKotlinLocale(): Locale = Locale(this)

/**
 * Converts this [Locale] to an [NSLocale].
 */
public fun Locale.toNSLocale(): NSLocale = this.value
