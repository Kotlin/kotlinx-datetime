/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

internal fun Throwable.isJodaArithmeticException(): Boolean = hasJsExceptionName("ArithmeticException")
internal fun Throwable.isJodaDateTimeException(): Boolean = hasJsExceptionName("DateTimeException")
internal fun Throwable.isJodaDateTimeParseException(): Boolean = hasJsExceptionName("DateTimeParseException")

internal expect fun Throwable.hasJsExceptionName(name: String): Boolean

internal expect inline fun <reified T : Any> jsTry(crossinline body: () -> T): T