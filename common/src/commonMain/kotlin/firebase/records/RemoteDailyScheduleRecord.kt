package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey


class RemoteDailyScheduleRecord<T : CustomTimeId, U : ProjectKey>(
        id: String,
        remoteTaskRecord: RemoteTaskRecord<T, U>,
        scheduleWrapper: ScheduleWrapper
) : RemoteScheduleRecord<T, U>(
        id,
        remoteTaskRecord,
        scheduleWrapper,
        scheduleWrapper.dailyScheduleJson!!,
        "dailyScheduleJson"
) {

    override fun deleteFromParent() = check(remoteTaskRecord.remoteDailyScheduleRecords.remove(id) == this)
}
