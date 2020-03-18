package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.WeeklyScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteDailyScheduleRecord
import com.krystianwsul.common.firebase.records.RemoteProjectRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemoteDailyScheduleBridge<T : RemoteCustomTimeId, U : ProjectKey>(
        remoteProjectRecord: RemoteProjectRecord<T, U>,
        remoteDailyScheduleRecord: RemoteDailyScheduleRecord<T, U>
) : RemoteScheduleBridge<T, U>(remoteProjectRecord, remoteDailyScheduleRecord), WeeklyScheduleBridge {

    override val daysOfWeek = DayOfWeek.values()
            .map { it.ordinal }
            .toSet()

    override var from: Date? = null
    override var until: Date? = null
}
