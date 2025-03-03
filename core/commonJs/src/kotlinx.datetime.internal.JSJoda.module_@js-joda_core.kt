@file:kotlinx.datetime.internal.JsModule("@js-joda/core")
@file:kotlinx.datetime.internal.JsNonModule
package kotlinx.datetime.internal.JSJoda

import kotlinx.datetime.internal.InteropInterface

internal external class ZoneId : InteropInterface {
    fun id(): String

    companion object {
        fun systemDefault(): ZoneId
    }
}

internal external object ZoneRulesProvider : InteropInterface
