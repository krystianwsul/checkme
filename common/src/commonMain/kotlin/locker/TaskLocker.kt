package com.krystianwsul.common.locker

import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectType

class TaskLocker<T : ProjectType>(private val projectLocker: ProjectLocker<T>) {

    private val instanceLockers = mutableMapOf<InstanceKey, InstanceLocker<T>>()

    val now get() = projectLocker.now

    var instances = mutableMapOf<Triple<ExactTimeStamp?, ExactTimeStamp, ExactTimeStamp>, Task.InstanceResult<T>>()

    fun getInstanceLocker(instanceKey: InstanceKey): InstanceLocker<T> {
        if (!instanceLockers.containsKey(instanceKey)) instanceLockers[instanceKey] = InstanceLocker(this)

        return instanceLockers.getValue(instanceKey)
    }
}