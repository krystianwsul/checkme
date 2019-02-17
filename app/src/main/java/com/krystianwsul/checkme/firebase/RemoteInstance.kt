package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.utils.InstanceData
import com.krystianwsul.checkme.utils.InstanceData.VirtualInstanceData
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.time.*

class RemoteInstance<T : RemoteCustomTimeId> : Instance {

    private val remoteProject: RemoteProject<T>

    override var instanceData: InstanceData<String, RemoteCustomTimeId, RemoteInstanceRecord<T>>

    override var instanceShownRecord: InstanceShownRecord? = null
        private set

    private val taskId
        get() = instanceData.let {
            when (it) {
                is InstanceData.RealInstanceData<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> -> it.instanceRecord.taskId
                is VirtualInstanceData<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> -> it.taskId
            }
        }

    override val notified get() = this.instanceShownRecord?.notified == true

    override val notificationShown get() = this.instanceShownRecord?.notificationShown == true

    override val scheduleCustomTimeKey
        get() = instanceData.let {
            when (it) {
                is InstanceData.RealInstanceData<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> -> it.instanceRecord
                        .scheduleKey
                        .scheduleTimePair
                        .customTimeKey
                is VirtualInstanceData<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> -> it.scheduleDateTime
                        .time
                        .timePair
                        .customTimeKey
            }
        }

    override val task: RemoteTask<T>

    override val project get() = remoteProject

    override val customTimeKey // scenario already covered by task/schedule relevance
        get() = (instanceData as? RemoteRealInstanceData<T>)?.instanceRecord
                ?.instanceJsonTime
                ?.let { (it as? JsonTime.Custom)?.let { Pair(remoteProject.id, it.id) } }

    constructor(
            domainFactory: DomainFactory,
            remoteProject: RemoteProject<T>,
            remoteTask: RemoteTask<T>,
            remoteInstanceRecord: RemoteInstanceRecord<T>,
            instanceShownRecord: InstanceShownRecord?,
            now: ExactTimeStamp) : super(domainFactory) {
        this.remoteProject = remoteProject
        task = remoteTask
        val realInstanceData = RemoteRealInstanceData(this, remoteInstanceRecord)
        instanceData = realInstanceData
        this.instanceShownRecord = instanceShownRecord

        val date = instanceDate
        val instanceTimeStamp = ExactTimeStamp(date, instanceTime.getHourMinute(date.dayOfWeek).toHourMilli())
        if (realInstanceData.instanceRecord.done != null || instanceTimeStamp > now)
            instanceShownRecord?.notified = false
    }

    constructor(
            domainFactory: DomainFactory,
            remoteProject: RemoteProject<T>,
            remoteTask: RemoteTask<T>,
            scheduleDateTime: DateTime,
            instanceShownRecord: InstanceShownRecord?) : super(domainFactory) {
        this.remoteProject = remoteProject
        task = remoteTask
        instanceData = VirtualInstanceData(task.id, scheduleDateTime)
        this.instanceShownRecord = instanceShownRecord
    }

    override fun setInstanceDateTime(date: Date, timePair: TimePair, now: ExactTimeStamp) {
        check(isRootInstance(now))

        createInstanceHierarchy(now)

        (instanceData as RemoteRealInstanceData).instanceRecord.let {
            it.instanceDate = date

            it.instanceJsonTime = timePair.run {
                hourMinute?.let { JsonTime.Normal<T>(it) }
                        ?: JsonTime.Custom(destructureRemote(remoteProject).first!!)
            }
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

        instanceData = RemoteRealInstanceData(this, task.createRemoteInstanceRecord(this, scheduleDateTime))
    }

    override fun setNotificationShown(notificationShown: Boolean, now: ExactTimeStamp) {
        createInstanceShownRecord()

        checkNotNull(this.instanceShownRecord)

        this.instanceShownRecord!!.notificationShown = notificationShown
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

        checkNotNull(instanceShownRecord)

        instanceShownRecord!!.notified = true
    }

    override fun delete() {
        checkNotNull(instanceData is RemoteRealInstanceData<T>)

        task.deleteInstance(this)

        (instanceData as RemoteRealInstanceData<T>).instanceRecord.delete()
    }

    override fun belongsToRemoteProject() = true

    override fun getNullableOrdinal() = (instanceData as? RemoteRealInstanceData<T>)?.instanceRecord?.ordinal

    private class RemoteRealInstanceData<T : RemoteCustomTimeId>(private val remoteInstance: RemoteInstance<T>, remoteInstanceRecord: RemoteInstanceRecord<T>) : InstanceData.RealInstanceData<String, RemoteCustomTimeId, RemoteInstanceRecord<T>>(remoteInstanceRecord) {

        override fun getCustomTime(customTimeId: RemoteCustomTimeId) = remoteInstance.remoteProject.getRemoteCustomTime(customTimeId)

        override fun getSignature() = "${remoteInstance.name} ${remoteInstance.instanceKey}"
    }
}
