package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType


class YearlyScheduleRecord<T : ProjectType> : ScheduleRecord<T> {

    private val yearlyScheduleJson by lazy { createObject.yearlyScheduleJson!! }

    val month by lazy { yearlyScheduleJson.month }
    val day by lazy { yearlyScheduleJson.day }

    val from by lazy { yearlyScheduleJson.from }
    val until by lazy { yearlyScheduleJson.until }

    constructor(
            id: String,
            taskRecord: TaskRecord<T>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            id,
            taskRecord,
            scheduleWrapper,
            scheduleWrapper.yearlyScheduleJson!!,
            "yearlyScheduleJson"
    )

    constructor(
            taskRecord: TaskRecord<T>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            taskRecord,
            scheduleWrapper,
            scheduleWrapper.yearlyScheduleJson!!,
            "yearlyScheduleJson"
    )

    override fun deleteFromParent() = check(taskRecord.yearlyScheduleRecords.remove(id) == this)
}
