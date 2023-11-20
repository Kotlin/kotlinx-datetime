@file:JsModule("@js-joda/core")
@file:kotlinx.datetime.internal.JsNonModule
@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE", "INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
package kotlinx.datetime.internal.JSJoda

import kotlinx.datetime.internal.InteropInterface
import kotlin.js.*

external fun nativeJs(date: Date, zone: ZoneId = definedExternally): TemporalAccessor

external fun nativeJs(date: Date): TemporalAccessor

external fun nativeJs(date: InteropInterface, zone: ZoneId = definedExternally): TemporalAccessor

external fun nativeJs(date: InteropInterface): TemporalAccessor

external interface `T$0` : InteropInterface {
    var toDate: () -> Date
    var toEpochMilli: () -> Double
}

external fun convert(temporal: LocalDate, zone: ZoneId = definedExternally): `T$0`

external fun convert(temporal: LocalDate): `T$0`

external fun convert(temporal: LocalDateTime, zone: ZoneId = definedExternally): `T$0`

external fun convert(temporal: LocalDateTime): `T$0`

external fun convert(temporal: ZonedDateTime, zone: ZoneId = definedExternally): `T$0`

external fun convert(temporal: ZonedDateTime): `T$0`