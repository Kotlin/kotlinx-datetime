/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmName("MonthKt")
@file:JvmMultifileClass
package kotlinx.datetime

/**
 * @suppress
 */
@Deprecated("Use kotlinx.datetime.Month", ReplaceWith("toKotlinMonth().number"), level = DeprecationLevel.ERROR)
public val java.time.Month.number: Int get() = toKotlinMonth().number

