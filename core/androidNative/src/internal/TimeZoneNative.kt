/*
 * Copyright 2019-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.datetime.internal

import kotlinx.cinterop.*
import kotlinx.datetime.TimeZoneIdProvider
import kotlinx.datetime.TimeZone
import kotlinx.datetime.TimeZoneDatabase
import platform.posix.*

private fun readSystemProperty(name: String): String? = memScoped {
    // see https://android.googlesource.com/platform/bionic/+/froyo/libc/include/sys/system_properties.h
    val result = allocArray<ByteVar>(92)
    val error = __system_property_get(name, result)
    if (error == 0) null else result.toKString()
}

internal actual fun currentSystemDefaultTimeZone(): TimeZone =
    systemTimezoneDatabase.get(systemTimeZoneIdProvider.currentTimeZoneId())

internal actual val timeZoneDatabaseImpl: TimeZoneDatabase = tryInitializeTimezoneDatabase { TzdbBionic() }

internal actual val systemTimeZoneIdProvider: TimeZoneIdProvider = object: TimeZoneIdProvider {
    override fun currentTimeZoneId(): String = memScoped {
        readSystemProperty("persist.sys.timezone")
            ?: throw IllegalStateException("The system property 'persist.sys.timezone' should contain the system timezone")
    }
}
