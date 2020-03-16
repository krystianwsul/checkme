package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.models.TaskHierarchy
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemoteToRemoteConversion<S : RemoteCustomTimeId, T : ProjectKey> {

    val startTasks = mutableMapOf<String, Pair<Task<S, T>, List<Instance<S, T>>>>()
    val startTaskHierarchies = mutableListOf<TaskHierarchy<S, T>>()

    val endTasks = HashMap<String, Task<*, *>>()
    val endTaskHierarchies = ArrayList<TaskHierarchy<*, *>>()
}
