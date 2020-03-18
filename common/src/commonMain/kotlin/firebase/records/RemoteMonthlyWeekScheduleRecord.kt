package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId


class RemoteMonthlyWeekScheduleRecord<T : RemoteCustomTimeId, U : ProjectKey> : RemoteScheduleRecord<T, U> {

    private val monthlyWeekScheduleJson by lazy { createObject.monthlyWeekScheduleJson!! }

    val dayOfMonth by lazy { monthlyWeekScheduleJson.dayOfMonth }

    val dayOfWeek by lazy { monthlyWeekScheduleJson.dayOfWeek }

    val beginningOfMonth by lazy { monthlyWeekScheduleJson.beginningOfMonth }

    override val customTimeId by lazy { monthlyWeekScheduleJson.customTimeId?.let { remoteTaskRecord.getRemoteCustomTimeId(it) } }

    val hour by lazy { monthlyWeekScheduleJson.hour }

    val minute by lazy { monthlyWeekScheduleJson.minute }

    val from by lazy { monthlyWeekScheduleJson.from }
    val until by lazy { monthlyWeekScheduleJson.until }

    constructor(id: String, remoteTaskRecord: RemoteTaskRecord<T, U>, scheduleWrapper: ScheduleWrapper) : super(id, remoteTaskRecord, scheduleWrapper)

    constructor(remoteTaskRecord: RemoteTaskRecord<T, U>, scheduleWrapper: ScheduleWrapper) : super(remoteTaskRecord, scheduleWrapper)

    override val startTime by lazy { monthlyWeekScheduleJson.startTime }

    override var endTime by Committer(monthlyWeekScheduleJson::endTime, "$key/monthlyWeekScheduleJson")

    override fun deleteFromParent() = check(remoteTaskRecord.remoteMonthlyWeekScheduleRecords.remove(id) == this)
}
