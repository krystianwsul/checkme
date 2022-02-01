package com.krystianwsul.common.firebase.models.taskhierarchy

import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.taskhierarchy.ParentTaskDelegate.Project
import com.krystianwsul.common.firebase.models.taskhierarchy.ParentTaskDelegate.Root
import com.krystianwsul.common.firebase.records.taskhierarchy.TaskHierarchyRecord
import com.krystianwsul.common.utils.TaskKey

sealed class ParentTaskDelegate(private val taskHierarchyRecord: TaskHierarchyRecord<*>) {

    abstract fun getTaskKey(parentTaskId: String): TaskKey

    abstract fun getTask(parentTaskKey: String): Task

    class Project(
        private val project: com.krystianwsul.common.firebase.models.project.Project<*>,
        taskHierarchyRecord: TaskHierarchyRecord<*>,
    ) : ParentTaskDelegate(taskHierarchyRecord) {

        override fun getTaskKey(parentTaskId: String) = TaskKey.Project(project.projectKey, parentTaskId)

        override fun getTask(parentTaskKey: String): Task = project.getProjectTaskForce(getTaskKey(parentTaskKey))
    }

    class Root(
        private val rootTaskParent: RootTask.Parent,
        taskHierarchyRecord: TaskHierarchyRecord<*>,
    ) : ParentTaskDelegate(taskHierarchyRecord) {

        override fun getTaskKey(parentTaskId: String) = TaskKey.Root(parentTaskId)

        override fun getTask(parentTaskKey: String) = rootTaskParent.getRootTask(getTaskKey(parentTaskKey))
    }

    sealed interface Factory {

        fun newDelegate(taskHierarchyRecord: TaskHierarchyRecord<*>): ParentTaskDelegate

        class Project(private val project: com.krystianwsul.common.firebase.models.project.Project<*>) : Factory {

            override fun newDelegate(taskHierarchyRecord: TaskHierarchyRecord<*>) = Project(project, taskHierarchyRecord)
        }

        class Root(private val rootTaskParent: RootTask.Parent) : Factory {

            override fun newDelegate(taskHierarchyRecord: TaskHierarchyRecord<*>) = Root(rootTaskParent, taskHierarchyRecord)
        }
    }
}