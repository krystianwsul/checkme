package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.RemoteCustomTimeId


class RemoteDailyScheduleRecord<T : RemoteCustomTimeId>(
        id: String,
        remoteTaskRecord: RemoteTaskRecord<T>,
        scheduleWrapper: ScheduleWrapper
) : RemoteScheduleRecord<T>(id, remoteTaskRecord, scheduleWrapper) {

    private val dailyScheduleJson by lazy { scheduleWrapper.dailyScheduleJson!! }

    override val customTimeId by lazy { dailyScheduleJson.customTimeId?.let { remoteTaskRecord.getRemoteCustomTimeId(it) } }

    val hour by lazy { dailyScheduleJson.hour }

    val minute by lazy { dailyScheduleJson.minute }

    override val startTime by lazy { dailyScheduleJson.startTime }

    override var endTime by Committer(dailyScheduleJson::endTime, "$key/dailyScheduleJson")

    override fun deleteFromParent() = check(remoteTaskRecord.remoteDailyScheduleRecords.remove(id) == this)
}
