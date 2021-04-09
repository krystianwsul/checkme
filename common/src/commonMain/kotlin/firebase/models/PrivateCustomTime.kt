package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.PrivateCustomTimeRecord
import com.krystianwsul.common.firebase.records.ProjectCustomTimeRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType

class PrivateCustomTime(
        override val project: PrivateProject,
        override val customTimeRecord: PrivateCustomTimeRecord
) : Time.Custom<ProjectType.Private>() {

    override val key = customTimeRecord.customTimeKey
    override val id = key.customTimeId

    private fun getAllRecords(allRecordsSource: AllRecordsSource) = allRecordsSource.getSharedCustomTimes(key)
            .map { (it as Custom<*>).customTimeRecord }
            .toMutableList<ProjectCustomTimeRecord<*>>()
            .apply { add(customTimeRecord) }

    fun current(exactTimeStamp: ExactTimeStamp.Local): Boolean {
        val current = customTimeRecord.current
        val endExactTimeStamp = endExactTimeStamp

        check(endExactTimeStamp == null || !current)

        return endExactTimeStamp?.let { it > exactTimeStamp } ?: current
    }

    var endExactTimeStamp
        get() = customTimeRecord.endTime?.let { ExactTimeStamp.Local(it) }
        set(value) {
            check((value == null) != (customTimeRecord.endTime == null))

            customTimeRecord.current = value == null
            customTimeRecord.endTime = value?.long
        }

    override fun delete() {
        project.deleteCustomTime(this)

        customTimeRecord.delete()
    }

    fun setHourMinute(
            allRecordsSource: AllRecordsSource,
            dayOfWeek: DayOfWeek,
            hourMinute: HourMinute
    ) = getAllRecords(allRecordsSource).forEach { it.setHourMinute(dayOfWeek, hourMinute) }

    fun setName(allRecordsSource: AllRecordsSource, name: String) = getAllRecords(allRecordsSource).forEach { it.name = name }

    interface AllRecordsSource {

        fun getSharedCustomTimes(customTimeKey: CustomTimeKey.Project.Private): List<SharedCustomTime>
    }
}
