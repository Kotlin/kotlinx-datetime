/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal inline fun <A, T: Number> A.onNonZero(value: T, block: (T) -> A): A = if (value != 0) block(value) else this
