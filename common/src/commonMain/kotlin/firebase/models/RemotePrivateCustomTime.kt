package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.RemoteCustomTimeRecord
import com.krystianwsul.common.firebase.records.RemotePrivateCustomTimeRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemotePrivateCustomTime(
        override val project: PrivateProject,
        override val remoteCustomTimeRecord: RemotePrivateCustomTimeRecord
) : RemoteCustomTime<RemoteCustomTimeId.Private, ProjectKey.Private>() {

    override val id = remoteCustomTimeRecord.id

    override val customTimeKey by lazy { CustomTimeKey.Private(projectId, id) }

    private fun getAllRecords(allRecordsSource: AllRecordsSource) = allRecordsSource.getSharedCustomTimes(id)
            .map { (it as RemoteCustomTime<*, *>).remoteCustomTimeRecord }
            .toMutableList<RemoteCustomTimeRecord<*, *>>()
            .apply { add(remoteCustomTimeRecord) }

    fun current(exactTimeStamp: ExactTimeStamp): Boolean {
        val current = remoteCustomTimeRecord.current
        val endExactTimeStamp = endExactTimeStamp

        check(endExactTimeStamp == null || !current)

        return endExactTimeStamp?.let { it > exactTimeStamp } ?: current
    }

    var endExactTimeStamp
        get() = remoteCustomTimeRecord.endTime?.let { ExactTimeStamp(it) }
        set(value) {
            check((value == null) != (remoteCustomTimeRecord.endTime == null))

            remoteCustomTimeRecord.current = value == null
            remoteCustomTimeRecord.endTime = value?.long
        }

    override fun delete() {
        project.deleteCustomTime(this)

        remoteCustomTimeRecord.delete()
    }

    fun setHourMinute(
            allRecordsSource: AllRecordsSource,
            dayOfWeek: DayOfWeek,
            hourMinute: HourMinute
    ) = getAllRecords(allRecordsSource).forEach { it.setHourMinute(dayOfWeek, hourMinute) }

    fun setName(allRecordsSource: AllRecordsSource, name: String) = getAllRecords(allRecordsSource).forEach { it.name = name }

    interface AllRecordsSource {

        fun getSharedCustomTimes(privateCustomTimeId: RemoteCustomTimeId.Private): List<RemoteSharedCustomTime>
    }
}
