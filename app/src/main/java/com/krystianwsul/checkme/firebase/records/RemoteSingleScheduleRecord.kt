package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.json.ScheduleWrapper


class RemoteSingleScheduleRecord : RemoteScheduleRecord {

    private val singleScheduleJson by lazy { createObject.singleScheduleJson!! }

    override val startTime by lazy { singleScheduleJson.startTime }

    override var endTime
        get() = singleScheduleJson.endTime
        set(value) {
            if (value == singleScheduleJson.endTime)
                return

            singleScheduleJson.endTime = value
            addValue("$key/singleScheduleJson/endTime", value)
        }

    val year by lazy { singleScheduleJson.year }

    val month by lazy { singleScheduleJson.month }

    val day by lazy { singleScheduleJson.day }

    override val customTimeId by lazy { singleScheduleJson.customTimeId }

    val hour by lazy { singleScheduleJson.hour }

    val minute by lazy { singleScheduleJson.minute }

    constructor(id: String, remoteTaskRecord: RemoteTaskRecord, scheduleWrapper: ScheduleWrapper) : super(id, remoteTaskRecord, scheduleWrapper)

    constructor(remoteTaskRecord: RemoteTaskRecord, scheduleWrapper: ScheduleWrapper) : super(remoteTaskRecord, scheduleWrapper)

    override fun deleteFromParent() = check(remoteTaskRecord.remoteSingleScheduleRecords.remove(id) == this)
}
