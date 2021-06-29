package com.krystianwsul.common.firebase.models.cache

open class InvalidatableManager : Invalidatable {

    protected val invalidatables = mutableSetOf<Invalidatable>()

    override fun invalidate() {
        invalidatables.forEach { it.invalidate() }
    }

    open fun addInvalidatable(invalidatable: Invalidatable): Removable {
        check(invalidatable !in invalidatables)

        invalidatables += invalidatable

        return Removable { removeInvalidatable(invalidatable) }
    }

    open fun removeInvalidatable(invalidatable: Invalidatable) {
        check(invalidatable in invalidatables)

        invalidatables -= invalidatable
    }
}