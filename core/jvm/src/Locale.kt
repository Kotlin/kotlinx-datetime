/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

/**
 * Platform-specific locale implementation for JVM.
 * Wraps [java.util.Locale].
 */
public actual class Locale internal constructor(internal val value: java.util.Locale) {
    public actual companion object {
        /**
         * Returns the default locale for the current system.
         */
        public actual fun getDefault(): Locale = Locale(java.util.Locale.getDefault())
    }
}

/**
 * Converts this [java.util.Locale] to a [Locale].
 */
public fun java.util.Locale.toKotlinLocale(): Locale = Locale(this)

/**
 * Converts this [Locale] to a [java.util.Locale].
 */
public fun Locale.toJavaLocale(): java.util.Locale = this.value
