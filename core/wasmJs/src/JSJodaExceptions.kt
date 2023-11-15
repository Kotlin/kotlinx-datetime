/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime


private fun checkExceptionName(exception: JsAny, name: String): Boolean =
    js("exception.name === name")

private fun checkForName(exception: Throwable, name: String): Boolean {
    val cause = (exception as? JsException)?.jsException ?: return false
    return checkExceptionName(cause, name)
}

internal actual fun Throwable.isJodaArithmeticException(): Boolean = checkForName(this, "ArithmeticException")
internal actual fun Throwable.isJodaDateTimeException(): Boolean = checkForName(this, "DateTimeException")
internal actual fun Throwable.isJodaDateTimeParseException(): Boolean = checkForName(this, "DateTimeParseException")

private fun withCaughtJsException(body: () -> Unit): JsAny? = js("""{
    try {
        body();
        return null;
    } catch(e) {
        return e;        
    }
}""")

internal class JsException(val jsException: JsAny): Throwable()

internal actual inline fun <reified T : Any> jsTry(crossinline body: () -> T): T {
    var result: T? = null
    val exception = withCaughtJsException {
        result = body()
    }

    if (exception != null) {
        throw JsException(exception)
    } else {
        return result as T
    }
}