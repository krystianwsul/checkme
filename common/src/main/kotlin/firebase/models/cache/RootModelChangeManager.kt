package com.krystianwsul.common.firebase.models.cache

class RootModelChangeManager {

    val existingInstancesInvalidatableManager = InvalidatableManager()

    val rootTaskProjectIdInvalidatableManager = InvalidatableManager()

    val rootModelInvalidatableManager = InvalidatableManager()

    val rootTaskInvalidatableManager = InvalidatableManager()

    val userInvalidatableManager = InvalidatableManager()

    /*
    This handles an atypical subset of changes:
    1. Local edits to the HourMinutes for existing CustomTimes
    2. Remote changes to my user
    3. Remote changes to other users
     */
    val customTimesInvalidatableManager = InvalidatableManager().also(userInvalidatableManager::addInvalidatable)

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
        rootModelInvalidatableManager.invalidate()
    }

    fun invalidateUsers() = userInvalidatableManager.invalidate()
}