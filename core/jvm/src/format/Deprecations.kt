/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

/**
 * This class preserves backward compatibility with the version 0.6.2 of `kotlinx-datetime`,
 * where `year` and `monthNumber` with default values were parts of the `WithDate` interface.
 *
 * Now, these methods were moved to the `WithYearMonth` interface,
 * but the static methods corresponding to the default values are not inherited.
 */
@PublishedApi
internal class `DateTimeFormatBuilder$WithDate$DefaultImpls` {
    // public static synthetic fun monthNumber$default (Lkotlinx/datetime/format/DateTimeFormatBuilder$WithDate;Lkotlinx/datetime/format/Padding;ILjava/lang/Object;)V
    // public static synthetic fun year$default (Lkotlinx/datetime/format/DateTimeFormatBuilder$WithDate;Lkotlinx/datetime/format/Padding;ILjava/lang/Object;)V
    // public static synthetic fun day$default (Lkotlinx/datetime/format/DateTimeFormatBuilder$WithDate;Lkotlinx/datetime/format/Padding;ILjava/lang/Object;)V
    // public static fun dayOfMonth (Lkotlinx/datetime/format/DateTimeFormatBuilder$WithDate;Lkotlinx/datetime/format/Padding;)V
    // public static synthetic fun dayOfMonth$default (Lkotlinx/datetime/format/DateTimeFormatBuilder$WithDate;Lkotlinx/datetime/format/Padding;ILjava/lang/Object;)V

    companion object {
        @JvmStatic
        fun `monthNumber$default`(format: DateTimeFormatBuilder.WithDate, padding: Padding?, i: Int, j: Any?) {
            format.monthNumber()
        }

        @JvmStatic
        fun `year$default`(format: DateTimeFormatBuilder.WithDate, padding: Padding?, i: Int, j: Any?) {
            format.year()
        }

        @JvmStatic
        fun `day$default`(format: DateTimeFormatBuilder.WithDate, padding: Padding?, i: Int, j: Any?) {
            format.day()
        }

        @JvmStatic
        fun dayOfMonth(format: DateTimeFormatBuilder.WithDate, padding: Padding?) {
            format.day(padding = padding ?: Padding.ZERO)
        }

        @JvmStatic
        fun `dayOfMonth$default`(format: DateTimeFormatBuilder.WithDate, padding: Padding?, i: Int, j: Any?) {
            format.day()
        }
    }
}
