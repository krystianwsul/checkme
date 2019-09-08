package com.krystianwsul.checkme.firebase.records


import com.krystianwsul.common.firebase.ScheduleWrapper
import com.krystianwsul.common.utils.RemoteCustomTimeId


class RemoteDailyScheduleRecord<T : RemoteCustomTimeId>(
        id: String,
        remoteTaskRecord: RemoteTaskRecord<T>,
        scheduleWrapper: ScheduleWrapper) : RemoteScheduleRecord<T>(id, remoteTaskRecord, scheduleWrapper) {

    private val dailyScheduleJson by lazy { scheduleWrapper.dailyScheduleJson!! }

    override val customTimeId by lazy { dailyScheduleJson.customTimeId?.let { remoteTaskRecord.getRemoteCustomTimeId(it) } }

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

    override fun deleteFromParent() = check(remoteTaskRecord.remoteDailyScheduleRecords.remove(id) == this)
}
