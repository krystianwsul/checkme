package com.krystianwsul.common.firebase.models.cache

private object UNINITIALIZED_VALUE

class InvalidatableCache<T>(
    private val initializer: () -> T,
    private val setup: (property: InvalidatableCache<T>, value: T) -> Unit,
    private val teardown: (property: InvalidatableCache<T>, value: T) -> Unit,
) : Lazy<T>, Invalidatable {

    private var _value: Any? = UNINITIALIZED_VALUE

    private var initializing = false

    override val value: T
        get() {
            val v1 = _value

            if (v1 !== UNINITIALIZED_VALUE) return getTypedValue()

            if (initializing) throw IllegalStateException()
            initializing = true

            return try {
                val typedValue = initializer()
                _value = typedValue

                setup(this, typedValue)

                typedValue
            } finally {
                initializing = false
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun getTypedValue() = _value as T

    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun invalidate() {
        if (!isInitialized()) return

        teardown(this, getTypedValue())

        _value = UNINITIALIZED_VALUE
    }

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}

fun <T> invalidatableCache(
    initializer: () -> T,
    setup: (property: InvalidatableCache<T>, value: T) -> Unit = { _, _ -> },
    teardown: (property: InvalidatableCache<T>, value: T) -> Unit = { _, _ -> },
    rootCacheCoordinator: RootCacheCoordinator? = null,
) = InvalidatableCache(initializer, setup, teardown).also { property ->
    rootCacheCoordinator?.let { it.invalidatables += property }
}