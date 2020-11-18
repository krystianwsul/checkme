package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType


class MonthlyDayScheduleRecord<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        scheduleWrapper: ScheduleWrapper,
        id: String? = null
) : RepeatingScheduleRecord<T>(
        taskRecord,
        scheduleWrapper,
        scheduleWrapper.monthlyDayScheduleJson!!,
        "monthlyDayScheduleJson",
        id
) {

    private val monthlyDayScheduleJson by lazy { createObject.monthlyDayScheduleJson!! }

    val dayOfMonth by lazy { monthlyDayScheduleJson.dayOfMonth }

    val beginningOfMonth by lazy { monthlyDayScheduleJson.beginningOfMonth }

    override fun deleteFromParent() = check(taskRecord.monthlyDayScheduleRecords.remove(id) == this)
}
