/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format

internal interface Predicate<in T> {
    fun test(value: T): Boolean
}

internal class BasicPredicate<in T>(
    private val operation: T.() -> Boolean
): Predicate<T> {
    override fun test(value: T): Boolean = value.operation()
}

internal class ComparisonPredicate<in T, E>(
    private val expectedValue: E,
    private val getter: (T) -> E?
): Predicate<T> {
    override fun test(value: T): Boolean = getter(value) == expectedValue
}

internal class ConjunctionPredicate<in T>(
    private val predicates: List<Predicate<T>>
): Predicate<T> {
    override fun test(value: T): Boolean = predicates.all { it.test(value) }
    fun isConstTrue(): Boolean = predicates.isEmpty()
}
