package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.task.ProjectTask
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey

class ProjectToRootConversion {

    val startTasks = mutableMapOf<TaskKey.Project, Pair<ProjectTask, List<Instance>>>()
    val startTaskHierarchies = mutableMapOf<TaskHierarchyKey, TaskHierarchy>()

    val endTasks = HashMap<TaskKey.Project, RootTask>()

    val copiedTaskKeys = mutableMapOf<TaskKey, TaskKey>()
}
