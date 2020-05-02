package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType


class SingleScheduleRecord<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        scheduleWrapper: ScheduleWrapper,
        id: String? = null
) : ScheduleRecord<T>(
        taskRecord,
        scheduleWrapper,
        scheduleWrapper.singleScheduleJson!!,
        "singleScheduleJson",
        id
) {

    private val singleScheduleJson by lazy { createObject.singleScheduleJson!! }

    val year by lazy { singleScheduleJson.year }
    val month by lazy { singleScheduleJson.month }
    val day by lazy { singleScheduleJson.day }

    override fun deleteFromParent() = check(taskRecord.singleScheduleRecords.remove(id) == this)
}
