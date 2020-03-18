package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId


class RemoteWeeklyScheduleRecord<T : RemoteCustomTimeId, U : ProjectKey> : RemoteScheduleRecord<T, U> {

    private val weeklyScheduleJson by lazy { createObject.weeklyScheduleJson!! }

    override var endTime by Committer(weeklyScheduleJson::endTime, "$key/weeklyScheduleJson")

    val dayOfWeek by lazy { weeklyScheduleJson.dayOfWeek }

    val from by lazy { weeklyScheduleJson.from }
    val until by lazy { weeklyScheduleJson.until }

    constructor(
            id: String,
            remoteTaskRecord: RemoteTaskRecord<T, U>,
            scheduleWrapper: ScheduleWrapper
    ) : super(id, remoteTaskRecord, scheduleWrapper, scheduleWrapper.weeklyScheduleJson!!)

    constructor(
            remoteTaskRecord: RemoteTaskRecord<T, U>,
            scheduleWrapper: ScheduleWrapper
    ) : super(remoteTaskRecord, scheduleWrapper, scheduleWrapper.weeklyScheduleJson!!)

    override fun deleteFromParent() = check(remoteTaskRecord.remoteWeeklyScheduleRecords.remove(id) == this)
}
