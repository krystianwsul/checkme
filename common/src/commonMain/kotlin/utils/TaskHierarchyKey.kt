package com.krystianwsul.common.utils

sealed class TaskHierarchyKey {

    abstract val taskHierarchyId: String

    data class Project(val projectId: ProjectKey<*>, override val taskHierarchyId: String) : TaskHierarchyKey()

    data class Nested(val childTaskKey: TaskKey, override val taskHierarchyId: String) : TaskHierarchyKey()
}