/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

/**
 * Thrown by datetime arithmetic operations if the result cannot be computed or represented.
 */
public class DateTimeArithmeticException: RuntimeException {
    public constructor(): super()
    public constructor(message: String): super(message)
    public constructor(cause: Throwable): super(cause)
    public constructor(message: String, cause: Throwable): super(message, cause)
}

/**
 * Thrown when attempting to construct a [TimeZone] with an invalid ID or unavailable rules.
 */
public class IllegalTimeZoneException: IllegalArgumentException {
    public constructor(): super()
    public constructor(message: String): super(message)
    public constructor(cause: Throwable): super(cause)
    public constructor(message: String, cause: Throwable): super(message, cause)
}

internal class DateTimeFormatException: IllegalArgumentException {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(cause: Throwable): super(cause)
    constructor(message: String, cause: Throwable): super(message, cause)
}
