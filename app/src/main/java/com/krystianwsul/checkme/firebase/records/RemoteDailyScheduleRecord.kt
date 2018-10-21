package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.json.ScheduleWrapper



class RemoteDailyScheduleRecord(id: String, remoteTaskRecord: RemoteTaskRecord, scheduleWrapper: ScheduleWrapper) : RemoteScheduleRecord(id, remoteTaskRecord, scheduleWrapper) {

    private val dailyScheduleJson by lazy { scheduleWrapper.dailyScheduleJson!! }

    val customTimeId by lazy { dailyScheduleJson.customTimeId }

    val hour by lazy { dailyScheduleJson.hour }

    val minute by lazy { dailyScheduleJson.minute }

    override val startTime by lazy { dailyScheduleJson.startTime }

    override val endTime get() = dailyScheduleJson.endTime

    fun setEndTime(endTime: Long) {
        check(dailyScheduleJson.endTime == null)

        if (endTime == dailyScheduleJson.endTime)
            return

        dailyScheduleJson.endTime = endTime
        addValue("$key/dailyScheduleJson/endTime", endTime)
    }
}
