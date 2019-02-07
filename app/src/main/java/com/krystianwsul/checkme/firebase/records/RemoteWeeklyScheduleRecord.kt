package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.json.ScheduleWrapper
import com.krystianwsul.checkme.utils.RemoteCustomTimeId


class RemoteWeeklyScheduleRecord<T : RemoteCustomTimeId> : RemoteScheduleRecord<T> {

    private val weeklyScheduleJson by lazy { createObject.weeklyScheduleJson!! }

    override val startTime by lazy { weeklyScheduleJson.startTime }

    override var endTime
        get() = weeklyScheduleJson.endTime
        set(value) {
            if (value == weeklyScheduleJson.endTime)
                return

            weeklyScheduleJson.endTime = value
            addValue("$key/weeklyScheduleJson/endTime", value)
        }

    val dayOfWeek by lazy { weeklyScheduleJson.dayOfWeek }

    override val customTimeId by lazy { weeklyScheduleJson.customTimeId?.let { remoteTaskRecord.getRemoteCustomTimeId(it) } }

    val hour by lazy { weeklyScheduleJson.hour }

    val minute by lazy { weeklyScheduleJson.minute }

    constructor(id: String, remoteTaskRecord: RemoteTaskRecord<T>, scheduleWrapper: ScheduleWrapper) : super(id, remoteTaskRecord, scheduleWrapper)

    constructor(remoteTaskRecord: RemoteTaskRecord<T>, scheduleWrapper: ScheduleWrapper) : super(remoteTaskRecord, scheduleWrapper)

    override fun deleteFromParent() = check(remoteTaskRecord.remoteWeeklyScheduleRecords.remove(id) == this)
}
