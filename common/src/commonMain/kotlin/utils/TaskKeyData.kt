package com.krystianwsul.common.utils

data class TaskKeyData(val projectId: String, val taskId: String) {

    constructor(taskKey: TaskKey) : this(
            (taskKey as? TaskKey.Project)?.projectKey
                    ?.key
                    ?: "",
            taskKey.taskId,
    )

    val root by lazy { projectId.isEmpty() }
}