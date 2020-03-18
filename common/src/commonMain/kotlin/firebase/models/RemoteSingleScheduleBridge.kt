package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.SingleScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteSingleScheduleRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemoteSingleScheduleBridge<T : RemoteCustomTimeId, U : ProjectKey>(
        private val remoteSingleScheduleRecord: RemoteSingleScheduleRecord<T, U>
) : RemoteScheduleBridge<T, U>(remoteSingleScheduleRecord), SingleScheduleBridge {

    override val year get() = remoteSingleScheduleRecord.year

    override val month get() = remoteSingleScheduleRecord.month

    override val day get() = remoteSingleScheduleRecord.day
}
