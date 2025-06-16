/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/*
 * Based on the bionic project.
 * Copyright (C) 2017 The Android Open Source Project
 */

package kotlinx.datetime.internal

private class TzdbBionic(private val rules: Map<String, Entry>) : TimeZoneDatabase {
    override fun rulesForId(id: String): TimeZoneRulesCommon =
        rules[id]?.readRules() ?: throw IllegalStateException("Unknown time zone $id")

    override fun availableTimeZoneIds(): Set<String> = rules.keys

    class Entry(val file: ByteArray, val offset: Int, val length: Int) {
        fun readRules(): TimeZoneRulesCommon = readTzFile(file.copyOfRange(offset, offset + length)).toTimeZoneRules()
    }
}

// see https://android.googlesource.com/platform/bionic/+/master/libc/tzcode/bionic.cpp for the format
internal fun TzdbBionic(): TimeZoneDatabase = TzdbBionic(buildMap<String, TzdbBionic.Entry> {
    for (path in listOf(
        Path.fromString("/system/usr/share/zoneinfo/tzdata"), // immutable fallback tzdb
        Path.fromString("/apex/com.android.tzdata/etc/tz/tzdata"), // an up-to-date tzdb, may not exist
    )) {
        // be careful to only read each file a single time and keep many references to the same ByteArray in memory.
        val content = path.readBytes() ?: continue // this file does not exist
        val header = BionicTzdbHeader.parse(content)
        val indexSize = header.data_offset - header.index_offset
        check(indexSize % 52 == 0) { "Invalid index size: $indexSize (must be a multiple of 52)" }
        val reader = BinaryDataReader(content, header.index_offset)
        repeat(indexSize / 52) {
            val name = reader.readNullTerminatedUtf8String(40)
            val start = reader.readInt()
            val length = reader.readInt()
            reader.readInt() // unused
            // intentionally overwrite the older entries
            put(name, TzdbBionic.Entry(content, header.data_offset + start, length))
        }
    }
})

// bionic_tzdata_header_t
private class BionicTzdbHeader(
    val version: String,
    val index_offset: Int,
    val data_offset: Int,
    val final_offset: Int,
) {
    override fun toString(): String =
        "BionicTzdbHeader(version='$version', index_offset=$index_offset, " +
            "data_offset=$data_offset, final_offset=$final_offset)"

    companion object {
        fun parse(content: ByteArray): BionicTzdbHeader =
            with(BinaryDataReader(content)) {
                BionicTzdbHeader(
                    version = readNullTerminatedUtf8String(12),
                    index_offset = readInt(),
                    data_offset = readInt(),
                    final_offset = readInt(),
                )
            }.apply {
                check(version.startsWith("tzdata") && version.length < 12) { "Unknown tzdata version: $version" }
                check(index_offset <= data_offset) { "Invalid data and index offsets: $data_offset and $index_offset" }
            }
    }
}
