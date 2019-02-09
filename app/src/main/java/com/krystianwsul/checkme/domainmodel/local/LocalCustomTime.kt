package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.CustomTime
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.RemotePrivateProject
import com.krystianwsul.checkme.firebase.records.RemoteSharedCustomTimeRecord
import com.krystianwsul.checkme.persistencemodel.LocalCustomTimeRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair
import java.util.*

class LocalCustomTime(
        private val domainFactory: DomainFactory,
        private val localCustomTimeRecord: LocalCustomTimeRecord) : CustomTime {

    val id get() = localCustomTimeRecord.id

    var current
        get() = localCustomTimeRecord.current
        set(value) {
            localCustomTimeRecord.current = value
        }

    override var name
        get() = localCustomTimeRecord.name
        set(value) {
            localCustomTimeRecord.name = value
    }

    override fun getHourMinute(dayOfWeek: DayOfWeek): HourMinute = when (dayOfWeek) {
        DayOfWeek.SUNDAY -> HourMinute(localCustomTimeRecord.sundayHour, localCustomTimeRecord.sundayMinute)
        DayOfWeek.MONDAY -> HourMinute(localCustomTimeRecord.mondayHour, localCustomTimeRecord.mondayMinute)
        DayOfWeek.TUESDAY -> HourMinute(localCustomTimeRecord.tuesdayHour, localCustomTimeRecord.tuesdayMinute)
        DayOfWeek.WEDNESDAY -> HourMinute(localCustomTimeRecord.wednesdayHour, localCustomTimeRecord.wednesdayMinute)
        DayOfWeek.THURSDAY -> HourMinute(localCustomTimeRecord.thursdayHour, localCustomTimeRecord.thursdayMinute)
        DayOfWeek.FRIDAY -> HourMinute(localCustomTimeRecord.fridayHour, localCustomTimeRecord.fridayMinute)
        DayOfWeek.SATURDAY -> HourMinute(localCustomTimeRecord.saturdayHour, localCustomTimeRecord.saturdayMinute)
    }

    override val hourMinutes
        get() = TreeMap<DayOfWeek, HourMinute>().apply {
            putAll(DayOfWeek.values().map { Pair(it, getHourMinute(it)) })
        }

    override fun setHourMinute(dayOfWeek: DayOfWeek, hourMinute: HourMinute) = localCustomTimeRecord.setHourMinute(dayOfWeek, hourMinute)

    override fun toString() = name

    override val timePair by lazy { TimePair(CustomTimeKey.LocalCustomTimeKey(localCustomTimeRecord.id), null) }

    fun delete() {
        domainFactory.localFactory.deleteCustomTime(this)

        localCustomTimeRecord.delete()
    }

    override val customTimeKey get() = CustomTimeKey.LocalCustomTimeKey(id)

    fun updateRemoteCustomTimeRecord(remoteCustomTimeRecord: RemoteSharedCustomTimeRecord, privateProject: RemotePrivateProject) {
        check(remoteCustomTimeRecord.localId == localCustomTimeRecord.id)

        val remotePrivateCustomTimeRecord = privateProject.getRemoteCustomTimeIfPresent(id)!!

        remoteCustomTimeRecord.ownerKey = privateProject.id
        remoteCustomTimeRecord.privateKey = remotePrivateCustomTimeRecord.id
    }
}
