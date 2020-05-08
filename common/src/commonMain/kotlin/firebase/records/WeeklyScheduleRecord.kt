package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType


class WeeklyScheduleRecord<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        scheduleWrapper: ScheduleWrapper,
        id: String? = null
) : RepeatingScheduleRecord<T>(
        taskRecord,
        scheduleWrapper,
        scheduleWrapper.weeklyScheduleJson!!,
        "weeklyScheduleJson",
        id
) {

    private val weeklyScheduleJson by lazy { createObject.weeklyScheduleJson!! }

    val daysOfWeek by lazy { setOf(weeklyScheduleJson.dayOfWeek) } // todo no set

    override fun deleteFromParent() = check(taskRecord.weeklyScheduleRecords.remove(id) == this)
}
