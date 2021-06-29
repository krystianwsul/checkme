package com.krystianwsul.common.utils

import com.krystianwsul.common.firebase.models.cache.InvalidatableManager

class RootModelChangeManager {

    val existingInstancesInvalidatableManager = InvalidatableManager()

    val rootModelInvalidatableManager = InvalidatableManager()

    fun invalidateExistingInstances() = existingInstancesInvalidatableManager.invalidate()

    fun invalidateRootModels() {
        invalidateExistingInstances()
        rootModelInvalidatableManager.invalidate()
    }
}