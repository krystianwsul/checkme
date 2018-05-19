package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.ScheduleWrapper

abstract class RemoteScheduleRecord : RemoteRecord {

    companion object {

        const val SCHEDULES = "schedules"
    }

    val id: String

    private val remoteTaskRecord: RemoteTaskRecord

    final override val createObject: ScheduleWrapper

    override val key get() = remoteTaskRecord.key + "/" + SCHEDULES + "/" + id

    abstract val startTime: Long

    abstract val endTime: Long?

    val projectId get() = remoteTaskRecord.projectId

    val taskId get() = remoteTaskRecord.id

    constructor(id: String, remoteTaskRecord: RemoteTaskRecord, scheduleWrapper: ScheduleWrapper) : super(false) {
        this.id = id
        this.remoteTaskRecord = remoteTaskRecord
        this.createObject = scheduleWrapper
    }

    constructor(remoteTaskRecord: RemoteTaskRecord, scheduleWrapper: ScheduleWrapper) : super(true) {
        id = DatabaseWrapper.getScheduleRecordId(remoteTaskRecord.projectId, remoteTaskRecord.id)
        this.remoteTaskRecord = remoteTaskRecord
        this.createObject = scheduleWrapper
    }
}
