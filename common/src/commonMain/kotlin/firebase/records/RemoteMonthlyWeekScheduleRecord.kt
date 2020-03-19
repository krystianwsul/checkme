package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey


class RemoteMonthlyWeekScheduleRecord<T : CustomTimeId, U : ProjectKey> : RemoteScheduleRecord<T, U> {

    private val monthlyWeekScheduleJson by lazy { createObject.monthlyWeekScheduleJson!! }

    val dayOfMonth by lazy { monthlyWeekScheduleJson.dayOfMonth }

    val dayOfWeek by lazy { monthlyWeekScheduleJson.dayOfWeek }

    val beginningOfMonth by lazy { monthlyWeekScheduleJson.beginningOfMonth }

    val from by lazy { monthlyWeekScheduleJson.from }
    val until by lazy { monthlyWeekScheduleJson.until }

    constructor(
            id: String,
            remoteTaskRecord: RemoteTaskRecord<T, U>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            id,
            remoteTaskRecord,
            scheduleWrapper,
            scheduleWrapper.monthlyWeekScheduleJson!!,
            "monthlyWeekScheduleJson"
    )

    constructor(
            remoteTaskRecord: RemoteTaskRecord<T, U>,
            scheduleWrapper: ScheduleWrapper
    ) : super(
            remoteTaskRecord,
            scheduleWrapper,
            scheduleWrapper.monthlyWeekScheduleJson!!,
            "monthlyWeekScheduleJson"
    )

    override fun deleteFromParent() = check(remoteTaskRecord.remoteMonthlyWeekScheduleRecords.remove(id) == this)
}
