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
 * A serializer for [Month] that represents the month value as a string.
 *
 * JSON example: `"JANUARY"`
 */
@Suppress("EnumValuesSoftDeprecate") // createEnumSerializer requires an array
public object MonthSerializer : KSerializer<Month> by createEnumSerializer<Month>(
    "kotlinx.datetime.Month",
    Month.values())

// Until https://github.com/Kotlin/kotlinx.serialization/issues/2459 is resolved
internal fun <E : Enum<E>> createEnumSerializer(serialName: String, values: Array<E>): KSerializer<E> {
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    return kotlinx.serialization.internal.EnumSerializer(serialName, values)
}
