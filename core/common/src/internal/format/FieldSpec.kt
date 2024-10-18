/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format

import kotlinx.datetime.internal.format.parser.AssignableField
import kotlin.reflect.KMutableProperty1

/**
 * A lens-like interface for accessing fields of objects.
 */
internal interface Accessor<in Object, Field>: AssignableField<Object, Field> {
    /**
     * The value of the field in the given object, or `null` if the field is not set.
     */
    fun getter(container: Object): Field?

    /**
     * Function that returns the value of the field in the given object,
     * or throws [IllegalStateException] if the field is not set.
     *
     * This function is used to access fields during formatting.
     */
    fun getterNotNull(container: Object): Field =
        getter(container) ?: throw IllegalStateException("Field $name is not set")
}

/**
 * An implementation of [Accessor] for a mutable property of an object.
 */
internal class PropertyAccessor<Object, Field>(private val property: KMutableProperty1<Object, Field?>): Accessor<Object, Field> {
    override val name: String get() = property.name

    override fun trySetWithoutReassigning(container: Object, newValue: Field): Field? {
        val oldValue = property.get(container)
        return when {
            oldValue === null -> {
                property.set(container, newValue)
                null
            }
            oldValue == newValue -> null
            else -> oldValue
        }
    }

    override fun getter(container: Object): Field? = property.get(container)
}

/**
 * A description of the field's numeric sign.
 *
 * Several fields can share the same sign. For example, the hour and the minute of the UTC offset have the same sign,
 * and setting the sign of the hour also sets the sign of the minute.
 *
 * Implementations of this interface are *not* required to redefine [equals] and [hashCode].
 * Instead, signs should be defined as singletons and compared by reference.
 */
internal interface FieldSign<in Target> {
    /**
     * The field that is `true` if the value of the field is known to be negative, and `false` otherwise.
     * Can be both read from and written to.
     */
    val isNegative: Accessor<Target, Boolean>

    /**
     * A check for whether the current value of the field is zero.
     */
    fun isZero(obj: Target): Boolean
}

/**
 * A specification of a field.
 *
 * Fields represent parts of objects, regardless of how they are stored or formatted.
 * For example, a field "day of week" can be represented with both strings of various kinds ("Monday", "Mon") and
 * numbers ("1" for Monday, "2" for Tuesday, etc.), but the field itself is the same.
 *
 * Fields can typically contain `null` values, which means that the field is not set.
 */
internal interface FieldSpec<in Target, Type> {
    /**
     * The function with which the field can be accessed.
     */
    val accessor: Accessor<Target, Type>

    /**
     * The default value of the field, or `null` if the field has none.
     */
    val defaultValue: Type?

    /**
     * The name of the field.
     */
    val name: String

    /**
     * The sign corresponding to the field value, or `null` if the field has none.
     */
    val sign: FieldSign<Target>?
}

/**
 * Inherit from this class to obtain a sensible [toString] implementation for debugging.
 */
internal abstract class AbstractFieldSpec<in Target, Type>: FieldSpec<Target, Type> {
    override fun toString(): String = "The field $name (default value is $defaultValue)"
}

/**
 * A specification of a field that can contain values of any kind.
 * Used for fields additional information about which is not that important for parsing/formatting.
 */
internal class GenericFieldSpec<in Target, Type>(
    override val accessor: Accessor<Target, Type>,
    override val name: String = accessor.name,
    override val defaultValue: Type? = null,
    override val sign: FieldSign<Target>? = null,
) : AbstractFieldSpec<Target, Type>()

/**
 * A specification of a field that can only contain non-negative numeric values.
 */
internal class UnsignedFieldSpec<in Target>(
    override val accessor: Accessor<Target, Int>,
    /**
     * The minimum value of the field.
     */
    val minValue: Int,
    /**
     * The maximum value of the field.
     */
    val maxValue: Int,
    override val name: String = accessor.name,
    override val defaultValue: Int? = null,
    override val sign: FieldSign<Target>? = null,
) : AbstractFieldSpec<Target, Int>() {
    /**
     * The maximum length of the field when represented as a decimal number.
     */
    val maxDigits: Int = when {
        maxValue < 10 -> 1
        maxValue < 100 -> 2
        maxValue < 1000 -> 3
        else -> throw IllegalArgumentException("Max value $maxValue is too large")
    }
}
