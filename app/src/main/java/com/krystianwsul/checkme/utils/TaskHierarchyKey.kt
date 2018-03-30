package com.krystianwsul.checkme.utils

sealed class TaskHierarchyKey {

    data class LocalTaskHierarchyKey(val id: Int) : TaskHierarchyKey()

    data class RemoteTaskHierarchyKey(val projectId: String, val taskHierarchyId: String) : TaskHierarchyKey()
}