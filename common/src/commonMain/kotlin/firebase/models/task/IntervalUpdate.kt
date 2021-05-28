package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.firebase.models.interval.IntervalInfo

class IntervalUpdate(val task: RootTask, val intervalInfo: IntervalInfo)

private val intervalUpdates = mutableMapOf<Task, IntervalUpdate>()

fun RootTask.performIntervalUpdate(action: IntervalUpdate.() -> Unit) {
    checkNoIntervalUpdate()

    val intervalUpdate = IntervalUpdate(this, intervalInfo)

    try {
        intervalUpdate.action()
    } finally {
        check(intervalUpdates.containsKey(this))

        intervalUpdates.remove(this)
    }
}

fun Task.checkNoIntervalUpdate() = check(!intervalUpdates.containsKey(this))