/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.native.concurrent.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

object MonthIntSerializer: KSerializer<Month> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Month", PrimitiveKind.SHORT)

    override fun deserialize(decoder: Decoder): Month = Month(decoder.decodeShort().toInt())

    override fun serialize(encoder: Encoder, value: Month) {
        encoder.encodeShort(value.number.toShort())
    }
}

public expect enum class Month {
    JANUARY,
    FEBRUARY,
    MARCH,
    APRIL,
    MAY,
    JUNE,
    JULY,
    AUGUST,
    SEPTEMBER,
    OCTOBER,
    NOVEMBER,
    DECEMBER;
//    val value: Int // member missing in java.time.Month has to be an extension
}

public val Month.number: Int get() = ordinal + 1

@SharedImmutable
private val allMonths = Month.values().asList()

public fun Month(number: Int): Month {
    require(number in 1..12)
    return allMonths[number - 1]
}


// companion object members vs typealiasing to java.time.Month?
