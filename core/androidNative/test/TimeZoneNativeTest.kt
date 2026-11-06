/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime.test

import kotlin.time.Instant

actual val RELIABLE_TRANSITIONS = Instant.DISTANT_PAST..Instant.DISTANT_FUTURE
