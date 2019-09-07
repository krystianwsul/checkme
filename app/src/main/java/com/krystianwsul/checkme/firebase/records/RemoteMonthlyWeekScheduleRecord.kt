package com.krystianwsul.checkme.firebase.records


import com.krystianwsul.common.firebase.ScheduleWrapper
import com.krystianwsul.common.utils.RemoteCustomTimeId


class RemoteMonthlyWeekScheduleRecord<T : RemoteCustomTimeId> : RemoteScheduleRecord<T> {

    private val monthlyWeekScheduleJson by lazy { createObject.monthlyWeekScheduleJson!! }

    val dayOfMonth by lazy { monthlyWeekScheduleJson.dayOfMonth }

    val dayOfWeek by lazy { monthlyWeekScheduleJson.dayOfWeek }

    val beginningOfMonth by lazy { monthlyWeekScheduleJson.beginningOfMonth }

    override val customTimeId by lazy { monthlyWeekScheduleJson.customTimeId?.let { remoteTaskRecord.getRemoteCustomTimeId(it) } }

    val hour by lazy { monthlyWeekScheduleJson.hour }

    val minute by lazy { monthlyWeekScheduleJson.minute }

    constructor(id: String, remoteTaskRecord: RemoteTaskRecord<T>, scheduleWrapper: ScheduleWrapper) : super(id, remoteTaskRecord, scheduleWrapper)

    constructor(remoteTaskRecord: RemoteTaskRecord<T>, scheduleWrapper: ScheduleWrapper) : super(remoteTaskRecord, scheduleWrapper)

    override val startTime by lazy { monthlyWeekScheduleJson.startTime }

    override var endTime
        get() = monthlyWeekScheduleJson.endTime
        set(value) {
            if (value == monthlyWeekScheduleJson.endTime)
            return

            monthlyWeekScheduleJson.endTime = value
            addValue("$key/monthlyWeekScheduleJson/endTime", value)
    }

    override fun deleteFromParent() = check(remoteTaskRecord.remoteMonthlyWeekScheduleRecords.remove(id) == this)
}
