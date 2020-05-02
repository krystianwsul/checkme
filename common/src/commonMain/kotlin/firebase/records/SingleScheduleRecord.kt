package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.domain.schedules.SingleScheduleBridge
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
), SingleScheduleBridge<T> {

    private val singleScheduleJson by lazy { createObject.singleScheduleJson!! }

    override val year by lazy { singleScheduleJson.year }
    override val month by lazy { singleScheduleJson.month }
    override val day by lazy { singleScheduleJson.day }

    override val originalTimePair get() = timePair

    override fun deleteFromParent() = check(taskRecord.singleScheduleRecords.remove(id) == this)
}
