/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.serialization.*
import kotlinx.datetime.internal.JSJoda.DayOfWeek as jsDayOfWeek

@Serializable
public actual enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;
}

internal fun jsDayOfWeek.toDayOfWeek(): DayOfWeek = DayOfWeek(this.value().toInt())