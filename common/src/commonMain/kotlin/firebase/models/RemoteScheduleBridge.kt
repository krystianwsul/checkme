package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.ScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteProjectRecord
import com.krystianwsul.common.firebase.records.RemoteScheduleRecord
import com.krystianwsul.common.utils.RemoteCustomTimeId


abstract class RemoteScheduleBridge<T : RemoteCustomTimeId>(
        private val remoteProjectRecord: RemoteProjectRecord<T, *>,
        private val remoteScheduleRecord: RemoteScheduleRecord<T>
) : ScheduleBridge {

    // use project record instead
    override val customTimeKey by lazy { remoteScheduleRecord.customTimeId?.let { remoteProjectRecord.getRemoteCustomTimeKey(it) } }
}