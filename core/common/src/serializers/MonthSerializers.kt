/*
 * Copyright 2019-2021 JetBrains s.r.o.
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
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public object MonthSerializer: KSerializer<Month> {
    private val impl = EnumSerializer("Month", Month.values())

    override val descriptor: SerialDescriptor
        get() = impl.descriptor

    override fun deserialize(decoder: Decoder): Month = impl.deserialize(decoder)

    override fun serialize(encoder: Encoder, value: Month): Unit = impl.serialize(encoder, value)
}
