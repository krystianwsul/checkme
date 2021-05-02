package com.krystianwsul.common.firebase.records.schedule


import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapperBridge
import com.krystianwsul.common.firebase.records.task.TaskRecord


class YearlyScheduleRecord(
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
        scheduleWrapperBridge.yearlyScheduleJson!!,
        "yearlyScheduleJson",
        id,
) {

    private val yearlyScheduleJson by lazy { scheduleWrapperBridge.yearlyScheduleJson!! }

    val month by lazy { yearlyScheduleJson.month }
    val day by lazy { yearlyScheduleJson.day }

    override fun deleteFromParent() = check(taskRecord.yearlyScheduleRecords.remove(id) == this)
}
