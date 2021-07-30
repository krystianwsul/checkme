package com.krystianwsul.common.domain

import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey

class TaskUndoData {

    val taskKeys = mutableMapOf<TaskKey, Set<String>>() // schedule IDs
    val taskHierarchyKeys = mutableSetOf<TaskHierarchyKey>()
}