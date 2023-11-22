/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal expect fun getAvailableZoneIdsSet(): Set<String>

public expect interface InteropInterface

@OptIn(ExperimentalMultiplatform::class)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FILE)
@OptionalExpectation
public expect annotation class JsNonModule()