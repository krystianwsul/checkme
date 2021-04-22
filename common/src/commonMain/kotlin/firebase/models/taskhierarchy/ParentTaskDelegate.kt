package com.krystianwsul.common.firebase.models.taskhierarchy

import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.utils.TaskKey

class ParentTaskDelegate(private val project: Project<*>) {

    fun getTaskKey(parentTaskId: String): TaskKey = TaskKey.Project(project.projectKey, parentTaskId)

    fun getTask(parentTaskId: String): Task = project.getTaskForce(parentTaskId)
}