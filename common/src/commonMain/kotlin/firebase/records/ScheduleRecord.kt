package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleJson
import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType

abstract class ScheduleRecord<T : ProjectType>(
        create: Boolean,
        val id: String,
        protected val taskRecord: TaskRecord<T>,
        final override val createObject: ScheduleWrapper,
        private val scheduleJson: ScheduleJson,
        endTimeKey: String
) : RemoteRecord(create) {

    companion object {

        const val SCHEDULES = "schedules"
    }

    final override val key get() = taskRecord.key + "/" + SCHEDULES + "/" + id

    val startTime get() = scheduleJson.startTime

    var endTime by Committer(scheduleJson::endTime, "$key/$endTimeKey")

    val projectId get() = taskRecord.projectKey

    val taskId get() = taskRecord.id

    val hour get() = scheduleJson.hour

    val minute get() = scheduleJson.minute

    val customTimeKey by lazy {
        scheduleJson.customTimeId?.let { taskRecord.getCustomTimeKey(it) }
    }

    constructor(
            id: String,
            taskRecord: TaskRecord<T>,
            scheduleWrapper: ScheduleWrapper,
            scheduleJson: ScheduleJson,
            endTimeKey: String
    ) : this(
            false,
            id,
            taskRecord,
            scheduleWrapper,
            scheduleJson,
            endTimeKey
    )

    constructor(
            taskRecord: TaskRecord<T>,
            scheduleWrapper: ScheduleWrapper,
            scheduleJson: ScheduleJson,
            endTimeKey: String
    ) : this(
            true,
            taskRecord.getScheduleRecordId(),
            taskRecord,
            scheduleWrapper,
            scheduleJson,
            endTimeKey
    )
}
