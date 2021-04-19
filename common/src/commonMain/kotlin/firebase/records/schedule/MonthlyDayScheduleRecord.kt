package com.krystianwsul.common.firebase.records.schedule


import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapperBridge
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.ProjectType


class MonthlyDayScheduleRecord<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        scheduleWrapper: ScheduleWrapper,
        id: String? = null,
        override val scheduleWrapperBridge: ScheduleWrapperBridge = ScheduleWrapperBridge.fromScheduleWrapper(scheduleWrapper),
) : RepeatingScheduleRecord<T>(
        taskRecord,
        scheduleWrapper,
        scheduleWrapperBridge.monthlyDayScheduleJson!!,
        "monthlyDayScheduleJson",
        id
) {

    private val monthlyDayScheduleJson by lazy { scheduleWrapperBridge.monthlyDayScheduleJson!! }

    val dayOfMonth by lazy { monthlyDayScheduleJson.dayOfMonth }

    val beginningOfMonth by lazy { monthlyDayScheduleJson.beginningOfMonth }

    override fun deleteFromParent() = check(taskRecord.monthlyDayScheduleRecords.remove(id) == this)
}
