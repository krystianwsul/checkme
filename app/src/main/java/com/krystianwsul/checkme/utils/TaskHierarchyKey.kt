package com.krystianwsul.checkme.utils

sealed class TaskHierarchyKey {

    class LocalTaskHierarchyKey(val id: Int) : TaskHierarchyKey()

    class RemoteTaskHierarchyKey(val projectId: String, val taskHierarchyId: String) : TaskHierarchyKey()
}