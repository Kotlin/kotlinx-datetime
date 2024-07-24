/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

internal fun Char.asciiDigitToInt(): Int = this - '0'
