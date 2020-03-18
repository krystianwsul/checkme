package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId


class RemoteDailyScheduleRecord<T : RemoteCustomTimeId, U : ProjectKey>(
        id: String,
        remoteTaskRecord: RemoteTaskRecord<T, U>,
        scheduleWrapper: ScheduleWrapper
) : RemoteScheduleRecord<T, U>(id, remoteTaskRecord, scheduleWrapper, scheduleWrapper.dailyScheduleJson!!) {

    private val dailyScheduleJson by lazy { scheduleWrapper.dailyScheduleJson!! }

    override var endTime by Committer(dailyScheduleJson::endTime, "$key/dailyScheduleJson")

    override fun deleteFromParent() = check(remoteTaskRecord.remoteDailyScheduleRecords.remove(id) == this)
}
