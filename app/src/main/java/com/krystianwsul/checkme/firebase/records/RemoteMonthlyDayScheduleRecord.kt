package com.krystianwsul.checkme.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.RemoteCustomTimeId


class RemoteMonthlyDayScheduleRecord<T : RemoteCustomTimeId> : RemoteScheduleRecord<T> {

    private val monthlyDayScheduleJson by lazy { createObject.monthlyDayScheduleJson!! }

    val dayOfMonth by lazy { monthlyDayScheduleJson.dayOfMonth }

    val beginningOfMonth by lazy { monthlyDayScheduleJson.beginningOfMonth }

    override val customTimeId by lazy { monthlyDayScheduleJson.customTimeId?.let { remoteTaskRecord.getRemoteCustomTimeId(it) } }

    val hour by lazy { monthlyDayScheduleJson.hour }

    val minute by lazy { monthlyDayScheduleJson.minute }

    constructor(id: String, remoteTaskRecord: RemoteTaskRecord<T>, scheduleWrapper: ScheduleWrapper) : super(id, remoteTaskRecord, scheduleWrapper)

    constructor(remoteTaskRecord: RemoteTaskRecord<T>, scheduleWrapper: ScheduleWrapper) : super(remoteTaskRecord, scheduleWrapper)

    override val startTime by lazy { monthlyDayScheduleJson.startTime }

    override var endTime
        get() = monthlyDayScheduleJson.endTime
        set(value) {
            if (value == monthlyDayScheduleJson.endTime)
                return

            monthlyDayScheduleJson.endTime = value
            addValue("$key/monthlyDayScheduleJson/endTime", value)
        }

    override fun deleteFromParent() = check(remoteTaskRecord.remoteMonthlyDayScheduleRecords.remove(id) == this)
}
