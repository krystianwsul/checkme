package com.krystianwsul.common.firebase.models.cache

class RootModelChangeManager {

    val existingInstancesInvalidatableManager = InvalidatableManager()

    val rootModelInvalidatableManager = InvalidatableManager()

    val projectInvalidatableManager = InvalidatableManager()

    fun invalidateExistingInstances() = existingInstancesInvalidatableManager.invalidate()

    fun invalidateRootTasks() {
        invalidateExistingInstances()
        rootModelInvalidatableManager.invalidate()
    }

    fun invalidateProjects() {
        invalidateRootTasks()
        projectInvalidatableManager.invalidate()
    }
}