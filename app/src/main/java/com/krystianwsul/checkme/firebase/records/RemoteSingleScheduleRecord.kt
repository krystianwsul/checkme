package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.json.ScheduleWrapper


class RemoteSingleScheduleRecord : RemoteScheduleRecord {

    private val singleScheduleJson by lazy { createObject.singleScheduleJson!! }

    override val startTime by lazy { singleScheduleJson.startTime }

    override val endTime get() = singleScheduleJson.endTime

    val year by lazy { singleScheduleJson.year }

    val month by lazy { singleScheduleJson.month }

    val day by lazy { singleScheduleJson.day }

    val customTimeId by lazy { singleScheduleJson.customTimeId }

    val hour by lazy { singleScheduleJson.hour }

    val minute by lazy { singleScheduleJson.minute }

    constructor(id: String, remoteTaskRecord: RemoteTaskRecord, scheduleWrapper: ScheduleWrapper) : super(id, remoteTaskRecord, scheduleWrapper)

    constructor(remoteTaskRecord: RemoteTaskRecord, scheduleWrapper: ScheduleWrapper) : super(remoteTaskRecord, scheduleWrapper)

    fun setEndTime(endTime: Long) {
        check(singleScheduleJson.endTime == null)

        if (endTime == singleScheduleJson.endTime)
            return

        singleScheduleJson.endTime = endTime
        addValue("$key/singleScheduleJson/endTime", endTime)
    }
}
