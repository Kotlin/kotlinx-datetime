/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

internal actual fun Throwable.isJodaArithmeticException(): Boolean = this.asDynamic().name == "ArithmeticException"
internal actual fun Throwable.isJodaDateTimeException(): Boolean = this.asDynamic().name == "DateTimeException"
internal actual fun Throwable.isJodaDateTimeParseException(): Boolean = this.asDynamic().name == "DateTimeParseException"

internal actual inline fun <reified T : Any> jsTry(crossinline body: () -> T): T = body()