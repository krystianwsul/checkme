package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceData
import com.krystianwsul.checkme.utils.InstanceData.VirtualInstanceData
import com.krystianwsul.checkme.utils.time.*


class RemoteInstance : Instance {

    private val remoteProject: RemoteProject

    private var realInstanceData: InstanceData.RealInstanceData<String, RemoteInstanceRecord>? = null

    private var virtualInstanceData: VirtualInstanceData<String>? = null

    private var instanceShownRecord: InstanceShownRecord? = null

    private val taskId
        get() = if (realInstanceData != null) {
            check(virtualInstanceData == null)

            realInstanceData!!.instanceRecord.taskId
        } else {
            virtualInstanceData!!.taskId
        }

    override val scheduleDate
        get() = if (realInstanceData != null) {
            check(virtualInstanceData == null)

            Date(realInstanceData!!.instanceRecord.scheduleYear, realInstanceData!!.instanceRecord.scheduleMonth, realInstanceData!!.instanceRecord.scheduleDay)
        } else {
            virtualInstanceData!!.scheduleDateTime.date
        }

    override val scheduleTime: Time
        get() = if (realInstanceData != null) {
            check(virtualInstanceData == null)

            val customTimeId = realInstanceData!!.instanceRecord.scheduleCustomTimeId
            val hour = realInstanceData!!.instanceRecord.scheduleHour
            val minute = realInstanceData!!.instanceRecord.scheduleMinute

            check(hour == null == (minute == null))
            check(customTimeId == null != (hour == null))

            customTimeId?.let { remoteProject.getRemoteCustomTime(it) }
                    ?: NormalTime(hour!!, minute!!)
        } else {
            virtualInstanceData!!.scheduleDateTime.time
        }

    override val taskKey by lazy { task.taskKey }

    override val done get() = realInstanceData?.instanceRecord?.done?.let { ExactTimeStamp(it) }

    override val instanceDate
        get() = if (realInstanceData != null) {
            check(virtualInstanceData == null)

            if (realInstanceData!!.instanceRecord.instanceYear != null) {
                checkNotNull(realInstanceData!!.instanceRecord.instanceMonth)
                checkNotNull(realInstanceData!!.instanceRecord.instanceDay)

                Date(realInstanceData!!.instanceRecord.instanceYear!!, realInstanceData!!.instanceRecord.instanceMonth!!, realInstanceData!!.instanceRecord.instanceDay!!)
            } else {
                check(realInstanceData!!.instanceRecord.instanceMonth == null)
                check(realInstanceData!!.instanceRecord.instanceDay == null)

                scheduleDate
            }
        } else {
            virtualInstanceData!!.scheduleDateTime.date
        }

    override val instanceTime
        get() = if (realInstanceData != null) {
            check(virtualInstanceData == null)

            check(realInstanceData!!.instanceRecord.instanceHour == null == (realInstanceData!!.instanceRecord.instanceMinute == null))
            check(realInstanceData!!.instanceRecord.instanceHour == null || realInstanceData!!.instanceRecord.instanceCustomTimeId == null)

            when {
                realInstanceData!!.instanceRecord.instanceCustomTimeId != null -> remoteProject.getRemoteCustomTime(realInstanceData!!.instanceRecord.instanceCustomTimeId!!)
                realInstanceData!!.instanceRecord.instanceHour != null -> NormalTime(realInstanceData!!.instanceRecord.instanceHour!!, realInstanceData!!.instanceRecord.instanceMinute!!)
                else -> scheduleTime
            }
        } else {
            virtualInstanceData!!.scheduleDateTime.time
        }

    override val name get() = task.name

    private val remoteFactory get() = domainFactory.remoteProjectFactory!!

    override val notified get() = instanceShownRecord?.notified == true

    override val notificationShown get() = instanceShownRecord?.notificationShown == true

    override val scheduleCustomTimeKey
        get() = if (realInstanceData != null) {
            check(virtualInstanceData == null)

            val customTimeId = realInstanceData!!.instanceRecord.scheduleCustomTimeId

            customTimeId?.let { domainFactory.getCustomTimeKey(remoteProject.id, it) }
        } else {
            val customTimeKey = virtualInstanceData!!
                    .scheduleDateTime
                    .time
                    .timePair
                    .customTimeKey

            if (customTimeKey is CustomTimeKey.RemoteCustomTimeKey) {
                domainFactory.getCustomTimeKey(customTimeKey.remoteProjectId, customTimeKey.remoteCustomTimeId)
            } else {
                customTimeKey
            }
        }

    override val scheduleHourMinute
        get() = if (realInstanceData != null) {
            check(virtualInstanceData == null)

            val hour = realInstanceData!!.instanceRecord.scheduleHour
            val minute = realInstanceData!!.instanceRecord.scheduleMinute

            if (hour == null) {
                check(minute == null)

                null
            } else {
                checkNotNull(minute)

                HourMinute(hour, minute)
            }
        } else {
            virtualInstanceData!!.scheduleDateTime
                    .time
                    .timePair
                    .hourMinute
        }

    override val task get() = remoteProject.getRemoteTaskForce(taskId)

    override val remoteNullableProject get() = task.remoteProject

    override val remoteNonNullProject get() = task.remoteProject

    override val remoteCustomTimeKey // scenario already covered by task/schedule relevance
        get() = realInstanceData?.instanceRecord?.instanceCustomTimeId?.let { Pair(remoteProject.id, it) }

    constructor(
            domainFactory: DomainFactory,
            remoteProject: RemoteProject,
            remoteInstanceRecord: RemoteInstanceRecord,
            instanceShownRecord: InstanceShownRecord?,
            now: ExactTimeStamp) : super(domainFactory) {
        this.remoteProject = remoteProject
        realInstanceData = InstanceData.RealInstanceData(remoteInstanceRecord)
        virtualInstanceData = null
        this.instanceShownRecord = instanceShownRecord

        val date = instanceDate
        val instanceTimeStamp = ExactTimeStamp(date, instanceTime.getHourMinute(date.dayOfWeek).toHourMilli())
        if (this.instanceShownRecord != null && (this.realInstanceData!!.instanceRecord.done != null || instanceTimeStamp > now))
            this.instanceShownRecord!!.notified = false
    }

    constructor(
            domainFactory: DomainFactory,
            remoteProject: RemoteProject,
            taskId: String,
            scheduleDateTime: DateTime,
            instanceShownRecord: InstanceShownRecord?) : super(domainFactory) {
        check(!TextUtils.isEmpty(taskId))

        this.remoteProject = remoteProject
        realInstanceData = null
        virtualInstanceData = VirtualInstanceData(taskId, scheduleDateTime)
        this.instanceShownRecord = instanceShownRecord
    }

    override fun exists(): Boolean {
        check(realInstanceData == null != (virtualInstanceData == null))

        return realInstanceData != null
    }

    override fun setInstanceDateTime(date: Date, timePair: TimePair, now: ExactTimeStamp) {
        check(isRootInstance(now))

        createInstanceHierarchy(now)

        realInstanceData!!.instanceRecord.setInstanceYear(date.year)
        realInstanceData!!.instanceRecord.setInstanceMonth(date.month)
        realInstanceData!!.instanceRecord.setInstanceDay(date.day)

        val (customTimeId, hour, minute) = timePair.destructureRemote(remoteFactory, remoteProject)

        realInstanceData!!.instanceRecord.instanceCustomTimeId = customTimeId
        realInstanceData!!.instanceRecord.instanceHour = hour
        realInstanceData!!.instanceRecord.instanceMinute = minute

        createInstanceShownRecord()

        checkNotNull(instanceShownRecord)

        instanceShownRecord!!.notified = false
    }

    private fun createInstanceShownRecord() {
        if (instanceShownRecord != null)
            return

        instanceShownRecord = domainFactory.localFactory.createInstanceShownRecord(taskId, scheduleDateTime, task.remoteProject.id)
    }

    override fun createInstanceHierarchy(now: ExactTimeStamp) {
        check(realInstanceData == null != (virtualInstanceData == null))

        if (realInstanceData != null)
            return

        getParentInstance(now)?.createInstanceHierarchy(now)

        createInstanceRecord()
    }

    private fun createInstanceRecord() {
        val task = task

        val scheduleDateTime = scheduleDateTime

        realInstanceData = InstanceData.RealInstanceData(task.createRemoteInstanceRecord(this, scheduleDateTime))

        virtualInstanceData = null
    }

    override fun setNotificationShown(notificationShown: Boolean, now: ExactTimeStamp) {
        createInstanceShownRecord()

        checkNotNull(instanceShownRecord)

        instanceShownRecord!!.notificationShown = notificationShown
    }

    override fun setDone(done: Boolean, now: ExactTimeStamp) {
        if (done) {
            createInstanceHierarchy(now)

            checkNotNull(realInstanceData)

            realInstanceData!!.instanceRecord.done = now.long

            instanceShownRecord?.notified = false
        } else {
            checkNotNull(realInstanceData)

            realInstanceData!!.instanceRecord.done = null
        }
    }

    override fun setNotified(now: ExactTimeStamp) {
        createInstanceShownRecord()

        check(instanceShownRecord != null)

        instanceShownRecord!!.notified = true
    }

    override fun delete() {
        checkNotNull(realInstanceData)

        task.deleteInstance(this)

        realInstanceData!!.instanceRecord.delete()
    }

    override fun belongsToRemoteProject() = true

    override fun getNullableOrdinal() = realInstanceData?.instanceRecord?.ordinal

    override fun setOrdinal(ordinal: Double, now: ExactTimeStamp) {
        createInstanceHierarchy(now)

        realInstanceData!!.instanceRecord.setOrdinal(ordinal)
    }
}
