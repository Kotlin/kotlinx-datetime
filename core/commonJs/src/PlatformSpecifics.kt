/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

// a pair of ZoneRulesProvider.asDynamic().getTzdbData().zones and ZoneRulesProvider.asDynamic().getTzdbData().links
internal expect fun readTzdb(): Pair<List<String>, List<String>>?

public expect interface InteropInterface

@OptIn(ExperimentalMultiplatform::class)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FILE)
@OptionalExpectation
public expect annotation class JsNonModule()

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FILE)
public expect annotation class JsModule(val import: String)
