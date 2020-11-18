package com.krystianwsul.common.firebase.records.schedule


import com.krystianwsul.common.firebase.json.schedule.ScheduleJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapperBridge
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleId
import com.krystianwsul.common.utils.TaskKey

abstract class ScheduleRecord<T : ProjectType>(
        val taskRecord: TaskRecord<T>,
        final override val createObject: ScheduleWrapper<T>,
        private val scheduleJson: ScheduleJson<T>,
        scheduleTypeSubkey: String,
        _id: String?,
) : RemoteRecord(_id == null) {

    companion object {

        const val SCHEDULES = "schedules"
    }

    val id = _id ?: taskRecord.getScheduleRecordId()

    final override val key get() = taskRecord.key + "/" + SCHEDULES + "/" + id

    val startTime get() = scheduleJson.startTime
    open var startTimeOffset by Committer(scheduleJson::startTimeOffset, "$key/$scheduleTypeSubkey")

    open var endTime by Committer(scheduleJson::endTime, "$key/$scheduleTypeSubkey")
    open var endTimeOffset by Committer(scheduleJson::endTimeOffset, "$key/$scheduleTypeSubkey")

    val projectKey = taskRecord.projectKey

    val taskId = taskRecord.id

    val rootTaskKey by lazy { TaskKey(projectKey, taskId) }

    val scheduleId by lazy { ScheduleId(projectKey, taskId, id) }

    open val timePair by lazy {
        scheduleJson.run {
            customTimeId?.let {
                TimePair(taskRecord.getCustomTimeKey(it))
            } ?: TimePair(HourMinute(hour!!, minute!!))
        }
    }

    @Suppress("UNCHECKED_CAST") // I prefer to cast, than to have two entry points for this
    val customTimeKey
        get() = timePair.customTimeKey as? CustomTimeKey<T>

    val assignedTo get() = taskRecord.assignedToHelper.getAssignedTo(scheduleJson)

    abstract val scheduleWrapperBridge: ScheduleWrapperBridge<T>
}
