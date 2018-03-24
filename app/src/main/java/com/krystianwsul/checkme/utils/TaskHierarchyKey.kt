package com.krystianwsul.checkme.utils

sealed class TaskHierarchyKey {

    class LocalTaskHierarchyKey(id: Int) : TaskHierarchyKey()

    class RemoteTaskHierarchyKey(projectId: String, taskHierarchyId: String) : TaskHierarchyKey()
}