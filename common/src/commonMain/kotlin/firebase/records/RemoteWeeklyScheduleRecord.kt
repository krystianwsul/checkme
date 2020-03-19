package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey


class RemoteWeeklyScheduleRecord<T : CustomTimeId, U : ProjectKey> : RemoteScheduleRecord<T, U> {

    private val weeklyScheduleJson by lazy { createObject.weeklyScheduleJson!! }

    val dayOfWeek by lazy { weeklyScheduleJson.dayOfWeek }

    val from by lazy { weeklyScheduleJson.from }
    val until by lazy { weeklyScheduleJson.until }

    constructor(
            id: String,
            remoteTaskRecord: RemoteTaskRecord<T, U>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            id,
            remoteTaskRecord,
            scheduleWrapper,
            scheduleWrapper.weeklyScheduleJson!!,
            "weeklyScheduleJson"
    )

    constructor(
            remoteTaskRecord: RemoteTaskRecord<T, U>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            remoteTaskRecord,
            scheduleWrapper,
            scheduleWrapper.weeklyScheduleJson!!,
            "weeklyScheduleJson"
    )

    override fun deleteFromParent() = check(remoteTaskRecord.remoteWeeklyScheduleRecords.remove(id) == this)
}
