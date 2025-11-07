/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlin.test.*

class LocaleSamples {

    @Test
    fun usage() {
        // Using the default system locale
        val locale = Locale.getDefault()
        val monday = DayOfWeek.MONDAY

        // Get localized name using default locale
        val displayName = monday.displayName(locale = locale)
        check(displayName.isNotEmpty())
    }
}
