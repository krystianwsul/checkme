package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleJson
import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType

abstract class RemoteScheduleRecord<T : ProjectType>(
        create: Boolean,
        val id: String,
        protected val remoteTaskRecord: RemoteTaskRecord<T>,
        final override val createObject: ScheduleWrapper,
        private val scheduleJson: ScheduleJson,
        endTimeKey: String
) : RemoteRecord(create) {

    companion object {

        const val SCHEDULES = "schedules"
    }

    final override val key get() = remoteTaskRecord.key + "/" + SCHEDULES + "/" + id

    val startTime get() = scheduleJson.startTime

    var endTime by Committer(scheduleJson::endTime, "$key/$endTimeKey")

    val projectId get() = remoteTaskRecord.projectId

    val taskId get() = remoteTaskRecord.id

    val hour get() = scheduleJson.hour

    val minute get() = scheduleJson.minute

    val customTimeKey by lazy {
        scheduleJson.customTimeId?.let { remoteTaskRecord.getRemoteCustomTimeKey(it) }
    }

    constructor(
            id: String,
            remoteTaskRecord: RemoteTaskRecord<T>,
            scheduleWrapper: ScheduleWrapper,
            scheduleJson: ScheduleJson,
            endTimeKey: String
    ) : this(
            false,
            id,
            remoteTaskRecord,
            scheduleWrapper,
            scheduleJson,
            endTimeKey
    )

    constructor(
            remoteTaskRecord: RemoteTaskRecord<T>,
            scheduleWrapper: ScheduleWrapper,
            scheduleJson: ScheduleJson,
            endTimeKey: String
    ) : this(
            true,
            remoteTaskRecord.getScheduleRecordId(),
            remoteTaskRecord,
            scheduleWrapper,
            scheduleJson,
            endTimeKey
    )
}
