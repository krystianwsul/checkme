package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId


class RemoteSingleScheduleRecord<T : RemoteCustomTimeId, U : ProjectKey> : RemoteScheduleRecord<T, U> {

    private val singleScheduleJson by lazy { createObject.singleScheduleJson!! }

    val year by lazy { singleScheduleJson.year }
    val month by lazy { singleScheduleJson.month }
    val day by lazy { singleScheduleJson.day }

    constructor(
            id: String,
            remoteTaskRecord: RemoteTaskRecord<T, U>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            id,
            remoteTaskRecord,
            scheduleWrapper,
            scheduleWrapper.singleScheduleJson!!,
            "singleScheduleJson"
    )

    constructor(
            remoteTaskRecord: RemoteTaskRecord<T, U>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            remoteTaskRecord,
            scheduleWrapper,
            scheduleWrapper.singleScheduleJson!!,
            "singleScheduleJson"
    )

    override fun deleteFromParent() = check(remoteTaskRecord.remoteSingleScheduleRecords.remove(id) == this)
}
