package com.krystianwsul.common.firebase.models.cache

open class InvalidatableManager : Invalidatable {

    protected val invalidatables = mutableListOf<Invalidatable>()

    override fun invalidate() {
        invalidatables.toMutableList().forEach { it.invalidate() }
    }

    open fun addInvalidatable(invalidatable: Invalidatable): Removable {
        invalidatables += invalidatable

        return Removable { removeInvalidatable(invalidatable) }
    }

    open fun removeInvalidatable(invalidatable: Invalidatable) {
        check(invalidatable in invalidatables)

        invalidatables -= invalidatable
    }
}