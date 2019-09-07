package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.utils.InstanceData
import com.krystianwsul.checkme.utils.InstanceData.Virtual

import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemoteInstance<T : RemoteCustomTimeId> : Instance {

    private val remoteProject: RemoteProject<T>

    override var instanceData: InstanceData<String, RemoteCustomTimeId, RemoteInstanceRecord<T>>

    override var instanceShownRecord: InstanceShownRecord? = null
        private set

    private val taskId
        get() = instanceData.let {
            when (it) {
                is InstanceData.Real<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> -> it.instanceRecord.taskId
                is Virtual<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> -> it.taskId
            }
        }

    override var notified
        get() = this.instanceShownRecord?.notified == true
        set(value) {
            createInstanceShownRecord()

            instanceShownRecord!!.notified = value
        }

    override var notificationShown
        get() = this.instanceShownRecord?.notificationShown == true
        set(value) {
            createInstanceShownRecord()

            instanceShownRecord!!.notificationShown = value
        }

    override val scheduleCustomTimeKey
        get() = instanceData.let {
            when (it) {
                is InstanceData.Real<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> -> it.instanceRecord
                        .scheduleKey
                        .scheduleTimePair
                        .customTimeKey
                is Virtual<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> -> it.scheduleDateTime
                        .time
                        .timePair
                        .customTimeKey
            }
        }

    override val task: RemoteTask<T>

    override val project get() = remoteProject

    override val customTimeKey // scenario already covered by task/schedule relevance
        get() = (instanceData as? RemoteReal<T>)?.instanceRecord
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
        val realInstanceData = RemoteReal(this, remoteInstanceRecord)
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
        instanceData = Virtual(task.id, scheduleDateTime)
        this.instanceShownRecord = instanceShownRecord
    }

    override fun setInstanceDateTime(date: Date, timePair: TimePair, now: ExactTimeStamp) {
        check(isRootInstance(now))

        createInstanceHierarchy(now)

        (instanceData as RemoteReal).instanceRecord.let {
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

    override fun createInstanceRecord(now: ExactTimeStamp): InstanceData.Real<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> = RemoteReal(this, task.createRemoteInstanceRecord(this, scheduleDateTime)).also {
        instanceData = it
    }

    override fun setDone(done: Boolean, now: ExactTimeStamp) {
        if (done) {
            createInstanceHierarchy(now).instanceRecord.done = now.long

            instanceShownRecord?.notified = false
        } else {
            (instanceData as RemoteReal).instanceRecord.done = null
        }
    }

    override fun delete() {
        checkNotNull(instanceData is RemoteReal<T>)

        task.deleteInstance(this)

        (instanceData as RemoteReal<T>).instanceRecord.delete()
    }

    override fun belongsToRemoteProject() = true

    override fun getNullableOrdinal() = (instanceData as? RemoteReal<T>)?.instanceRecord?.ordinal

    private class RemoteReal<T : RemoteCustomTimeId>(private val remoteInstance: RemoteInstance<T>, remoteInstanceRecord: RemoteInstanceRecord<T>) : InstanceData.Real<String, RemoteCustomTimeId, RemoteInstanceRecord<T>>(remoteInstanceRecord) {

        override fun getCustomTime(customTimeId: RemoteCustomTimeId) = remoteInstance.remoteProject.getRemoteCustomTime(customTimeId)

        override fun getSignature() = "${remoteInstance.name} ${remoteInstance.instanceKey}"
    }
}
