package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.SingleScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteSingleScheduleRecord
import com.krystianwsul.common.utils.ProjectType

class RemoteSingleScheduleBridge<T : ProjectType>(
        private val remoteSingleScheduleRecord: RemoteSingleScheduleRecord<T>
) : RemoteScheduleBridge<T>(remoteSingleScheduleRecord), SingleScheduleBridge<T> {

    override val year get() = remoteSingleScheduleRecord.year

    override val month get() = remoteSingleScheduleRecord.month

    override val day get() = remoteSingleScheduleRecord.day

    override val originalTimePair get() = timePair
}
