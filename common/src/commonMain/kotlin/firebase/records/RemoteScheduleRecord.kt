package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.RemoteCustomTimeId

abstract class RemoteScheduleRecord<T : RemoteCustomTimeId> : RemoteRecord {

    companion object {

        const val SCHEDULES = "schedules"
    }

    val id: String

    protected val remoteTaskRecord: RemoteTaskRecord<T>

    final override val createObject: ScheduleWrapper

    override val key get() = remoteTaskRecord.key + "/" + SCHEDULES + "/" + id

    abstract val startTime: Long

    abstract var endTime: Long?

    val projectId get() = remoteTaskRecord.projectId

    val taskId get() = remoteTaskRecord.id

    abstract val customTimeId: T?

    constructor(id: String, remoteTaskRecord: RemoteTaskRecord<T>, scheduleWrapper: ScheduleWrapper) : super(false) {
        this.id = id
        this.remoteTaskRecord = remoteTaskRecord
        this.createObject = scheduleWrapper
    }

    constructor(remoteTaskRecord: RemoteTaskRecord<T>, scheduleWrapper: ScheduleWrapper) : super(true) {
        id = remoteTaskRecord.getScheduleRecordId()
        this.remoteTaskRecord = remoteTaskRecord
        this.createObject = scheduleWrapper
    }
}
