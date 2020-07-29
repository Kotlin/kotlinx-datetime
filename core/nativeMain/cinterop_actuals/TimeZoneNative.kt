/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

internal actual fun available_zone_ids(): kotlinx.cinterop.CPointer<kotlinx.cinterop.CPointerVar<kotlinx.cinterop.ByteVar>>? =
        kotlinx.datetime.internal.available_zone_ids()

internal actual fun get_system_timezone(id: kotlinx.cinterop.CValuesRef<kotlinx.datetime.TZIDVar /* = kotlinx.cinterop.ULongVarOf<kotlin.ULong> */>?): kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar /* = kotlinx.cinterop.ByteVarOf<kotlin.Byte> */>? =
        kotlinx.datetime.internal.get_system_timezone(id)

internal actual fun offset_at_datetime(zone: kotlinx.datetime.TZID /* = kotlin.ULong */, epoch_sec: platform.posix.int64_t /* = kotlin.Long */, offset: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.IntVar /* = kotlinx.cinterop.IntVarOf<kotlin.Int> */>?): kotlin.Int =
        kotlinx.datetime.internal.offset_at_datetime(zone, epoch_sec, offset)

internal actual fun offset_at_instant(zone: kotlinx.datetime.TZID /* = kotlin.ULong */, epoch_sec: platform.posix.int64_t /* = kotlin.Long */): kotlin.Int =
        kotlinx.datetime.internal.offset_at_instant(zone, epoch_sec)

internal actual fun timezone_by_name(zone_name: kotlin.String?): kotlinx.datetime.TZID /* = kotlin.ULong */ =
        kotlinx.datetime.internal.timezone_by_name(zone_name)

internal actual val TZID_INVALID: TZID
    get() = kotlinx.datetime.internal.TZID_INVALID
