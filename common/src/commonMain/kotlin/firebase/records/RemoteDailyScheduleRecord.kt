package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType


class RemoteDailyScheduleRecord<T : ProjectType>(
        id: String,
        remoteTaskRecord: RemoteTaskRecord<T>,
        scheduleWrapper: ScheduleWrapper
) : RemoteScheduleRecord<T>(
        id,
        remoteTaskRecord,
        scheduleWrapper,
        scheduleWrapper.dailyScheduleJson!!,
        "dailyScheduleJson"
) {

    override fun deleteFromParent() = check(remoteTaskRecord.remoteDailyScheduleRecords.remove(id) == this)
}
