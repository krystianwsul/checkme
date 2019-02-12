package com.krystianwsul.checkme.utils

sealed class TaskHierarchyKey {

    data class RemoteTaskHierarchyKey(val projectId: String, val taskHierarchyId: String) : TaskHierarchyKey()
}