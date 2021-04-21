package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.task.ProjectTask
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.utils.TaskKey

class RemoteToRemoteConversion {

    val startTasks = mutableMapOf<String, Pair<ProjectTask, List<Instance>>>()
    val startTaskHierarchies = mutableListOf<TaskHierarchy>()

    val endTasks = HashMap<String, ProjectTask>()

    val copiedTaskKeys = mutableMapOf<TaskKey, TaskKey>()
}
