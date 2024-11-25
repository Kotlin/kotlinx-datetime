/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:kotlinx.datetime.internal.JsModule("@js-joda/core")
@file:kotlinx.datetime.internal.JsNonModule
package kotlinx.datetime.test.JSJoda

import kotlinx.datetime.internal.InteropInterface

external class ZonedDateTime : InteropInterface {
    fun year(): Int
    fun monthValue(): Int
    fun dayOfMonth(): Int
    fun hour(): Int
    fun minute(): Int
    fun second(): Int
    fun nano(): Double
}

external class Instant : InteropInterface {
    fun atZone(zone: ZoneId): ZonedDateTime
    companion object {
        fun ofEpochMilli(epochMilli: Double): Instant
    }
}

external class ZoneId : InteropInterface {
    companion object {
        fun of(zoneId: String): ZoneId
    }
}
