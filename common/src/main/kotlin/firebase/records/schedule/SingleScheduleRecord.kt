package com.krystianwsul.common.firebase.records.schedule


import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapperBridge
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ScheduleId


open class SingleScheduleRecord(
    taskRecord: TaskRecord,
    scheduleWrapper: ScheduleWrapper,
    override val projectHelper: ProjectHelper,
    projectRootDelegate: ProjectRootDelegate,
    id: ScheduleId,
    create: Boolean,
    override val scheduleWrapperBridge: ScheduleWrapperBridge =
        ScheduleWrapperBridge.fromScheduleWrapper(scheduleWrapper),
) : ScheduleRecord(
    taskRecord,
    scheduleWrapper,
    scheduleWrapperBridge.singleScheduleJson!!,
    "singleScheduleJson",
    id,
    create,
    projectRootDelegate,
) {

    val singleScheduleJson by lazy { scheduleWrapperBridge.singleScheduleJson!! }

    open val originalTimePair get() = timePair

    open val date by lazy {
        singleScheduleJson.run { Date(year, month, day) }
    }

    open val originalDate get() = date

    override fun deleteFromParent() = check(taskRecord.singleScheduleRecords.remove(id) == this)
}
