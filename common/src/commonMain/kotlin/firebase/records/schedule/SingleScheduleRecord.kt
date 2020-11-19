package com.krystianwsul.common.firebase.records.schedule


import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapperBridge
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ProjectType


open class SingleScheduleRecord<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        scheduleWrapper: ScheduleWrapper<T>,
        id: String? = null,
        override val scheduleWrapperBridge: ScheduleWrapperBridge<T> = ScheduleWrapperBridge.fromScheduleWrapper(scheduleWrapper),
) : ScheduleRecord<T>(
        taskRecord,
        scheduleWrapper,
        scheduleWrapperBridge.singleScheduleJson!!,
        "singleScheduleJson",
        id,
) {

    val singleScheduleJson by lazy { scheduleWrapperBridge.singleScheduleJson!! }

    open val originalTimePair get() = timePair

    open val date by lazy {
        singleScheduleJson.run { Date(year, month, day) }
    }

    open val originalDate get() = date

    val group = singleScheduleJson.group

    override fun deleteFromParent() = check(taskRecord.singleScheduleRecords.remove(id) == this)
}