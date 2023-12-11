/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*

internal class TzdbOnFilesystem(val tzdbPath: Path): TimezoneDatabase {

    override fun rulesForId(id: String): TimeZoneRules =
        readTzFile(tzdbPath.resolve(Path.fromString(id)).readBytes()).toTimeZoneRules()

    override fun availableTimeZoneIds(): Set<String> = buildSet {
        tzdbPath.traverseDirectory(exclude = tzdbUnneededFiles) { add(it.toString()) }
    }

}

private val tzdbUnneededFiles = setOf(
    "posix",
    "posixrules",
    "Factory",
    "iso3166.tab",
    "right",
    "+VERSION",
    "zone.tab",
    "zone1970.tab",
    "tzdata.zi",
    "leapseconds",
    "leap-seconds.list"
)
