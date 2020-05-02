package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType


class YearlyScheduleRecord<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        scheduleWrapper: ScheduleWrapper,
        id: String? = null
) : ScheduleRecord<T>(
        taskRecord,
        scheduleWrapper,
        scheduleWrapper.yearlyScheduleJson!!,
        "yearlyScheduleJson",
        id
) {

    private val yearlyScheduleJson by lazy { createObject.yearlyScheduleJson!! }

    val month by lazy { yearlyScheduleJson.month }
    val day by lazy { yearlyScheduleJson.day }

    val from by lazy { yearlyScheduleJson.from }
    val until by lazy { yearlyScheduleJson.until }

    override fun deleteFromParent() = check(taskRecord.yearlyScheduleRecords.remove(id) == this)
}
