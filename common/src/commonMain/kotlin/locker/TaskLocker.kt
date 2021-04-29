package com.krystianwsul.common.locker

import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectType

class TaskLocker<T : ProjectType>(private val projectLocker: ProjectLocker<T>) {

    private val instanceLockers = mutableMapOf<InstanceKey, InstanceLocker>()

    val now get() = projectLocker.now

    fun getInstanceLocker(instanceKey: InstanceKey) =
            instanceLockers.getOrPut(instanceKey) { InstanceLocker(this) }
}