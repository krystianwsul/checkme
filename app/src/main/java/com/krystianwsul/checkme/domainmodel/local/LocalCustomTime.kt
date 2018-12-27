package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.CustomTime
import com.krystianwsul.checkme.domainmodel.CustomTimeRecord
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord
import com.krystianwsul.checkme.persistencemodel.LocalCustomTimeRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair
import java.util.*

class LocalCustomTime(
        private val domainFactory: DomainFactory,
        private val localCustomTimeRecord: LocalCustomTimeRecord) : CustomTime {

    private val remoteCustomTimeRecords = HashMap<String, RemoteCustomTimeRecord>()

    val id get() = localCustomTimeRecord.id

    var current
        get() = localCustomTimeRecord.current
        set(value) {
            localCustomTimeRecord.current = value
        }

    private val customTimeRecords get() = mutableListOf<CustomTimeRecord>(localCustomTimeRecord).apply { addAll(remoteCustomTimeRecords.values) }

    override val name get() = localCustomTimeRecord.name

    fun setName(name: String) {
        check(name.isNotEmpty())
        localCustomTimeRecord.name = name

        remoteCustomTimeRecords.values.forEach { it.name = name }
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

    fun setHourMinute(dayOfWeek: DayOfWeek, hourMinute: HourMinute) {
        when (dayOfWeek) {
            DayOfWeek.SUNDAY -> {
                customTimeRecords.forEach {
                    it.sundayHour = hourMinute.hour
                    it.sundayMinute = hourMinute.minute
                }
            }
            DayOfWeek.MONDAY -> {
                customTimeRecords.forEach {
                    it.mondayHour = hourMinute.hour
                    it.mondayMinute = hourMinute.minute
                }
            }
            DayOfWeek.TUESDAY -> {
                customTimeRecords.forEach {
                    it.tuesdayHour = hourMinute.hour
                    it.tuesdayMinute = hourMinute.minute
                }
            }
            DayOfWeek.WEDNESDAY -> {
                customTimeRecords.forEach {
                    it.wednesdayHour = hourMinute.hour
                    it.wednesdayMinute = hourMinute.minute
                }
            }
            DayOfWeek.THURSDAY -> {
                customTimeRecords.forEach {
                    it.thursdayHour = hourMinute.hour
                    it.thursdayMinute = hourMinute.minute
                }
            }
            DayOfWeek.FRIDAY -> {
                customTimeRecords.forEach {
                    it.fridayHour = hourMinute.hour
                    it.fridayMinute = hourMinute.minute
                }
            }
            DayOfWeek.SATURDAY -> {
                customTimeRecords.forEach {
                    it.saturdayHour = hourMinute.hour
                    it.saturdayMinute = hourMinute.minute
                }
            }
        }
    }

    override fun toString() = name

    override val timePair by lazy { TimePair(CustomTimeKey.LocalCustomTimeKey(localCustomTimeRecord.id), null) }

    fun delete() {
        domainFactory.localFactory.deleteCustomTime(this)

        localCustomTimeRecord.delete()
    }

    override val customTimeKey get() = CustomTimeKey.LocalCustomTimeKey(id)

    fun addRemoteCustomTimeRecord(remoteCustomTimeRecord: RemoteCustomTimeRecord) {
        check(remoteCustomTimeRecord.localId == localCustomTimeRecord.id)
        check(!remoteCustomTimeRecords.containsKey(remoteCustomTimeRecord.projectId))

        remoteCustomTimeRecords[remoteCustomTimeRecord.projectId] = remoteCustomTimeRecord

        // bez zapisywania na razie, dopiero przy następnej okazji
        remoteCustomTimeRecord.name = localCustomTimeRecord.name

        remoteCustomTimeRecord.sundayHour = localCustomTimeRecord.sundayHour
        remoteCustomTimeRecord.sundayMinute = localCustomTimeRecord.sundayMinute

        remoteCustomTimeRecord.mondayHour = localCustomTimeRecord.mondayHour
        remoteCustomTimeRecord.mondayMinute = localCustomTimeRecord.mondayMinute

        remoteCustomTimeRecord.tuesdayHour = localCustomTimeRecord.tuesdayHour
        remoteCustomTimeRecord.tuesdayMinute = localCustomTimeRecord.tuesdayMinute

        remoteCustomTimeRecord.wednesdayHour = localCustomTimeRecord.wednesdayHour
        remoteCustomTimeRecord.wednesdayMinute = localCustomTimeRecord.wednesdayMinute

        remoteCustomTimeRecord.thursdayHour = localCustomTimeRecord.thursdayHour
        remoteCustomTimeRecord.thursdayMinute = localCustomTimeRecord.thursdayMinute

        remoteCustomTimeRecord.fridayHour = localCustomTimeRecord.fridayHour
        remoteCustomTimeRecord.fridayMinute = localCustomTimeRecord.fridayMinute

        remoteCustomTimeRecord.saturdayHour = localCustomTimeRecord.saturdayHour
        remoteCustomTimeRecord.saturdayMinute = localCustomTimeRecord.saturdayMinute
    }

    fun hasRemoteRecord(projectId: String): Boolean {
        check(projectId.isNotEmpty())

        return remoteCustomTimeRecords.containsKey(projectId)
    }

    fun clearRemoteRecords() {
        remoteCustomTimeRecords.clear()
    }

    fun getRemoteId(projectId: String): String {
        check(projectId.isNotEmpty())
        check(remoteCustomTimeRecords.containsKey(projectId))

        return remoteCustomTimeRecords[projectId]!!.id
    }
}
