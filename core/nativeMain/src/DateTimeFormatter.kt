/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

object DateTimeFormatter {

    val ISO_LOCAL_DATE = 0

    /*
    val ISO_LOCAL_DATE = DateTimeFormatterBuilder()
        .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
        .appendLiteral('-')
        .appendValue(MONTH_OF_YEAR, 2)
        .appendLiteral('-')
        .appendValue(DAY_OF_MONTH, 2)
        .toFormatter(ResolverStyle.STRICT).withChronology(IsoChronology.INSTANCE);

    val ISO_LOCAL_DATE_TIME = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(ISO_LOCAL_DATE)
        .appendLiteral('T')
        .append(ISO_LOCAL_TIME)
        .toFormatter(ResolverStyle.STRICT).withChronology(IsoChronology.INSTANCE);

    val ISO_LOCAL_TIME = DateTimeFormatterBuilder()
        .appendValue(HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(MINUTE_OF_HOUR, 2)
        .optionalStart()
        .appendLiteral(':')
        .appendValue(SECOND_OF_MINUTE, 2)
        .optionalStart()
        .appendFraction(NANO_OF_SECOND, 0, 9, true)
        .toFormatter(ResolverStyle.STRICT);

    val ISO_INSTANT = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendInstant()
        .toFormatter(ResolverStyle.STRICT);
     */

}
