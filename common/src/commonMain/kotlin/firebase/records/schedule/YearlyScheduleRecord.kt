package com.krystianwsul.common.firebase.records.schedule


import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapperBridge
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.ProjectType


class YearlyScheduleRecord<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        scheduleWrapper: ScheduleWrapper,
        id: String? = null,
        override val scheduleWrapperBridge: ScheduleWrapperBridge = ScheduleWrapperBridge.fromScheduleWrapper(scheduleWrapper),
) : RepeatingScheduleRecord<T>(
        taskRecord,
        scheduleWrapper,
        scheduleWrapperBridge.yearlyScheduleJson!!,
        "yearlyScheduleJson",
        id
) {

    private val yearlyScheduleJson by lazy { scheduleWrapperBridge.yearlyScheduleJson!! }

    val month by lazy { yearlyScheduleJson.month }
    val day by lazy { yearlyScheduleJson.day }

    override fun deleteFromParent() = check(taskRecord.yearlyScheduleRecords.remove(id) == this)
}
