package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.SingleScheduleBridge
import com.krystianwsul.common.firebase.records.SingleScheduleRecord
import com.krystianwsul.common.utils.ProjectType

class RemoteSingleScheduleBridge<T : ProjectType>(
        private val singleScheduleRecord: SingleScheduleRecord<T>
) : RemoteScheduleBridge<T>(singleScheduleRecord), SingleScheduleBridge<T> {

    override val year get() = singleScheduleRecord.year

    override val month get() = singleScheduleRecord.month

    override val day get() = singleScheduleRecord.day

    override val originalTimePair get() = timePair
}
