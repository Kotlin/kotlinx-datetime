/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.cinterop.*
import kotlinx.datetime.internal.*
import platform.posix.*
import kotlin.test.*

class TimeZoneRulesCompleteTest {
    @Test
    fun iterateOverAllTimezones() {
        val root = Path.fromString("/usr/share/zoneinfo")
        val tzdb = TzdbOnFilesystem(root)
        for (id in tzdb.availableTimeZoneIds()) {
            val file = root.resolve(Path.fromString(id))
            val rules = tzdb.rulesForId(id)
            runUnixCommand("zdump -v $file").windowed(size = 2, step = 2).forEach {
                print(it)
            }
        }
    }
}

private inline fun runUnixCommand(command: String): Sequence<String> = sequence {
    val pipe = popen(command, "r") ?: error("Failed to run command: $command")
    try {
        memScoped {
            // read line by line
            while (true) {
                val linePtr = alloc<CPointerVar<ByteVar>>()
                val nPtr = alloc<ULongVar>()
                try {
                    val result = getline(linePtr.ptr, nPtr.ptr, pipe)
                    if (result != (-1).convert<ssize_t>()) {
                        yield(linePtr.value!!.toKString())
                    } else {
                        break
                    }
                } finally {
                    free(linePtr.value)
                }
            }
        }
    } finally {
        pclose(pipe)
    }
}
