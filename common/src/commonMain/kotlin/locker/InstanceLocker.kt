package com.krystianwsul.common.locker

import com.krystianwsul.common.firebase.models.Instance

class InstanceLocker(private val taskLocker: TaskLocker<*>) {

    val now get() = taskLocker.now

    var childInstances: List<Instance>? = null

    val isVisible: MutableMap<Instance.VisibilityOptions, Boolean> = mutableMapOf()
}