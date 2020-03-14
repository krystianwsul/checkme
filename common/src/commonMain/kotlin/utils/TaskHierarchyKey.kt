package com.krystianwsul.common.utils

sealed class TaskHierarchyKey {

    data class Remote(val projectId: ProjectKey, val taskHierarchyId: String) : TaskHierarchyKey()
}