package com.krystianwsul.checkme.utils

import com.krystianwsul.checkme.domainmodel.InstanceRecord
import com.krystianwsul.checkme.utils.time.DateTime

sealed class InstanceData<T> {

    class RealInstanceData<T, U : InstanceRecord<T>>(val instanceRecord: U)

    class VirtualInstanceData<T>(val taskId: T, val scheduleDateTime: DateTime) : InstanceData<T>()
}

