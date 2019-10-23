package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.RemoteCustomTimeId


class RemoteMonthlyWeekScheduleRecord<T : RemoteCustomTimeId> : RemoteScheduleRecord<T> {

    private val monthlyWeekScheduleJson by lazy { createObject.monthlyWeekScheduleJson!! }

    val dayOfMonth by lazy { monthlyWeekScheduleJson.dayOfMonth }

    val dayOfWeek by lazy { monthlyWeekScheduleJson.dayOfWeek }

    val beginningOfMonth by lazy { monthlyWeekScheduleJson.beginningOfMonth }

    override val customTimeId by lazy { monthlyWeekScheduleJson.customTimeId?.let { remoteTaskRecord.getRemoteCustomTimeId(it) } }

    val hour by lazy { monthlyWeekScheduleJson.hour }

    val minute by lazy { monthlyWeekScheduleJson.minute }

    var from by Committer(monthlyWeekScheduleJson::from)
    var until by Committer(monthlyWeekScheduleJson::until)

    constructor(id: String, remoteTaskRecord: RemoteTaskRecord<T>, scheduleWrapper: ScheduleWrapper) : super(id, remoteTaskRecord, scheduleWrapper)

    constructor(remoteTaskRecord: RemoteTaskRecord<T>, scheduleWrapper: ScheduleWrapper) : super(remoteTaskRecord, scheduleWrapper)

    override val startTime by lazy { monthlyWeekScheduleJson.startTime }

    override var endTime by Committer(monthlyWeekScheduleJson::endTime, "$key/monthlyWeekScheduleJson")

    override fun deleteFromParent() = check(remoteTaskRecord.remoteMonthlyWeekScheduleRecords.remove(id) == this)
}
