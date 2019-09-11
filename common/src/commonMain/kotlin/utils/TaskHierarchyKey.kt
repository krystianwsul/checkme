package com.krystianwsul.common.utils

sealed class TaskHierarchyKey {

    data class Remote(val projectId: String, val taskHierarchyId: String) : TaskHierarchyKey()
}