package com.krystianwsul.common.utils

import kotlin.reflect.KProperty

expect class InvalidatableLazyImpl<T>(initializer: () -> T, lock: Any? = null) : Lazy<T>, Serializable {

    fun invalidate()

    operator fun setValue(any: Any, property: KProperty<*>, t: T)
}

fun <T> invalidatableLazy(initializer: () -> T) = InvalidatableLazyImpl(initializer)