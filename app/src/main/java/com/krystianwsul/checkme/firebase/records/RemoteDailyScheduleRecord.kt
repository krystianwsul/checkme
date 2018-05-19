package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.json.ScheduleWrapper

import junit.framework.Assert

class RemoteDailyScheduleRecord(id: String, remoteTaskRecord: RemoteTaskRecord, scheduleWrapper: ScheduleWrapper) : RemoteScheduleRecord(id, remoteTaskRecord, scheduleWrapper) {

    private val dailyScheduleJson by lazy { scheduleWrapper.dailyScheduleJson!! }

    val customTimeId by lazy { dailyScheduleJson.customTimeId }

    val hour by lazy { dailyScheduleJson.hour }

    val minute by lazy { dailyScheduleJson.minute }

    override fun getStartTime() = dailyScheduleJson.startTime

    override fun getEndTime() = dailyScheduleJson.endTime

    fun setEndTime(endTime: Long) {
        Assert.assertTrue(getEndTime() == null)

        dailyScheduleJson.setEndTime(endTime)
        addValue("$key/dailyScheduleJson/endTime", endTime)
    }
}
