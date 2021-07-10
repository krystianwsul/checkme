package com.krystianwsul.common.firebase.models.cache

class ClearableInvalidatableManager : InvalidatableManager() {

    private var cleared = false

    override fun addInvalidatable(invalidatable: Invalidatable): Removable {
        check(!cleared)

        return super.addInvalidatable(invalidatable)
    }

    override fun removeInvalidatable(invalidatable: Invalidatable) {
        check(!cleared)

        super.removeInvalidatable(invalidatable)
    }

    fun clear() {
        check(!cleared)

        invalidate()
        invalidatables.clear()

        cleared = true
    }
}