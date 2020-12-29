package com.krystianwsul.common.utils

class InvalidatableLazyImplCallbacks<T>(initializer: () -> T) : InvalidatableLazyImpl<T>(initializer) {

    private val callbacks = mutableSetOf<() -> Unit>()

    override fun invalidate() {
        super.invalidate()

        callbacks.toMutableList().forEach { it() }
    }

    fun addCallback(callback: () -> Unit) = callback.also { callbacks.add(it) }

    fun removeCallback(callback: () -> Unit) {
        check(callbacks.remove(callback))
    }
}

fun <T> invalidatableLazyCallbacks(initializer: () -> T) = InvalidatableLazyImplCallbacks(initializer)