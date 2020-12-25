package com.krystianwsul.common.utils

class InvalidatableLazyImplCallbacks<T>(initializer: () -> T) : InvalidatableLazyImpl<T>(initializer) {

    private val callbacks = mutableSetOf<WeakReference<() -> Unit>>()

    override fun invalidate() {
        super.invalidate()

        val remove = callbacks.filter { it.get() == null }
        callbacks.removeAll(remove)

        callbacks.forEach { it.get()!!() }
    }

    fun addCallback(callback: () -> Unit) = callbacks.add(WeakReference(callback))

    fun removeCallback(callback: () -> Unit) {
        val item = callbacks.single { it.get() == callback }
        callbacks.remove(item)
    }
}

fun <T> invalidatableLazyCallbacks(initializer: () -> T) = InvalidatableLazyImplCallbacks(initializer)