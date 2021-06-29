package com.krystianwsul.common.firebase.models.cache

class RootCacheCoordinator : Invalidatable {

    val invalidatables = mutableSetOf<Invalidatable>()

    private var cleared = false

    override fun invalidate() {
        invalidatables.forEach { it.invalidate() }
    }

    fun addInvalidatable(invalidatable: Invalidatable): Invalidatable {
        check(!cleared)
        check(invalidatable !in invalidatables)

        invalidatables += invalidatable

        return invalidatable
    }

    fun removeInvalidatable(invalidatable: Invalidatable) {
        check(!cleared)
        check(invalidatable in invalidatables)

        invalidatables -= invalidatable
    }

    fun clear() {
        check(!cleared)

        invalidate()
        invalidatables.clear()

        cleared = true
    }
}