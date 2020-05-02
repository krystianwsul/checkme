package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleJson
import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
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

    open val hour = scheduleJson.hour
    open val minute = scheduleJson.minute

    open val customTimeKey by lazy {
        scheduleJson.customTimeId?.let { taskRecord.getCustomTimeKey(it) }
    }

    val rootTaskKey by lazy { TaskKey(projectKey, taskId) }

    val scheduleId by lazy { ScheduleId(projectKey, taskId, id) }

    open val timePair
        get() = customTimeKey?.let { TimePair(it) } ?: TimePair(HourMinute(hour!!, minute!!))
}
