/*
 * Copyright 2019-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*

class LocalDateRangeTest {
    val Dec_24_1900 = LocalDate(1900, 12, 24)
    val Dec_30_1999 = LocalDate(1999, 12, 30)
    val Jan_01_2000 = LocalDate(2000, 1, 1)
    val Jan_02_2000 = LocalDate(2000, 1, 2)
    val Jan_05_2000 = LocalDate(2000, 1, 5)
    val Jan_24_2000 = LocalDate(2000, 1, 24)
    val Dec_24_2000 = LocalDate(2000, 12, 24)

    @Test
    fun emptyRange() {
        assertTrue { (Jan_05_2000..Jan_01_2000).isEmpty() }
        assertTrue { (Jan_01_2000 downTo Jan_05_2000).isEmpty() }
        assertTrue { LocalDateRange.EMPTY.isEmpty() }
    }

    @Test
    fun forwardRange() {
        assertContentEquals(
            (1..5).map { LocalDate(2000, 1, it) },
            Jan_01_2000..Jan_05_2000
        )
        assertContentEquals(
            listOf(Jan_01_2000),
            Jan_01_2000..Jan_01_2000
        )
        assertContentEquals(
            listOf(
                LocalDate(1999, 12, 30),
                LocalDate(1999, 12, 31),
                LocalDate(2000, 1, 1),
                LocalDate(2000, 1, 2)
            ),
            Dec_30_1999..Jan_02_2000
        )
    }

    @Test
    fun backwardRange() {
        assertContentEquals(
            (5 downTo 1).map { LocalDate(2000, 1, it) },
            Jan_05_2000 downTo Jan_01_2000
        )
        assertContentEquals(
            listOf(Jan_01_2000),
            Jan_01_2000 downTo Jan_01_2000
        )
        assertContentEquals(
            listOf(
                LocalDate(2000, 1, 2),
                LocalDate(2000, 1, 1),
                LocalDate(1999, 12, 31),
                LocalDate(1999, 12, 30),
            ),
            Jan_02_2000 downTo Dec_30_1999
        )
    }

    @Test
    fun step() {
        assertContentEquals(
            (1900..2000).map { LocalDate(it, 12, 24) },
            (Dec_24_1900..Dec_24_2000 step DatePeriod(years = 1))
        )
        assertContentEquals(
            (1..12).map { LocalDate(2000, it, 24) },
            (Jan_24_2000..Dec_24_2000 step DatePeriod(months = 1))
        )
        assertContentEquals(
            (1..12).map { LocalDate(2000, it, it * 2) },
            (Jan_02_2000..Dec_24_2000 step DatePeriod(months = 1, days = 2))
        )
    }

    @Test
    fun invalidSteps() {
        val range = Dec_24_1900..Jan_01_2000
        val downRange = Jan_01_2000 downTo Dec_24_1900

        listOf(
            0 to 0,
            4800 to -146097
        ).map {
            DatePeriod(months = it.first, days = it.second)
        }.forEach {
            assertFalse { it.positive() }
            assertFalse { it.negative() }
            assertFailsWith<IllegalArgumentException> { range step it }
            assertFailsWith<IllegalArgumentException> { downRange step it }
        }

        listOf(
            1 to -28,
            1 to -29,
            1 to -30,
            1 to -31,
            2 to -59,
            2 to -60,
            2 to -61,
            2 to -62,
            3 to -89,
            3 to -90,
            3 to -91,
            3 to -92,
            4 to -120,
            4 to -121,
            4 to -122,
            4 to -123,
            5 to -150,
            5 to -151,
            5 to -152,
            5 to -153,
            6 to -181,
            6 to -182,
            6 to -183,
            6 to -184,
            7 to -212,
            7 to -213,
            7 to -214,
            7 to -215,
            8 to -242,
            8 to -243,
            8 to -244,
            8 to -245,
            9 to -273,
            9 to -274,
            9 to -275,
            9 to -276,
            10 to -303,
            10 to -304,
            10 to -305,
            10 to -306,
            11 to -334,
            11 to -335,
            11 to -336,
            11 to -337,
            12 to -365,
            12 to -366,
            13 to -393,
            13 to -394,
            13 to -395,
            13 to -396,
            13 to -397,
            14 to -424,
            14 to -425,
            14 to -426,
            14 to -427,
            14 to -428,
            15 to -454,
            15 to -455,
            15 to -456,
            15 to -457,
            15 to -458,
            16 to -485,
            16 to -486,
            16 to -487,
            16 to -488,
            16 to -489,
            17 to -515,
            17 to -516,
            17 to -517,
            17 to -518,
            17 to -519,
            18 to -546,
            18 to -547,
            18 to -548,
            18 to -549,
            18 to -550,
            19 to -577,
            19 to -578,
            19 to -579,
            19 to -580,
            19 to -581,
            20 to -607,
            20 to -608,
            20 to -609,
            20 to -610,
            20 to -611,
            21 to -638,
            21 to -639,
            21 to -640,
            21 to -641,
            21 to -642,
            22 to -668,
            22 to -669,
            22 to -670,
            22 to -671,
            22 to -672,
            23 to -699,
            23 to -700,
            23 to -701,
            23 to -702,
            23 to -703,
            24 to -730,
            24 to -731,
            25 to -758,
            25 to -759,
            25 to -760,
            25 to -761,
            25 to -762,
            26 to -789,
            26 to -790,
            26 to -791,
            26 to -792,
            26 to -793,
            27 to -819,
            27 to -820,
            27 to -821,
            27 to -822,
            27 to -823,
            28 to -850,
            28 to -851,
            28 to -852,
            28 to -853,
            28 to -854,
            29 to -880,
            29 to -881,
            29 to -882,
            29 to -883,
            29 to -884,
            30 to -911,
            30 to -912,
            30 to -913,
            30 to -914,
            30 to -915,
            31 to -942,
            31 to -943,
            31 to -944,
            31 to -945,
            31 to -946,
            32 to -972,
            32 to -973,
            32 to -974,
            32 to -975,
            32 to -976,
            33 to -1003,
            33 to -1004,
            33 to -1005,
            33 to -1006,
            33 to -1007,
            34 to -1033,
            34 to -1034,
            34 to -1035,
            34 to -1036,
            34 to -1037,
            35 to -1064,
            35 to -1065,
            35 to -1066,
            35 to -1067,
            35 to -1068,
            36 to -1095,
            36 to -1096,
            37 to -1124,
            37 to -1125,
            37 to -1126,
            37 to -1127,
            38 to -1155,
            38 to -1156,
            38 to -1157,
            38 to -1158,
            39 to -1185,
            39 to -1186,
            39 to -1187,
            39 to -1188,
            40 to -1216,
            40 to -1217,
            40 to -1218,
            40 to -1219,
            41 to -1246,
            41 to -1247,
            41 to -1248,
            41 to -1249,
            42 to -1277,
            42 to -1278,
            42 to -1279,
            42 to -1280,
            43 to -1308,
            43 to -1309,
            43 to -1310,
            43 to -1311,
            44 to -1338,
            44 to -1339,
            44 to -1340,
            44 to -1341,
            45 to -1369,
            45 to -1370,
            45 to -1371,
            45 to -1372,
            46 to -1399,
            46 to -1400,
            46 to -1401,
            46 to -1402,
            47 to -1430,
            47 to -1431,
            47 to -1432,
            47 to -1433,
            48 to -1461
        ).map {
            DatePeriod(months = it.first, days = it.second)
        }.flatMap{
            listOf(it, -it)
        }.forEach {
            val step = when {
                it.positive() -> it
                else -> -it
            }
            assertFailsWith<IllegalStateException> { (range step step).toList() }
            assertFailsWith<IllegalStateException> { (downRange step -step).toList() }
        }
    }

    @Test
    fun string() {
        assertEquals(
            "2000-01-01..2000-01-05",
            (Jan_01_2000..Jan_05_2000).toString()
        )
        assertEquals(
            "2000-01-05 downTo 2000-01-01 step -P1D",
            (Jan_05_2000 downTo Jan_01_2000).toString()
        )
        assertEquals(
            "2000-01-01..2000-01-05 step P1D",
            LocalDateProgression.fromClosedRange(Jan_01_2000, Jan_05_2000, DatePeriod(days=1)).toString()
        )
        assertEquals(
            "2000-01-05 downTo 2000-01-01 step -P1D",
            LocalDateProgression.fromClosedRange(Jan_05_2000, Jan_01_2000, DatePeriod(days=-1)).toString()
        )
    }
}
