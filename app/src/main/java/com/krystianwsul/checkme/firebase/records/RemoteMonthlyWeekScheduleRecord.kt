package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.json.ScheduleWrapper
import junit.framework.Assert

class RemoteMonthlyWeekScheduleRecord : RemoteScheduleRecord {

    private val monthlyWeekScheduleJson by lazy { createObject.monthlyWeekScheduleJson!! }

    val dayOfMonth by lazy { monthlyWeekScheduleJson.dayOfMonth }

    val dayOfWeek by lazy { monthlyWeekScheduleJson.dayOfWeek }

    val beginningOfMonth by lazy { monthlyWeekScheduleJson.beginningOfMonth }

    val customTimeId by lazy { monthlyWeekScheduleJson.customTimeId }

    val hour by lazy { monthlyWeekScheduleJson.hour }

    val minute by lazy { monthlyWeekScheduleJson.minute }

    constructor(id: String, remoteTaskRecord: RemoteTaskRecord, scheduleWrapper: ScheduleWrapper) : super(id, remoteTaskRecord, scheduleWrapper)

    constructor(remoteTaskRecord: RemoteTaskRecord, scheduleWrapper: ScheduleWrapper) : super(remoteTaskRecord, scheduleWrapper)

    override val startTime by lazy { monthlyWeekScheduleJson.startTime }

    override val endTime get() = monthlyWeekScheduleJson.endTime

    fun setEndTime(endTime: Long) {
        Assert.assertTrue(monthlyWeekScheduleJson.endTime == null)

        monthlyWeekScheduleJson.endTime = endTime
        addValue("$key/monthlyWeekScheduleJson/endTime", endTime)
    }
}
