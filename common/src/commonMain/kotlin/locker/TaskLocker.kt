package com.krystianwsul.common.locker

import com.krystianwsul.common.utils.InstanceKey

class TaskLocker(private val locker: LockerManager.State.Locker) {

    private val instanceLockers = mutableMapOf<InstanceKey, InstanceLocker>()

    val now get() = locker.now

    fun getInstanceLocker(instanceKey: InstanceKey) =
            instanceLockers.getOrPut(instanceKey) { InstanceLocker(this) }
}