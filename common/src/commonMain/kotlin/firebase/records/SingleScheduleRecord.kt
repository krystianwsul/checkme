package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ProjectType


open class SingleScheduleRecord<T : ProjectType>(
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

    open val originalTimePair get() = timePair

    open val date by lazy {
        singleScheduleJson.run { Date(year, month, day) }
    }

    open val originalDate get() = date

    val group = singleScheduleJson.group

    override fun deleteFromParent() = check(taskRecord.singleScheduleRecords.remove(id) == this)
}
