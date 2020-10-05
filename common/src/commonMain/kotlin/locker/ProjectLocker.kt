package com.krystianwsul.common.locker

import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey

class ProjectLocker<T : ProjectType>(private val locker: LockerManager.State.Locker) {

    private val taskLockers = mutableMapOf<TaskKey, TaskLocker<T>>()

    val now get() = locker.now

    fun getTaskLocker(taskKey: TaskKey): TaskLocker<T> {
        if (!taskLockers.containsKey(taskKey)) taskLockers[taskKey] = TaskLocker(this)

        return taskLockers.getValue(taskKey)
    }
}