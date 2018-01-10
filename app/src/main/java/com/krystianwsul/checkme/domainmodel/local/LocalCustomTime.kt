package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.CustomTime
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord
import com.krystianwsul.checkme.persistencemodel.CustomTimeRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair
import junit.framework.Assert
import java.util.*

class LocalCustomTime(private val domainFactory: DomainFactory, private val customTimeRecord: CustomTimeRecord) : CustomTime {

    private val remoteCustomTimeRecords = HashMap<String, RemoteCustomTimeRecord>()

    val id get() = customTimeRecord.id

    val current get() = customTimeRecord.current

    override fun getName() = customTimeRecord.name

    fun setName(name: String) {
        Assert.assertTrue(name.isNotEmpty())
        customTimeRecord.name = name

        remoteCustomTimeRecords.values.forEach { it.name = name }
    }

    override fun getHourMinute(dayOfWeek: DayOfWeek): HourMinute = when (dayOfWeek) {
        DayOfWeek.SUNDAY -> HourMinute(customTimeRecord.sundayHour, customTimeRecord.sundayMinute)
        DayOfWeek.MONDAY -> HourMinute(customTimeRecord.mondayHour, customTimeRecord.mondayMinute)
        DayOfWeek.TUESDAY -> HourMinute(customTimeRecord.tuesdayHour, customTimeRecord.tuesdayMinute)
        DayOfWeek.WEDNESDAY -> HourMinute(customTimeRecord.wednesdayHour, customTimeRecord.wednesdayMinute)
        DayOfWeek.THURSDAY -> HourMinute(customTimeRecord.thursdayHour, customTimeRecord.thursdayMinute)
        DayOfWeek.FRIDAY -> HourMinute(customTimeRecord.fridayHour, customTimeRecord.fridayMinute)
        DayOfWeek.SATURDAY -> HourMinute(customTimeRecord.saturdayHour, customTimeRecord.saturdayMinute)
    }

    override fun getHourMinutes() = TreeMap<DayOfWeek, HourMinute>().apply {
        putAll(DayOfWeek.values().map { Pair(it, getHourMinute(it)) })
    }

    fun setHourMinute(dayOfWeek: DayOfWeek, hourMinute: HourMinute) {
        when (dayOfWeek) { // todo add a common interface for all custom time records
            DayOfWeek.SUNDAY -> {
                customTimeRecord.sundayHour = hourMinute.hour
                customTimeRecord.sundayMinute = hourMinute.minute

                remoteCustomTimeRecords.values.forEach {
                    it.sundayHour = hourMinute.hour
                    it.sundayMinute = hourMinute.minute
                }
            }
            DayOfWeek.MONDAY -> {
                customTimeRecord.mondayHour = hourMinute.hour
                customTimeRecord.mondayMinute = hourMinute.minute

                remoteCustomTimeRecords.values.forEach {
                    it.mondayHour = hourMinute.hour
                    it.mondayMinute = hourMinute.minute
                }
            }
            DayOfWeek.TUESDAY -> {
                customTimeRecord.tuesdayHour = hourMinute.hour
                customTimeRecord.tuesdayMinute = hourMinute.minute

                remoteCustomTimeRecords.values.forEach {
                    it.tuesdayHour = hourMinute.hour
                    it.tuesdayMinute = hourMinute.minute
                }
            }
            DayOfWeek.WEDNESDAY -> {
                customTimeRecord.wednesdayHour = hourMinute.hour
                customTimeRecord.wednesdayMinute = hourMinute.minute

                remoteCustomTimeRecords.values.forEach {
                    it.wednesdayHour = hourMinute.hour
                    it.wednesdayMinute = hourMinute.minute
                }
            }
            DayOfWeek.THURSDAY -> {
                customTimeRecord.thursdayHour = hourMinute.hour
                customTimeRecord.thursdayMinute = hourMinute.minute

                remoteCustomTimeRecords.values.forEach {
                    it.thursdayHour = hourMinute.hour
                    it.thursdayMinute = hourMinute.minute
                }
            }
            DayOfWeek.FRIDAY -> {
                customTimeRecord.fridayHour = hourMinute.hour
                customTimeRecord.fridayMinute = hourMinute.minute

                remoteCustomTimeRecords.values.forEach {
                    it.fridayHour = hourMinute.hour
                    it.fridayMinute = hourMinute.minute
                }
            }
            DayOfWeek.SATURDAY -> {
                customTimeRecord.saturdayHour = hourMinute.hour
                customTimeRecord.saturdayMinute = hourMinute.minute

                remoteCustomTimeRecords.values.forEach {
                    it.saturdayHour = hourMinute.hour
                    it.saturdayMinute = hourMinute.minute
                }
            }
        }
    }

    override fun toString() = name

    fun setCurrent() {
        customTimeRecord.current = false
    }

    override fun getTimePair() = TimePair(CustomTimeKey(customTimeRecord.id), null)

    fun delete() {
        domainFactory.localFactory.deleteCustomTime(this)

        customTimeRecord.delete()
    }

    override fun getCustomTimeKey() = CustomTimeKey(id)

    fun addRemoteCustomTimeRecord(remoteCustomTimeRecord: RemoteCustomTimeRecord) {
        Assert.assertTrue(remoteCustomTimeRecord.localId == customTimeRecord.id)
        Assert.assertTrue(!remoteCustomTimeRecords.containsKey(remoteCustomTimeRecord.projectId))

        remoteCustomTimeRecords.put(remoteCustomTimeRecord.projectId, remoteCustomTimeRecord)

        // bez zapisywania na razie, dopiero przy następnej okazji
        remoteCustomTimeRecord.name = customTimeRecord.name

        remoteCustomTimeRecord.sundayHour = customTimeRecord.sundayHour
        remoteCustomTimeRecord.sundayMinute = customTimeRecord.sundayMinute

        remoteCustomTimeRecord.mondayHour = customTimeRecord.mondayHour
        remoteCustomTimeRecord.mondayMinute = customTimeRecord.mondayMinute

        remoteCustomTimeRecord.tuesdayHour = customTimeRecord.tuesdayHour
        remoteCustomTimeRecord.tuesdayMinute = customTimeRecord.tuesdayMinute

        remoteCustomTimeRecord.wednesdayHour = customTimeRecord.wednesdayHour
        remoteCustomTimeRecord.wednesdayMinute = customTimeRecord.wednesdayMinute

        remoteCustomTimeRecord.thursdayHour = customTimeRecord.thursdayHour
        remoteCustomTimeRecord.thursdayMinute = customTimeRecord.thursdayMinute

        remoteCustomTimeRecord.fridayHour = customTimeRecord.fridayHour
        remoteCustomTimeRecord.fridayMinute = customTimeRecord.fridayMinute

        remoteCustomTimeRecord.saturdayHour = customTimeRecord.saturdayHour
        remoteCustomTimeRecord.saturdayMinute = customTimeRecord.saturdayMinute
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
