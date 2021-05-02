package com.krystianwsul.common.firebase.records.schedule


import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapperBridge
import com.krystianwsul.common.firebase.records.task.TaskRecord


class MonthlyDayScheduleRecord(
        // todo task ids update stuff on creation
        taskRecord: TaskRecord,
        scheduleWrapper: ScheduleWrapper,
        override val projectHelper: ProjectHelper,
        id: String? = null,
        override val scheduleWrapperBridge: ScheduleWrapperBridge =
                ScheduleWrapperBridge.fromScheduleWrapper(scheduleWrapper),
) : RepeatingScheduleRecord(
        taskRecord,
        scheduleWrapper,
        scheduleWrapperBridge.monthlyDayScheduleJson!!,
        "monthlyDayScheduleJson",
        id,
) {

    private val monthlyDayScheduleJson by lazy { scheduleWrapperBridge.monthlyDayScheduleJson!! }

    val dayOfMonth by lazy { monthlyDayScheduleJson.dayOfMonth }

    val beginningOfMonth by lazy { monthlyDayScheduleJson.beginningOfMonth }

    override fun deleteFromParent() = check(taskRecord.monthlyDayScheduleRecords.remove(id) == this)
}
