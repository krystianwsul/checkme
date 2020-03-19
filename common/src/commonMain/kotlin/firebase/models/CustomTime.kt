package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.CustomTimeRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType

abstract class CustomTime<T : ProjectType> : Time {

    protected abstract val project: Project<T>

    abstract val customTimeRecord: CustomTimeRecord<T>

    abstract val id: CustomTimeId<T>

    abstract val key: CustomTimeKey<T>

    val name get() = customTimeRecord.name

    val hourMinutes
        get() = DayOfWeek.values()
                .map { it to getHourMinute(it) }
                .toMap()

    override val timePair by lazy { TimePair(key, null) }// possibly should get local key from DomainFactory (instead I have to do it in RemoteInstance)

    val projectId by lazy { project.id }

    override fun getHourMinute(dayOfWeek: DayOfWeek) = when (dayOfWeek) {
        DayOfWeek.SUNDAY -> HourMinute(customTimeRecord.sundayHour, customTimeRecord.sundayMinute)
        DayOfWeek.MONDAY -> HourMinute(customTimeRecord.mondayHour, customTimeRecord.mondayMinute)
        DayOfWeek.TUESDAY -> HourMinute(customTimeRecord.tuesdayHour, customTimeRecord.tuesdayMinute)
        DayOfWeek.WEDNESDAY -> HourMinute(customTimeRecord.wednesdayHour, customTimeRecord.wednesdayMinute)
        DayOfWeek.THURSDAY -> HourMinute(customTimeRecord.thursdayHour, customTimeRecord.thursdayMinute)
        DayOfWeek.FRIDAY -> HourMinute(customTimeRecord.fridayHour, customTimeRecord.fridayMinute)
        DayOfWeek.SATURDAY -> HourMinute(customTimeRecord.saturdayHour, customTimeRecord.saturdayMinute)
    }

    override fun toString() = name

    abstract fun delete()
}
