package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType


class SingleScheduleRecord<T : ProjectType> : ScheduleRecord<T> {

    private val singleScheduleJson by lazy { createObject.singleScheduleJson!! }

    val year by lazy { singleScheduleJson.year }
    val month by lazy { singleScheduleJson.month }
    val day by lazy { singleScheduleJson.day }

    constructor(
            id: String,
            taskRecord: TaskRecord<T>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            id,
            taskRecord,
            scheduleWrapper,
            scheduleWrapper.singleScheduleJson!!,
            "singleScheduleJson"
    )

    constructor(
            taskRecord: TaskRecord<T>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            taskRecord,
            scheduleWrapper,
            scheduleWrapper.singleScheduleJson!!,
            "singleScheduleJson"
    )

    override fun deleteFromParent() = check(taskRecord.singleScheduleRecords.remove(id) == this)
}
