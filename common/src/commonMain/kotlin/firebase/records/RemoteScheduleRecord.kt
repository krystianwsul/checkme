package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleJson
import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

abstract class RemoteScheduleRecord<T : RemoteCustomTimeId, U : ProjectKey>(
        create: Boolean,
        val id: String,
        protected val remoteTaskRecord: RemoteTaskRecord<T, U>,
        final override val createObject: ScheduleWrapper,
        private val scheduleJson: ScheduleJson
) : RemoteRecord(create) {

    companion object {

        const val SCHEDULES = "schedules"
    }

    final override val key get() = remoteTaskRecord.key + "/" + SCHEDULES + "/" + id

    val startTime get() = scheduleJson.startTime

    abstract var endTime: Long? // todo instances get key for this

    val projectId get() = remoteTaskRecord.projectId

    val taskId get() = remoteTaskRecord.id

    val hour get() = scheduleJson.hour

    val minute get() = scheduleJson.minute

    val customTimeKey by lazy {
        scheduleJson.customTimeId?.let { remoteTaskRecord.getRemoteCustomTimeKey(it) }
    }

    constructor(
            id: String,
            remoteTaskRecord: RemoteTaskRecord<T, U>,
            scheduleWrapper: ScheduleWrapper,
            scheduleJson: ScheduleJson
    ) : this(
            false,
            id,
            remoteTaskRecord,
            scheduleWrapper,
            scheduleJson
    )

    constructor(
            remoteTaskRecord: RemoteTaskRecord<T, U>,
            scheduleWrapper: ScheduleWrapper,
            scheduleJson: ScheduleJson
    ) : this(
            true,
            remoteTaskRecord.getScheduleRecordId(),
            remoteTaskRecord,
            scheduleWrapper,
            scheduleJson
    )
}
