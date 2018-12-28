package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.json.ScheduleWrapper


class RemoteMonthlyDayScheduleRecord : RemoteScheduleRecord {

    private val monthlyDayScheduleJson by lazy { createObject.monthlyDayScheduleJson!! }

    val dayOfMonth by lazy { monthlyDayScheduleJson.dayOfMonth }

    val beginningOfMonth by lazy { monthlyDayScheduleJson.beginningOfMonth }

    val customTimeId by lazy { monthlyDayScheduleJson.customTimeId }

    val hour by lazy { monthlyDayScheduleJson.hour }

    val minute by lazy { monthlyDayScheduleJson.minute }

    constructor(id: String, remoteTaskRecord: RemoteTaskRecord, scheduleWrapper: ScheduleWrapper) : super(id, remoteTaskRecord, scheduleWrapper) {}

    constructor(remoteTaskRecord: RemoteTaskRecord, scheduleWrapper: ScheduleWrapper) : super(remoteTaskRecord, scheduleWrapper) {}

    override val startTime by lazy { monthlyDayScheduleJson.startTime }

    override var endTime
        get() = monthlyDayScheduleJson.endTime
        set(value) {
            if (value == monthlyDayScheduleJson.endTime)
                return

            monthlyDayScheduleJson.endTime = value
            addValue("$key/monthlyDayScheduleJson/endTime", value)
        }
}
