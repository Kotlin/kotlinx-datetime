/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

internal expect fun Throwable.isJodaArithmeticException(): Boolean
internal expect fun Throwable.isJodaDateTimeException(): Boolean
internal expect fun Throwable.isJodaDateTimeParseException(): Boolean

internal expect inline fun <reified T : Any> jsTry(crossinline body: () -> T): T