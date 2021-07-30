package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.task.ProjectTask
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey

class ProjectToRootConversion {

    val startTasks = mutableMapOf<String, Pair<ProjectTask, List<Instance>>>()
    val startTaskHierarchies = mutableMapOf<TaskHierarchyKey, TaskHierarchy>()

    val endTasks = HashMap<String, RootTask>()

    val copiedTaskKeys = mutableMapOf<TaskKey, TaskKey>()
}
