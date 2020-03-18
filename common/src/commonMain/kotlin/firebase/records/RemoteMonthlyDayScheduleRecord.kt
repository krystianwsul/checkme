package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId


class RemoteMonthlyDayScheduleRecord<T : RemoteCustomTimeId, U : ProjectKey> : RemoteScheduleRecord<T, U> {

    private val monthlyDayScheduleJson by lazy { createObject.monthlyDayScheduleJson!! }

    val dayOfMonth by lazy { monthlyDayScheduleJson.dayOfMonth }

    val beginningOfMonth by lazy { monthlyDayScheduleJson.beginningOfMonth }

    val from by lazy { monthlyDayScheduleJson.from }
    val until by lazy { monthlyDayScheduleJson.until }

    constructor(
            id: String,
            remoteTaskRecord: RemoteTaskRecord<T, U>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            id,
            remoteTaskRecord,
            scheduleWrapper,
            scheduleWrapper.monthlyDayScheduleJson!!,
            "monthlyDayScheduleJson"
    )

    constructor(
            remoteTaskRecord: RemoteTaskRecord<T, U>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            remoteTaskRecord,
            scheduleWrapper,
            scheduleWrapper.monthlyDayScheduleJson!!,
            "monthlyDayScheduleJson"
    )

    override fun deleteFromParent() = check(remoteTaskRecord.remoteMonthlyDayScheduleRecords.remove(id) == this)
}
