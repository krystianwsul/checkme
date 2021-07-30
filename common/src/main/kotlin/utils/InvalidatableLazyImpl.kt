package com.krystianwsul.common.utils

import kotlin.reflect.KProperty

expect open class InvalidatableLazyImpl<T>(initializer: () -> T, lock: Any? = null) : Lazy<T>, Serializable {

    open fun invalidate()

    operator fun setValue(any: Any, property: KProperty<*>, t: T)

    fun addTo(invalidatableLazyImplCallbacks: InvalidatableLazyImplCallbacks<*>): InvalidatableLazyImpl<T>
}

fun <T> invalidatableLazy(initializer: () -> T) = InvalidatableLazyImpl(initializer)