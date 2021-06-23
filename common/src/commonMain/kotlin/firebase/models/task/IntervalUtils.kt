package com.krystianwsul.common.firebase.models.task

private val intervalUpdates = mutableMapOf<Task, IntervalUpdate>()

fun Task.performIntervalUpdate(action: IntervalUpdate.() -> Unit) {
    checkNoIntervalUpdate()

    val intervalUpdate = IntervalUpdate(this, intervalInfo)
    intervalUpdates[this] = intervalUpdate

    try {
        intervalUpdate.action()
    } finally {
        check(intervalUpdates.containsKey(this))

        intervalUpdates.remove(this)
    }

    if (intervalUpdate.intervalsInvalid) intervalInfoProperty.invalidate()
}

fun RootTask.performRootIntervalUpdate(action: RootIntervalUpdate.() -> Unit) {
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