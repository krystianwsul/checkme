package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.models.TaskHierarchy
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey

class RemoteToRemoteConversion<T : ProjectType> {

    val startTasks = mutableMapOf<String, Pair<Task<T>, List<Instance<T>>>>()
    val startTaskHierarchies = mutableListOf<TaskHierarchy<T>>()

    val endTasks = HashMap<String, Task<*>>()

    val copiedTaskKeys = mutableMapOf<TaskKey, TaskKey>()
}
