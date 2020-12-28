package com.krystianwsul.common.locker

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.utils.ProjectType

class InstanceLocker<T : ProjectType>(private val taskLocker: TaskLocker<T>) {

    val now get() = taskLocker.now

    var childInstances: List<Instance<T>>? = null

    val isVisible: MutableMap<Instance.VisibilityOptions, Boolean> = mutableMapOf()
}