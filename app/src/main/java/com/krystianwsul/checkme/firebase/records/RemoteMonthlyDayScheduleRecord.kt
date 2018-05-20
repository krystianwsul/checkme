package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.json.ScheduleWrapper
import junit.framework.Assert

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

    override val endTime get() = monthlyDayScheduleJson.endTime

    fun setEndTime(endTime: Long) {
        Assert.assertTrue(monthlyDayScheduleJson.endTime == null)

        if (endTime == monthlyDayScheduleJson.endTime)
            return

        monthlyDayScheduleJson.endTime = endTime
        addValue("$key/monthlyDayScheduleJson/endTime", endTime)
    }
}
