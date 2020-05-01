package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType


class DailyScheduleRecord<T : ProjectType>(
        id: String,
        taskRecord: TaskRecord<T>,
        scheduleWrapper: ScheduleWrapper
) : ScheduleRecord<T>(
        id,
        taskRecord,
        scheduleWrapper,
        scheduleWrapper.dailyScheduleJson!!,
        "dailyScheduleJson"
) {

    override fun deleteFromParent() = check(taskRecord.dailyScheduleRecords.remove(id) == this)
}
