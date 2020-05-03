package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleJson
import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleId
import com.krystianwsul.common.utils.TaskKey

abstract class ScheduleRecord<T : ProjectType>(
        val taskRecord: TaskRecord<T>,
        final override val createObject: ScheduleWrapper,
        private val scheduleJson: ScheduleJson,
        endTimeKey: String,
        _id: String?
) : RemoteRecord(_id == null) {

    companion object {

        const val SCHEDULES = "schedules"
    }

    val id = _id ?: taskRecord.getScheduleRecordId()

    final override val key get() = taskRecord.key + "/" + SCHEDULES + "/" + id

    val startTime get() = scheduleJson.startTime

    var endTime by Committer(scheduleJson::endTime, "$key/$endTimeKey")

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
        get() = timePair.customTimeKey as CustomTimeKey<T>
}
