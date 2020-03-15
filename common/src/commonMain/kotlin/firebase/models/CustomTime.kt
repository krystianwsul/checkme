package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.RemoteCustomTimeRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

abstract class CustomTime<T : RemoteCustomTimeId, U : ProjectKey> : Time {

    protected abstract val project: Project<T, U>

    abstract val remoteCustomTimeRecord: RemoteCustomTimeRecord<T, *>

    abstract val id: T

    abstract val customTimeKey: CustomTimeKey<*, *>

    val name get() = remoteCustomTimeRecord.name

    val hourMinutes
        get() = DayOfWeek.values()
                .map { it to getHourMinute(it) }
                .toMap()

    override val timePair by lazy { TimePair(customTimeKey, null) }// possibly should get local key from DomainFactory (instead I have to do it in RemoteInstance)

    val projectId by lazy { project.id }

    override fun getHourMinute(dayOfWeek: DayOfWeek) = when (dayOfWeek) {
        DayOfWeek.SUNDAY -> HourMinute(remoteCustomTimeRecord.sundayHour, remoteCustomTimeRecord.sundayMinute)
        DayOfWeek.MONDAY -> HourMinute(remoteCustomTimeRecord.mondayHour, remoteCustomTimeRecord.mondayMinute)
        DayOfWeek.TUESDAY -> HourMinute(remoteCustomTimeRecord.tuesdayHour, remoteCustomTimeRecord.tuesdayMinute)
        DayOfWeek.WEDNESDAY -> HourMinute(remoteCustomTimeRecord.wednesdayHour, remoteCustomTimeRecord.wednesdayMinute)
        DayOfWeek.THURSDAY -> HourMinute(remoteCustomTimeRecord.thursdayHour, remoteCustomTimeRecord.thursdayMinute)
        DayOfWeek.FRIDAY -> HourMinute(remoteCustomTimeRecord.fridayHour, remoteCustomTimeRecord.fridayMinute)
        DayOfWeek.SATURDAY -> HourMinute(remoteCustomTimeRecord.saturdayHour, remoteCustomTimeRecord.saturdayMinute)
    }

    override fun toString() = name

    abstract fun delete()
}
