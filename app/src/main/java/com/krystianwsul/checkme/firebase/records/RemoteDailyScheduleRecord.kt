package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.json.ScheduleWrapper


class RemoteDailyScheduleRecord(id: String, remoteTaskRecord: RemoteTaskRecord, scheduleWrapper: ScheduleWrapper) : RemoteScheduleRecord(id, remoteTaskRecord, scheduleWrapper) {

    private val dailyScheduleJson by lazy { scheduleWrapper.dailyScheduleJson!! }

    val customTimeId by lazy { dailyScheduleJson.customTimeId }

    val hour by lazy { dailyScheduleJson.hour }

    val minute by lazy { dailyScheduleJson.minute }

    override val startTime by lazy { dailyScheduleJson.startTime }

    override var endTime
        get() = dailyScheduleJson.endTime
        set(value) {
            if (value == dailyScheduleJson.endTime)
                return

            dailyScheduleJson.endTime = value
            addValue("$key/dailyScheduleJson/endTime", value)
        }
}
