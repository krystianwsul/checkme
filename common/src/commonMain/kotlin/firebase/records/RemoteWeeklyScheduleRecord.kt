package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType


class RemoteWeeklyScheduleRecord<T : ProjectType> : RemoteScheduleRecord<T> {

    private val weeklyScheduleJson by lazy { createObject.weeklyScheduleJson!! }

    val dayOfWeek by lazy { weeklyScheduleJson.dayOfWeek }

    val from by lazy { weeklyScheduleJson.from }
    val until by lazy { weeklyScheduleJson.until }

    constructor(
            id: String,
            remoteTaskRecord: RemoteTaskRecord<T>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            id,
            remoteTaskRecord,
            scheduleWrapper,
            scheduleWrapper.weeklyScheduleJson!!,
            "weeklyScheduleJson"
    )

    constructor(
            remoteTaskRecord: RemoteTaskRecord<T>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            remoteTaskRecord,
            scheduleWrapper,
            scheduleWrapper.weeklyScheduleJson!!,
            "weeklyScheduleJson"
    )

    override fun deleteFromParent() = check(remoteTaskRecord.remoteWeeklyScheduleRecords.remove(id) == this)
}
