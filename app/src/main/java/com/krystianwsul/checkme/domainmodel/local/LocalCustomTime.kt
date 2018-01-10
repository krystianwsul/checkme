package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.CustomTime
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord
import com.krystianwsul.checkme.persistencemodel.LocalCustomTimeRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair
import junit.framework.Assert
import java.util.*

class LocalCustomTime(private val domainFactory: DomainFactory, private val localCustomTimeRecord: LocalCustomTimeRecord) : CustomTime {

    private val remoteCustomTimeRecords = HashMap<String, RemoteCustomTimeRecord>()

    val id get() = localCustomTimeRecord.id

    val current get() = localCustomTimeRecord.current

    override fun getName() = localCustomTimeRecord.name

    fun setName(name: String) {
        Assert.assertTrue(name.isNotEmpty())
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

    override fun getHourMinutes() = TreeMap<DayOfWeek, HourMinute>().apply {
        putAll(DayOfWeek.values().map { Pair(it, getHourMinute(it)) })
    }

    fun setHourMinute(dayOfWeek: DayOfWeek, hourMinute: HourMinute) {
        when (dayOfWeek) { // todo add a common interface for all custom time records
            DayOfWeek.SUNDAY -> {
                localCustomTimeRecord.sundayHour = hourMinute.hour
                localCustomTimeRecord.sundayMinute = hourMinute.minute

                remoteCustomTimeRecords.values.forEach {
                    it.sundayHour = hourMinute.hour
                    it.sundayMinute = hourMinute.minute
                }
            }
            DayOfWeek.MONDAY -> {
                localCustomTimeRecord.mondayHour = hourMinute.hour
                localCustomTimeRecord.mondayMinute = hourMinute.minute

                remoteCustomTimeRecords.values.forEach {
                    it.mondayHour = hourMinute.hour
                    it.mondayMinute = hourMinute.minute
                }
            }
            DayOfWeek.TUESDAY -> {
                localCustomTimeRecord.tuesdayHour = hourMinute.hour
                localCustomTimeRecord.tuesdayMinute = hourMinute.minute

                remoteCustomTimeRecords.values.forEach {
                    it.tuesdayHour = hourMinute.hour
                    it.tuesdayMinute = hourMinute.minute
                }
            }
            DayOfWeek.WEDNESDAY -> {
                localCustomTimeRecord.wednesdayHour = hourMinute.hour
                localCustomTimeRecord.wednesdayMinute = hourMinute.minute

                remoteCustomTimeRecords.values.forEach {
                    it.wednesdayHour = hourMinute.hour
                    it.wednesdayMinute = hourMinute.minute
                }
            }
            DayOfWeek.THURSDAY -> {
                localCustomTimeRecord.thursdayHour = hourMinute.hour
                localCustomTimeRecord.thursdayMinute = hourMinute.minute

                remoteCustomTimeRecords.values.forEach {
                    it.thursdayHour = hourMinute.hour
                    it.thursdayMinute = hourMinute.minute
                }
            }
            DayOfWeek.FRIDAY -> {
                localCustomTimeRecord.fridayHour = hourMinute.hour
                localCustomTimeRecord.fridayMinute = hourMinute.minute

                remoteCustomTimeRecords.values.forEach {
                    it.fridayHour = hourMinute.hour
                    it.fridayMinute = hourMinute.minute
                }
            }
            DayOfWeek.SATURDAY -> {
                localCustomTimeRecord.saturdayHour = hourMinute.hour
                localCustomTimeRecord.saturdayMinute = hourMinute.minute

                remoteCustomTimeRecords.values.forEach {
                    it.saturdayHour = hourMinute.hour
                    it.saturdayMinute = hourMinute.minute
                }
            }
        }
    }

    override fun toString() = name

    fun setCurrent() {
        localCustomTimeRecord.current = false
    }

    override fun getTimePair() = TimePair(CustomTimeKey(localCustomTimeRecord.id), null)

    fun delete() {
        domainFactory.localFactory.deleteCustomTime(this)

        localCustomTimeRecord.delete()
    }

    override fun getCustomTimeKey() = CustomTimeKey(id)

    fun addRemoteCustomTimeRecord(remoteCustomTimeRecord: RemoteCustomTimeRecord) {
        Assert.assertTrue(remoteCustomTimeRecord.localId == localCustomTimeRecord.id)
        Assert.assertTrue(!remoteCustomTimeRecords.containsKey(remoteCustomTimeRecord.projectId))

        remoteCustomTimeRecords.put(remoteCustomTimeRecord.projectId, remoteCustomTimeRecord)

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
        Assert.assertTrue(projectId.isNotEmpty())

        return remoteCustomTimeRecords.containsKey(projectId)
    }

    fun clearRemoteRecords() {
        remoteCustomTimeRecords.clear()
    }

    fun getRemoteId(projectId: String): String {
        Assert.assertTrue(projectId.isNotEmpty())
        Assert.assertTrue(remoteCustomTimeRecords.containsKey(projectId))

        return remoteCustomTimeRecords[projectId]!!.id
    }
}
