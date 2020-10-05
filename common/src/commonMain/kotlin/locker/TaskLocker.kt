package com.krystianwsul.common.locker

import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectType

class TaskLocker<T : ProjectType>(projectLocker: ProjectLocker<T>) {

    private val instanceLockers = mutableMapOf<InstanceKey, InstanceLocker<T>>()

    fun getTaskLocker(instanceKey: InstanceKey): InstanceLocker<T> {
        if (!instanceLockers.containsKey(instanceKey)) instanceLockers[instanceKey] = InstanceLocker(this)

        return instanceLockers.getValue(instanceKey)
    }
}