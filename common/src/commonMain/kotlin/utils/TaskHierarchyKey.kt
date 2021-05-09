package com.krystianwsul.common.utils

sealed class TaskHierarchyKey {

    abstract val taskHierarchyId: TaskHierarchyId

    data class Project(val projectId: ProjectKey<*>, override val taskHierarchyId: TaskHierarchyId) : TaskHierarchyKey()

    data class Nested(val childTaskKey: TaskKey, override val taskHierarchyId: TaskHierarchyId) : TaskHierarchyKey()
}