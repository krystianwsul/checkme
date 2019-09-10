package com.krystianwsul.checkme.firebase.models

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.domainmodel.TaskHierarchy
import com.krystianwsul.checkme.utils.time.destructureRemote
import com.krystianwsul.common.domain.InstanceData
import com.krystianwsul.common.domain.InstanceData.Virtual
import com.krystianwsul.common.firebase.records.RemoteInstanceRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemoteInstance<T : RemoteCustomTimeId> : Instance {

    private val remoteProject: RemoteProject<T>

    override var instanceData: InstanceData<String, RemoteCustomTimeId, RemoteInstanceRecord<T>>

    override var shown: Shown? = null
        private set

    private val taskId
        get() = instanceData.let {
            when (it) {
                is InstanceData.Real<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> -> it.instanceRecord.taskId
                is Virtual<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> -> it.taskId
            }
        }

    override var notified
        get() = shown?.notified == true
        set(value) {
            createInstanceShownRecord()

            shown!!.notified = value
        }

    override var notificationShown
        get() = shown?.notificationShown == true
        set(value) {
            createInstanceShownRecord()

            shown!!.notificationShown = value
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
            shown: Shown?,
            now: ExactTimeStamp) : super(domainFactory) {
        this.remoteProject = remoteProject
        task = remoteTask
        val realInstanceData = RemoteReal(this, remoteInstanceRecord)
        instanceData = realInstanceData
        this.shown = shown

        val date = instanceDate
        val instanceTimeStamp = ExactTimeStamp(date, instanceTime.getHourMinute(date.dayOfWeek).toHourMilli())
        if (realInstanceData.instanceRecord.done != null || instanceTimeStamp > now)
            shown?.notified = false
    }

    constructor(
            domainFactory: DomainFactory,
            remoteProject: RemoteProject<T>,
            remoteTask: RemoteTask<T>,
            scheduleDateTime: DateTime,
            shown: Shown?) : super(domainFactory) {
        this.remoteProject = remoteProject
        task = remoteTask
        instanceData = Virtual(task.id, scheduleDateTime)
        this.shown = shown
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

        shown!!.notified = false
    }

    private fun createInstanceShownRecord() {
        if (shown != null)
            return

        shown = domainFactory.localFactory.createInstanceShownRecord(taskId, scheduleDateTime, task.remoteProject.id)
    }

    override fun createInstanceRecord(now: ExactTimeStamp): InstanceData.Real<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> = RemoteReal(this, task.createRemoteInstanceRecord(this, scheduleDateTime)).also {
        instanceData = it
    }

    override fun setDone(done: Boolean, now: ExactTimeStamp) {
        if (done) {
            createInstanceHierarchy(now).instanceRecord.done = now.long

            shown?.notified = false
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

    override fun getCreateTaskTimePair(ownerKey: String): TimePair {
        val instanceTimePair = instanceTime.timePair
        val shared = instanceTimePair.customTimeKey as? CustomTimeKey.Shared

        return if (shared != null) {
            val sharedCustomTime = remoteProject.getRemoteCustomTime(shared.remoteCustomTimeId) as RemoteSharedCustomTime

            if (sharedCustomTime.ownerKey == ownerKey) {
                val privateCustomTimeKey = CustomTimeKey.Private(ownerKey, sharedCustomTime.privateKey!!)

                TimePair(privateCustomTimeKey)
            } else {
                val hourMinute = sharedCustomTime.getHourMinute(instanceDate.dayOfWeek)

                TimePair(hourMinute)
            }
        } else {
            instanceTimePair
        }
    }

    override fun getChildInstances(now: ExactTimeStamp): List<Pair<Instance, TaskHierarchy>> {
        val hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now).first

        val task = task

        val scheduleDateTime = scheduleDateTime

        val taskHierarchies = task.getTaskHierarchiesByParentTaskKey(task.taskKey)
        val childInstances = HashMap<InstanceKey, Pair<Instance, TaskHierarchy>>()
        for (taskHierarchy in taskHierarchies) {
            if (taskHierarchy.notDeleted(hierarchyExactTimeStamp) && taskHierarchy.childTask.notDeleted(hierarchyExactTimeStamp)) {
                val childInstance = (taskHierarchy.childTask as RemoteTask<*>).getInstance(scheduleDateTime)

                val parentInstance = childInstance.getParentInstance(now)
                if (parentInstance?.instanceKey == instanceKey)
                    childInstances[childInstance.instanceKey] = Pair(childInstance, taskHierarchy)
            }
        }

        return ArrayList(childInstances.values)
    }

    private class RemoteReal<T : RemoteCustomTimeId>(private val remoteInstance: RemoteInstance<T>, remoteInstanceRecord: RemoteInstanceRecord<T>) : InstanceData.Real<String, RemoteCustomTimeId, RemoteInstanceRecord<T>>(remoteInstanceRecord) {

        override fun getCustomTime(customTimeId: RemoteCustomTimeId) = remoteInstance.remoteProject.getRemoteCustomTime(customTimeId)

        override fun getSignature() = "${remoteInstance.name} ${remoteInstance.instanceKey}"
    }
}
