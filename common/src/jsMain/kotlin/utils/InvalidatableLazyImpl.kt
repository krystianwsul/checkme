package com.krystianwsul.common.utils

import kotlin.jvm.Volatile
import kotlin.reflect.KProperty

private object UNINITIALIZED_VALUE

@Suppress("UNCHECKED_CAST")
actual open class InvalidatableLazyImpl<T> actual constructor(
        private val initializer: () -> T,
        lock: Any?
) : Lazy<T>, Serializable {

    @Volatile
    private var _value: Any? = UNINITIALIZED_VALUE
    private val lock = lock ?: this

    actual open fun invalidate() {
        _value = UNINITIALIZED_VALUE
    }

    override val value: T
        get() {
            val v1 = _value
            if (v1 !== UNINITIALIZED_VALUE) {
                return v1 as T
            }

            return synchronized(lock) {
                val v2 = _value
                if (v2 !== UNINITIALIZED_VALUE) {
                    v2 as T
                } else {
                    val typedValue = initializer()
                    _value = typedValue
                    typedValue
                }
            }
        }


    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    actual operator fun setValue(any: Any, property: KProperty<*>, t: T) {
        _value = t
    }

    actual fun addTo(invalidatableLazyImplCallbacks: InvalidatableLazyImplCallbacks<*>): InvalidatableLazyImpl<T> {
        invalidatableLazyImplCallbacks.addCallback { invalidate() }

        return this
    }
}