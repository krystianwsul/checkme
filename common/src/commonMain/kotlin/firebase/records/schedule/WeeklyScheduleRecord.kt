package com.krystianwsul.common.firebase.records.schedule


import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapperBridge
import com.krystianwsul.common.firebase.records.task.TaskRecord


class WeeklyScheduleRecord(
        taskRecord: TaskRecord,
        scheduleWrapper: ScheduleWrapper,
        override val projectHelper: ProjectHelper,
        id: String? = null,
        override val scheduleWrapperBridge: ScheduleWrapperBridge =
                ScheduleWrapperBridge.fromScheduleWrapper(scheduleWrapper),
) : RepeatingScheduleRecord(
        taskRecord,
        scheduleWrapper,
        scheduleWrapperBridge.weeklyScheduleJson!!,
        "weeklyScheduleJson",
        id,
) {

    private val weeklyScheduleJson by lazy { scheduleWrapperBridge.weeklyScheduleJson!! }

    val dayOfWeek by lazy { weeklyScheduleJson.dayOfWeek }

    val interval by lazy { weeklyScheduleJson.interval }

    override fun deleteFromParent() = check(taskRecord.weeklyScheduleRecords.remove(id) == this)
}
