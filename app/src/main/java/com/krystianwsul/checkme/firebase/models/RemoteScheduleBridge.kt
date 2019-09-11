package com.krystianwsul.checkme.firebase.models

import com.krystianwsul.checkme.domain.schedules.ScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteProjectRecord
import com.krystianwsul.common.firebase.records.RemoteScheduleRecord
import com.krystianwsul.common.utils.RemoteCustomTimeId


abstract class RemoteScheduleBridge<T : RemoteCustomTimeId>(
        private val remoteProjectRecord: RemoteProjectRecord<T>,
        private val remoteScheduleRecord: RemoteScheduleRecord<T>
) : ScheduleBridge {

    // use project record instead
    override val customTimeKey by lazy { remoteScheduleRecord.customTimeId?.let { remoteProjectRecord.getRemoteCustomTimeKey(it) } }
}