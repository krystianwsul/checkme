package com.krystianwsul.common.firebase.records.schedule


import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapperBridge
import com.krystianwsul.common.firebase.records.task.TaskRecord


class MonthlyWeekScheduleRecord(
        taskRecord: TaskRecord,
        scheduleWrapper: ScheduleWrapper,
        override val projectHelper: ProjectHelper,
        id: String? = null,
        override val scheduleWrapperBridge: ScheduleWrapperBridge =
                ScheduleWrapperBridge.fromScheduleWrapper(scheduleWrapper),
) : RepeatingScheduleRecord(
        taskRecord,
        scheduleWrapper,
        scheduleWrapperBridge.monthlyWeekScheduleJson!!,
        "monthlyWeekScheduleJson",
        id,
) {

    private val monthlyWeekScheduleJson by lazy { scheduleWrapperBridge.monthlyWeekScheduleJson!! }

    val weekOfMonth by lazy { monthlyWeekScheduleJson.dayOfMonth }

    val dayOfWeek by lazy { monthlyWeekScheduleJson.dayOfWeek }

    val beginningOfMonth by lazy { monthlyWeekScheduleJson.beginningOfMonth }

    override fun deleteFromParent() = check(taskRecord.monthlyWeekScheduleRecords.remove(id) == this)
}
