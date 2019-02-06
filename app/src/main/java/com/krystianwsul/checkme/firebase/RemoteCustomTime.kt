package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.CustomTime
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair
import java.util.*

abstract class RemoteCustomTime : CustomTime {

    protected abstract val remoteProject: RemoteProject

    abstract val remoteCustomTimeRecord: RemoteCustomTimeRecord

    val id by lazy { remoteCustomTimeRecord.id }

    override val name get() = remoteCustomTimeRecord.name

    override val hourMinutes
        get() = DayOfWeek.values()
                .map { it to getHourMinute(it) }
                .toMap(TreeMap())

    override val timePair by lazy { TimePair(customTimeKey, null) }// possibly should get local key from DomainFactory (instead I have to do it in RemoteInstance)

    override val customTimeKey by lazy { CustomTimeKey.RemoteCustomTimeKey(projectId, id) }

    val projectId by lazy { remoteProject.id }

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
