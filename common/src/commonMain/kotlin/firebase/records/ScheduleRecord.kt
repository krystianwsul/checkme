package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleJson
import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType

abstract class ScheduleRecord<T : ProjectType>(
        protected val taskRecord: TaskRecord<T>,
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

    val projectKey get() = taskRecord.projectKey

    val taskId get() = taskRecord.id

    val hour get() = scheduleJson.hour

    val minute get() = scheduleJson.minute

    val customTimeKey by lazy {
        scheduleJson.customTimeId?.let { taskRecord.getCustomTimeKey(it) }
    }
}
