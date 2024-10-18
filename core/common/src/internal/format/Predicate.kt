/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format

internal interface Predicate<in T> {
    fun test(value: T): Boolean
}

internal object Truth: Predicate<Any?> {
    override fun test(value: Any?): Boolean = true
}

internal class ComparisonPredicate<in T, E>(
    private val expectedValue: E,
    private val getter: (T) -> E?
): Predicate<T> {
    override fun test(value: T): Boolean = getter(value) == expectedValue
}

private class ConjunctionPredicate<in T>(
    private val predicates: List<Predicate<T>>
): Predicate<T> {
    override fun test(value: T): Boolean = predicates.all { it.test(value) }
}

internal fun<T> conjunctionPredicate(
    predicates: List<Predicate<T>>
): Predicate<T> = when {
    predicates.isEmpty() -> Truth
    predicates.size == 1 -> predicates.single()
    else -> ConjunctionPredicate(predicates)
}
