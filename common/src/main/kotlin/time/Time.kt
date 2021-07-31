package com.krystianwsul.common.time

import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.records.customtime.CustomTimeRecord
import com.krystianwsul.common.firebase.records.customtime.ProjectCustomTimeRecord
import com.krystianwsul.common.firebase.records.customtime.UserCustomTimeRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.Endable
import com.krystianwsul.common.utils.ProjectType

sealed class Time {

    abstract val timePair: TimePair

    abstract fun getHourMinute(dayOfWeek: DayOfWeek): HourMinute

    abstract override fun toString(): String

    abstract override fun hashCode(): Int

    abstract override fun equals(other: Any?): Boolean

    data class Normal(val hourMinute: HourMinute) : Time() {

        override val timePair get() = TimePair(null, hourMinute)

        constructor(hour: Int, minute: Int) : this(HourMinute(hour, minute))

        override fun getHourMinute(dayOfWeek: DayOfWeek) = hourMinute

        override fun toString() = hourMinute.toString()
    }

    sealed class Custom : Time(), CustomTimeProperties {

        abstract val customTimeRecord: CustomTimeRecord

        abstract val id: CustomTimeId

        override val name get() = customTimeRecord.name

        override val hourMinutes get() = DayOfWeek.values().associate { it to getHourMinute(it) }

        override val timePair by lazy { TimePair(key, null) }// possibly should get local key from DomainFactory (instead I have to do it in RemoteInstance)

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

        override fun hashCode() = id.hashCode()

        override fun equals(other: Any?): Boolean {
            if (other !is Custom) return false

            return id == other.id
        }

        abstract class Project<T : ProjectType> : Custom() {

            protected abstract val project: com.krystianwsul.common.firebase.models.project.Project<T>

            abstract override val customTimeRecord: ProjectCustomTimeRecord<T>

            abstract override val id: CustomTimeId.Project

            abstract override val key: CustomTimeKey.Project<T>

            val projectId by lazy { project.projectKey }

            override fun delete() {
                project.deleteCustomTime(this)

                customTimeRecord.delete()
            }
        }

        open class User(
            val user: RootUser,
            final override val customTimeRecord: UserCustomTimeRecord,
        ) : Custom(), Endable {

            override val id = customTimeRecord.id

            override val key = customTimeRecord.customTimeKey

            override val endExactTimeStamp get() = customTimeRecord.endTime?.let { ExactTimeStamp.Local(it) }

            override fun delete() {
                user.deleteCustomTime(this)

                customTimeRecord.delete()
            }
        }
    }
}
