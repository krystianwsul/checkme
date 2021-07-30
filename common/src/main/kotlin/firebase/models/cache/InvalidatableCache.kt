package com.krystianwsul.common.firebase.models.cache

class InvalidatableCache<T>(
    invalidatableManager: ClearableInvalidatableManager,
    private val initializer: (invalidatableCache: InvalidatableCache<T>) -> ValueHolder<T>,
) :
    Lazy<T>, Invalidatable {

    private var valueHolder: ValueHolder<T>? = null

    private var initializing = false

    override val value: T
        get() {
            valueHolder?.let { return it.value }

            if (initializing) throw IllegalStateException()
            initializing = true

            return try {
                valueHolder = initializer(this)

                valueHolder!!.value
            } finally {
                initializing = false
            }
        }

    val invalidatableManager = InvalidatableManager()

    init {
        invalidatableManager.addInvalidatable(this)
    }

    override fun isInitialized(): Boolean = valueHolder != null

    override fun invalidate() {
        invalidatableManager.invalidate()

        val currentValueHolder = valueHolder ?: return

        currentValueHolder.teardown()

        valueHolder = null
    }

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    class ValueHolder<T>(val value: T, val teardown: () -> Unit)
}

fun <T> invalidatableCache(
    clearableInvalidatableManager: ClearableInvalidatableManager,
    initializer: (invalidatableCache: InvalidatableCache<T>) -> InvalidatableCache.ValueHolder<T>,
) = InvalidatableCache(clearableInvalidatableManager, initializer)