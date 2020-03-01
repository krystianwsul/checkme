package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.RemoteCustomTimeId


class RemoteSingleScheduleRecord<T : RemoteCustomTimeId> : RemoteScheduleRecord<T> {

    private val singleScheduleJson by lazy { createObject.singleScheduleJson!! }

    override val startTime by lazy { singleScheduleJson.startTime }

    override var endTime by Committer(singleScheduleJson::endTime, "$key/singleScheduleJson")

    val year by lazy { singleScheduleJson.year }
    val month by lazy { singleScheduleJson.month }
    val day by lazy { singleScheduleJson.day }

    override val customTimeId by lazy {
        singleScheduleJson.customTimeId?.let { remoteTaskRecord.getRemoteCustomTimeId(it) }
    }

    val hour by lazy { singleScheduleJson.hour }
    val minute by lazy { singleScheduleJson.minute }

    constructor(
            id: String,
            remoteTaskRecord: RemoteTaskRecord<T, *>,
            scheduleWrapper: ScheduleWrapper
    ) : super(id, remoteTaskRecord, scheduleWrapper)

    constructor(
            remoteTaskRecord: RemoteTaskRecord<T, *>,
            scheduleWrapper: ScheduleWrapper
    ) : super(remoteTaskRecord, scheduleWrapper)

    override fun deleteFromParent() = check(remoteTaskRecord.remoteSingleScheduleRecords.remove(id) == this)
}
