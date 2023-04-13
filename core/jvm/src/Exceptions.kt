/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime


/**
 * [kotlinx.datetime.DateTimeUnit] includes some units that are not supported by the underlying platform.
 * This exception is thrown when an unsupported unit is used.
 *
 */
public class UnsupportedDateTimeUnitException: RuntimeException {
    public constructor(): super()
    public constructor(message: String): super(message)
    public constructor(cause: Throwable): super(cause)
    public constructor(message: String, cause: Throwable): super(message, cause)
}