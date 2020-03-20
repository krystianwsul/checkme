package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType


class RemoteMonthlyDayScheduleRecord<T : ProjectType> : RemoteScheduleRecord<T> {

    private val monthlyDayScheduleJson by lazy { createObject.monthlyDayScheduleJson!! }

    val dayOfMonth by lazy { monthlyDayScheduleJson.dayOfMonth }

    val beginningOfMonth by lazy { monthlyDayScheduleJson.beginningOfMonth }

    val from by lazy { monthlyDayScheduleJson.from }
    val until by lazy { monthlyDayScheduleJson.until }

    constructor(
            id: String,
            taskRecord: TaskRecord<T>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            id,
            taskRecord,
            scheduleWrapper,
            scheduleWrapper.monthlyDayScheduleJson!!,
            "monthlyDayScheduleJson"
    )

    constructor(
            taskRecord: TaskRecord<T>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            taskRecord,
            scheduleWrapper,
            scheduleWrapper.monthlyDayScheduleJson!!,
            "monthlyDayScheduleJson"
    )

    override fun deleteFromParent() = check(taskRecord.remoteMonthlyDayScheduleRecords.remove(id) == this)
}
