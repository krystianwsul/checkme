package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceData
import com.krystianwsul.checkme.utils.InstanceData.VirtualInstanceData
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DateTime
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.TimePair


class RemoteInstance : Instance {

    private val remoteProject: RemoteProject

    override var instanceData: InstanceData<String, RemoteInstanceRecord>

    private var instanceShownRecord: InstanceShownRecord? = null

    private val taskId
        get() = instanceData.let {
            when (it) {
                is InstanceData.RealInstanceData<String, RemoteInstanceRecord> -> it.instanceRecord.taskId
                is VirtualInstanceData<String, RemoteInstanceRecord> -> it.taskId
            }
        }

    private val remoteFactory get() = domainFactory.remoteProjectFactory!!

    override val notified get() = instanceShownRecord?.notified == true

    override val notificationShown get() = instanceShownRecord?.notificationShown == true

    override val scheduleCustomTimeKey
        get() = instanceData.let {
            when (it) {
                is InstanceData.RealInstanceData<String, RemoteInstanceRecord> -> {
                    val customTimeId = it.instanceRecord.scheduleCustomTimeId

                    customTimeId?.let { domainFactory.getCustomTimeKey(remoteProject.id, it) }
                }
                is VirtualInstanceData<String, RemoteInstanceRecord> -> {
                    val customTimeKey = it.scheduleDateTime
                            .time
                            .timePair
                            .customTimeKey

                    if (customTimeKey is CustomTimeKey.RemoteCustomTimeKey) {
                        domainFactory.getCustomTimeKey(customTimeKey.remoteProjectId, customTimeKey.remoteCustomTimeId)
                    } else {
                        customTimeKey
                    }
                }
            }
        }

    override val task get() = remoteProject.getRemoteTaskForce(taskId)

    override val remoteNullableProject get() = task.remoteProject

    override val remoteNonNullProject get() = task.remoteProject

    override val nullableInstanceShownRecord get() = instanceShownRecord

    override val remoteCustomTimeKey // scenario already covered by task/schedule relevance
        get() = (instanceData as? RemoteRealInstanceData)?.instanceRecord?.instanceCustomTimeId?.let { Pair(remoteProject.id, it) }

    constructor(
            domainFactory: DomainFactory,
            remoteProject: RemoteProject,
            remoteInstanceRecord: RemoteInstanceRecord,
            instanceShownRecord: InstanceShownRecord?,
            now: ExactTimeStamp) : super(domainFactory) {
        this.remoteProject = remoteProject
        instanceData = RemoteRealInstanceData(remoteInstanceRecord)
        this.instanceShownRecord = instanceShownRecord

        val date = instanceDate
        val instanceTimeStamp = ExactTimeStamp(date, instanceTime.getHourMinute(date.dayOfWeek).toHourMilli())
        if (this.instanceShownRecord != null && ((instanceData as RemoteRealInstanceData).instanceRecord.done != null || instanceTimeStamp > now))
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
        instanceData = VirtualInstanceData(taskId, scheduleDateTime)
        this.instanceShownRecord = instanceShownRecord
    }

    override fun setInstanceDateTime(date: Date, timePair: TimePair, now: ExactTimeStamp) {
        check(isRootInstance(now))

        createInstanceHierarchy(now)

        (instanceData as RemoteRealInstanceData).instanceRecord.let {
            it.setInstanceYear(date.year)
            it.setInstanceMonth(date.month)
            it.setInstanceDay(date.day)

            val (customTimeId, hour, minute) = timePair.destructureRemote(remoteFactory, remoteProject)

            it.instanceCustomTimeId = customTimeId
            it.instanceHour = hour
            it.instanceMinute = minute
        }

        createInstanceShownRecord()

        instanceShownRecord!!.notified = false
    }

    private fun createInstanceShownRecord() {
        if (instanceShownRecord != null)
            return

        instanceShownRecord = domainFactory.localFactory.createInstanceShownRecord(taskId, scheduleDateTime, task.remoteProject.id)
    }

    override fun createInstanceRecord(now: ExactTimeStamp) {
        val task = task

        val scheduleDateTime = scheduleDateTime

        instanceData = RemoteRealInstanceData(task.createRemoteInstanceRecord(this, scheduleDateTime))
    }

    override fun setNotificationShown(notificationShown: Boolean, now: ExactTimeStamp) {
        createInstanceShownRecord()

        checkNotNull(instanceShownRecord)

        instanceShownRecord!!.notificationShown = notificationShown
    }

    override fun setDone(done: Boolean, now: ExactTimeStamp) {
        if (done) {
            createInstanceHierarchy(now)

            (instanceData as RemoteRealInstanceData).instanceRecord.done = now.long

            instanceShownRecord?.notified = false
        } else {
            (instanceData as RemoteRealInstanceData).instanceRecord.done = null
        }
    }

    override fun setNotified(now: ExactTimeStamp) {
        createInstanceShownRecord()

        check(instanceShownRecord != null)

        instanceShownRecord!!.notified = true
    }

    override fun delete() {
        checkNotNull(instanceData is RemoteRealInstanceData)

        task.deleteInstance(this)

        (instanceData as RemoteRealInstanceData).instanceRecord.delete()
    }

    override fun belongsToRemoteProject() = true

    override fun getNullableOrdinal() = (instanceData as? RemoteRealInstanceData)?.instanceRecord?.ordinal

    private inner class RemoteRealInstanceData(remoteInstanceRecord: RemoteInstanceRecord) : InstanceData.RealInstanceData<String, RemoteInstanceRecord>(remoteInstanceRecord) {

        override fun getCustomTime(customTimeId: String) = remoteProject.getRemoteCustomTime(customTimeId)

        override fun getSignature() = name + " " + instanceKey.toString()
    }
}
