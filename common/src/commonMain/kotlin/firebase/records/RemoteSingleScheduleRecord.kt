package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType


class RemoteSingleScheduleRecord<T : ProjectType> : RemoteScheduleRecord<T> {

    private val singleScheduleJson by lazy { createObject.singleScheduleJson!! }

    val year by lazy { singleScheduleJson.year }
    val month by lazy { singleScheduleJson.month }
    val day by lazy { singleScheduleJson.day }

    constructor(
            id: String,
            remoteTaskRecord: RemoteTaskRecord<T>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            id,
            remoteTaskRecord,
            scheduleWrapper,
            scheduleWrapper.singleScheduleJson!!,
            "singleScheduleJson"
    )

    constructor(
            remoteTaskRecord: RemoteTaskRecord<T>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            remoteTaskRecord,
            scheduleWrapper,
            scheduleWrapper.singleScheduleJson!!,
            "singleScheduleJson"
    )

    override fun deleteFromParent() = check(remoteTaskRecord.remoteSingleScheduleRecords.remove(id) == this)
}
