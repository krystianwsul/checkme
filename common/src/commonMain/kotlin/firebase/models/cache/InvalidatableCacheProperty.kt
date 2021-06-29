package com.krystianwsul.common.firebase.models.cache

import kotlin.reflect.KProperty

private object UNINITIALIZED_VALUE

class InvalidatableCacheProperty<T>(
    private val initializer: () -> T,
) : Lazy<T>, Invalidatable {

    private var _value: Any? = UNINITIALIZED_VALUE

    private var initializing = false

    override val value: T
        get() {
            val v1 = _value

            @Suppress("UNCHECKED_CAST")
            if (v1 !== UNINITIALIZED_VALUE) return v1 as T

            if (initializing) throw IllegalStateException()
            initializing = true

            return try {
                val typedValue = initializer()
                _value = typedValue
                typedValue
            } finally {
                initializing = false
            }
        }

    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun invalidate() {
        _value = UNINITIALIZED_VALUE
    }

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    operator fun setValue(any: Any, property: KProperty<*>, t: T) {
        _value = t
    }
}

fun <T> invalidatableCacheProperty(initializer: () -> T) = InvalidatableCacheProperty(initializer)