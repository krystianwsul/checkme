package com.krystianwsul.common.firebase.models.cache

class RootCacheCoordinator : Invalidatable {

    val invalidatables = mutableSetOf<Invalidatable>()

    private var cleared = false

    override fun invalidate() {
        invalidatables.forEach { it.invalidate() }
    }

    fun clear() {
        check(!cleared)

        invalidate()
        invalidatables.clear()

        cleared = true
    }
}