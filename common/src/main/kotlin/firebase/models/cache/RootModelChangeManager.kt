package com.krystianwsul.common.firebase.models.cache

class RootModelChangeManager {

    val existingInstancesInvalidatableManager = InvalidatableManager()

    val rootTaskProjectIdInvalidatableManager = InvalidatableManager()

    val rootModelInvalidatableManager = InvalidatableManager()

    val rootTaskInvalidatableManager = InvalidatableManager()

    val projectInvalidatableManager = InvalidatableManager()

    fun invalidateExistingInstances() = existingInstancesInvalidatableManager.invalidate()

    fun invalidateRootTaskProjectIds() = rootTaskProjectIdInvalidatableManager.invalidate()

    fun invalidateRootTasks() {
        invalidateExistingInstances()
        invalidateRootTaskProjectIds()
        rootTaskInvalidatableManager.invalidate()
        rootModelInvalidatableManager.invalidate()
    }

    fun invalidateProjects() {
        invalidateExistingInstances()
        projectInvalidatableManager.invalidate()
        rootModelInvalidatableManager.invalidate()
    }
}