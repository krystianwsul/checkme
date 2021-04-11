package com.krystianwsul.common.time

import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.records.CustomTimeRecord
import com.krystianwsul.common.firebase.records.ProjectCustomTimeRecord
import com.krystianwsul.common.firebase.records.UserCustomTimeRecord
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

    sealed class Custom : Time() {

        abstract val customTimeRecord: CustomTimeRecord

        abstract val id: CustomTimeId

        abstract val key: CustomTimeKey

        val name get() = customTimeRecord.name

        val hourMinutes get() = DayOfWeek.values().associate { it to getHourMinute(it) }

        // todo customtime timepair
        override val timePair by lazy { TimePair(key as CustomTimeKey.Project<*>, null) }// possibly should get local key from DomainFactory (instead I have to do it in RemoteInstance)

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

        abstract class Project<T : ProjectType> : Custom() {

            protected abstract val project: com.krystianwsul.common.firebase.models.Project<T>

            abstract override val customTimeRecord: ProjectCustomTimeRecord<T>

            abstract override val id: CustomTimeId.Project<T>

            abstract override val key: CustomTimeKey.Project<T>

            val projectId by lazy { project.projectKey }

            override fun delete() {
                project.deleteCustomTime(this)

                customTimeRecord.delete()
            }
        }

        class User(private val user: RootUser, override val customTimeRecord: UserCustomTimeRecord) : Custom() {

            override val id = customTimeRecord.id

            override val key = customTimeRecord.customTimeKey

            override fun delete() {
                user.deleteCustomTime(this)

                customTimeRecord.delete()
            }
        }
    }
}
