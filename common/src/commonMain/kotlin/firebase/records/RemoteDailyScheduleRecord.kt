package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType


class RemoteDailyScheduleRecord<T : ProjectType>(
        id: String,
        taskRecord: TaskRecord<T>,
        scheduleWrapper: ScheduleWrapper
) : RemoteScheduleRecord<T>(
        id,
        taskRecord,
        scheduleWrapper,
        scheduleWrapper.dailyScheduleJson!!,
        "dailyScheduleJson"
) {

    override fun deleteFromParent() = check(taskRecord.remoteDailyScheduleRecords.remove(id) == this)
}
