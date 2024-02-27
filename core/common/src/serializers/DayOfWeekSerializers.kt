/*
 * Copyright 2019-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*

/**
 * A serializer for [DayOfWeek] that represents the values as strings.
 *
 * JSON example: `"MONDAY"`
 */
@Suppress("EnumValuesSoftDeprecate") // createEnumSerializer requires an array
public object DayOfWeekSerializer : KSerializer<DayOfWeek> by createEnumSerializer<DayOfWeek>(
    "kotlinx.datetime.DayOfWeek",
    DayOfWeek.values()
)
