package com.krystianwsul.common.locker

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.TaskHierarchy
import com.krystianwsul.common.utils.ProjectType

class InstanceLocker<T : ProjectType>(private val taskLocker: TaskLocker<T>) {

    val now get() = taskLocker.now

    var childInstances: List<Pair<Instance<T>, TaskHierarchy<T>>>? = null
}