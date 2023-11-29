/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

private fun checkExceptionName(exception: JsAny, name: String): Boolean =
    js("exception.name === name")

internal actual fun Throwable.hasJsExceptionName(name: String): Boolean {
    val cause = (this as? JsException)?.jsException ?: return false
    return checkExceptionName(cause, name)
}

private fun withCaughtJsException(body: () -> Unit): JsAny? = js("""{
    try {
        body();
        return null;
    } catch(e) {
        return e;
    }
}""")

private fun getExceptionMessage(jsException: JsAny): String? = js("jsException.message")

internal class JsException(val jsException: JsAny): Throwable() {
    override val message: String?
        get() = getExceptionMessage(jsException)
}

internal inline fun <reified T : Any> jsTry(crossinline body: () -> T): T {
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