package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType


class WeeklyScheduleRecord<T : ProjectType> : ScheduleRecord<T> {

    private val weeklyScheduleJson by lazy { createObject.weeklyScheduleJson!! }

    val dayOfWeek by lazy { weeklyScheduleJson.dayOfWeek }

    val from by lazy { weeklyScheduleJson.from }
    val until by lazy { weeklyScheduleJson.until }

    constructor(
            id: String,
            taskRecord: TaskRecord<T>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            id,
            taskRecord,
            scheduleWrapper,
            scheduleWrapper.weeklyScheduleJson!!,
            "weeklyScheduleJson"
    )

    constructor(
            taskRecord: TaskRecord<T>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            taskRecord,
            scheduleWrapper,
            scheduleWrapper.weeklyScheduleJson!!,
            "weeklyScheduleJson"
    )

    override fun deleteFromParent() = check(taskRecord.weeklyScheduleRecords.remove(id) == this)
}
