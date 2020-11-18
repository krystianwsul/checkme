package com.krystianwsul.common.firebase.records.schedule


import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.ProjectType


class WeeklyScheduleRecord<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        scheduleWrapper: ScheduleWrapper<T>,
        assignedToHelper: AssignedToHelper<T>,
        id: String? = null,
) : RepeatingScheduleRecord<T>(
        taskRecord,
        scheduleWrapper,
        scheduleWrapper.weeklyScheduleJson!!,
        assignedToHelper,
        "weeklyScheduleJson",
        id
) {

    private val weeklyScheduleJson by lazy { createObject.weeklyScheduleJson!! }

    val dayOfWeek by lazy { weeklyScheduleJson.dayOfWeek }

    val interval by lazy { weeklyScheduleJson.interval }

    override fun deleteFromParent() = check(taskRecord.weeklyScheduleRecords.remove(id) == this)
}
