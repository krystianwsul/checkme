package com.krystianwsul.common.firebase.models.taskhierarchy

import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.taskhierarchy.ParentTaskDelegate.Project
import com.krystianwsul.common.firebase.models.taskhierarchy.ParentTaskDelegate.Root
import com.krystianwsul.common.firebase.records.taskhierarchy.TaskHierarchyRecord
import com.krystianwsul.common.utils.TaskKey

sealed class ParentTaskDelegate {

    abstract val parentTaskKey: TaskKey

    abstract fun getParentTask(): Task

    class Project(
        private val project: com.krystianwsul.common.firebase.models.project.Project<*>,
        taskHierarchyRecord: TaskHierarchyRecord<*>,
    ) : ParentTaskDelegate() {

        override val parentTaskKey by lazy { TaskKey.Project(project.projectKey, taskHierarchyRecord.parentTaskId) }

        override fun getParentTask() = project.getProjectTaskForce(parentTaskKey)
    }

    class Root(
        private val rootTaskParent: RootTask.Parent,
        taskHierarchyRecord: TaskHierarchyRecord<*>,
    ) : ParentTaskDelegate() {

        override val parentTaskKey by lazy { TaskKey.Root(taskHierarchyRecord.parentTaskId) }

        override fun getParentTask() = rootTaskParent.getRootTask(parentTaskKey)
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