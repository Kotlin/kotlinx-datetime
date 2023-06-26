/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*

/**
 * A serializer for [DayOfWeek] that represents the values as strings.
 *
 * JSON example: `"MONDAY"`
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public object DayOfWeekSerializer: KSerializer<DayOfWeek> {
    private val impl = EnumSerializer("Month", DayOfWeek.values())

    override val descriptor: SerialDescriptor
        get() = impl.descriptor

    override fun deserialize(decoder: Decoder): DayOfWeek = impl.deserialize(decoder)

    override fun serialize(encoder: Encoder, value: DayOfWeek): Unit = impl.serialize(encoder, value)
}
