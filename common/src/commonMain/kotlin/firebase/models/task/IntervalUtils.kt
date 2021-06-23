package com.krystianwsul.common.firebase.models.task

private val intervalUpdates = mutableMapOf<Task, RootIntervalUpdate>()

fun RootTask.performIntervalUpdate(action: RootIntervalUpdate.() -> Unit) {
    checkNoIntervalUpdate()
    ProjectRootTaskIdTracker.checkTracking()

    val intervalUpdate = RootIntervalUpdate(this, intervalInfo)
    intervalUpdates[this] = intervalUpdate

    try {
        intervalUpdate.action()
    } finally {
        check(intervalUpdates.containsKey(this))

        intervalUpdates.remove(this)
    }

    if (intervalUpdate.intervalsInvalid) intervalInfoProperty.invalidate()
}

fun Task.checkNoIntervalUpdate() = check(!intervalUpdates.containsKey(this))

fun Task.getIntervalUpdate() = intervalUpdates[this]