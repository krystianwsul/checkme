package com.krystianwsul.common.firebase.models.taskhierarchy

import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.utils.TaskKey

sealed class ParentTaskDelegate {

    abstract fun getTaskKey(parentTaskId: String): TaskKey

    abstract fun getTask(parentTaskId: String): Task

    abstract fun removeRootChildTaskFromParent(parentTask: Task, childTask: Task)

    class Project(
            private val project: com.krystianwsul.common.firebase.models.project.Project<*>,
    ) : ParentTaskDelegate() {

        override fun getTaskKey(parentTaskId: String) = TaskKey.Project(project.projectKey, parentTaskId)

        override fun getTask(parentTaskId: String): Task = project.getProjectTaskForce(getTaskKey(parentTaskId))

        override fun removeRootChildTaskFromParent(parentTask: Task, childTask: Task) {}
    }

    class Root(private val rootTaskParent: RootTask.Parent) : ParentTaskDelegate() {

        override fun getTaskKey(parentTaskId: String) = TaskKey.Root(parentTaskId)

        override fun getTask(parentTaskId: String) = rootTaskParent.getRootTask(getTaskKey(parentTaskId))

        override fun removeRootChildTaskFromParent(parentTask: Task, childTask: Task) {
            (parentTask as RootTask).removeRootTask(childTask as RootTask)
        }
    }
}