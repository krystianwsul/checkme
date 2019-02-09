package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceData
import com.krystianwsul.checkme.utils.InstanceData.VirtualInstanceData
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.time.*

class RemoteInstance<T : RemoteCustomTimeId> : Instance {

    private val remoteProject: RemoteProject<T>

    override var instanceData: InstanceData<String, RemoteCustomTimeId, RemoteInstanceRecord<T>>

    private var instanceShownRecord: InstanceShownRecord? = null

    private val taskId
        get() = instanceData.let {
            when (it) {
                is InstanceData.RealInstanceData<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> -> it.instanceRecord.taskId
                is VirtualInstanceData<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> -> it.taskId
            }
        }

    override val notified get() = instanceShownRecord?.notified == true

    override val notificationShown get() = instanceShownRecord?.notificationShown == true

    override val scheduleCustomTimeKey
        get() = instanceData.let {
            when (it) {
                is InstanceData.RealInstanceData<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> -> it.instanceRecord
                        .scheduleKey
                        .scheduleTimePair
                        .customTimeKey
                is VirtualInstanceData<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> -> {
                    val customTimeKey = it.scheduleDateTime
                            .time
                            .timePair
                            .customTimeKey

                    if (customTimeKey is CustomTimeKey.RemoteCustomTimeKey<*>) {
                        domainFactory.getLocalCustomTimeKeyIfPossible(customTimeKey.remoteProjectId, customTimeKey.remoteCustomTimeId)
                    } else {
                        customTimeKey
                    }
                }
            }
        }

    override val task: RemoteTask<T>

    override val remoteNullableProject get() = remoteProject

    override val remoteNonNullProject get() = remoteProject

    override val nullableInstanceShownRecord get() = instanceShownRecord

    override val remoteCustomTimeKey // scenario already covered by task/schedule relevance
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
        instanceData = RemoteRealInstanceData(this, remoteInstanceRecord)
        this.instanceShownRecord = instanceShownRecord

        val date = instanceDate
        val instanceTimeStamp = ExactTimeStamp(date, instanceTime.getHourMinute(date.dayOfWeek).toHourMilli())
        if (this.instanceShownRecord != null && ((instanceData as RemoteRealInstanceData).instanceRecord.done != null || instanceTimeStamp > now))
            this.instanceShownRecord!!.notified = false
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
