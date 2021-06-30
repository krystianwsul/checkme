package com.krystianwsul.common.firebase.models.cache

class RootModelChangeManager {

    val existingInstancesInvalidatableManager = InvalidatableManager()

    val rootModelInvalidatableManager = InvalidatableManager()

    fun invalidateExistingInstances() = existingInstancesInvalidatableManager.invalidate()

    fun invalidateRootModels() {
        invalidateExistingInstances()
        rootModelInvalidatableManager.invalidate()
    }
}