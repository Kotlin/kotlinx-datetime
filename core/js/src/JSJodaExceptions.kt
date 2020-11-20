/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

internal fun Throwable.isJodaArithmeticException(): Boolean = this.asDynamic().name == "ArithmeticException"
internal fun Throwable.isJodaDateTimeException(): Boolean = this.asDynamic().name == "DateTimeException"
internal fun Throwable.isJodaDateTimeParseException(): Boolean = this.asDynamic().name == "DateTimeParseException"
