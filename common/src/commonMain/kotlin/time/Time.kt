package com.krystianwsul.common.time

import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.firebase.records.CustomTimeRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType

sealed class Time {

    abstract val timePair: TimePair

    abstract fun getHourMinute(dayOfWeek: DayOfWeek): HourMinute

    abstract override fun toString(): String

    data class Normal(val hourMinute: HourMinute) : Time() {

        override val timePair get() = TimePair(null, hourMinute)

        constructor(hour: Int, minute: Int) : this(HourMinute(hour, minute))

        override fun getHourMinute(dayOfWeek: DayOfWeek) = hourMinute

        override fun toString() = hourMinute.toString()
    }

    abstract class Custom<T : ProjectType> : Time() {

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
}
