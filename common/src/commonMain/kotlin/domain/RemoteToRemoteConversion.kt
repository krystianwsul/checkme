package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.utils.TaskKey

class RemoteToRemoteConversion {

    val startTasks = mutableMapOf<String, Pair<Task, List<Instance>>>()
    val startTaskHierarchies = mutableListOf<TaskHierarchy>()

    val endTasks = HashMap<String, Task>()

    val copiedTaskKeys = mutableMapOf<TaskKey, TaskKey>()
}
