package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.domain.schedules.ScheduleBridge
import com.krystianwsul.common.firebase.json.ScheduleJson
import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleId
import com.krystianwsul.common.utils.TaskKey

abstract class ScheduleRecord<T : ProjectType>(
        protected val taskRecord: TaskRecord<T>,
        final override val createObject: ScheduleWrapper,
        private val scheduleJson: ScheduleJson,
        endTimeKey: String,
        _id: String?
) : RemoteRecord(_id == null), ScheduleBridge<T> {

    companion object {

        const val SCHEDULES = "schedules"
    }

    val id = _id ?: taskRecord.getScheduleRecordId()

    final override val key get() = taskRecord.key + "/" + SCHEDULES + "/" + id

    override val startTime get() = scheduleJson.startTime

    override var endTime by Committer(scheduleJson::endTime, "$key/$endTimeKey")

    val projectKey = taskRecord.projectKey

    val taskId = taskRecord.id

    override val hour = scheduleJson.hour

    override val minute = scheduleJson.minute

    override val customTimeKey by lazy {
        scheduleJson.customTimeId?.let { taskRecord.getCustomTimeKey(it) }
    }

    final override val rootTaskKey by lazy { TaskKey(projectKey, taskId) }

    final override val scheduleId by lazy { ScheduleId(projectKey, taskId, id) }
}
