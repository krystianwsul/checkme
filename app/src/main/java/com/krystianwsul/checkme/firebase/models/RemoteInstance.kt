package com.krystianwsul.checkme.firebase.models

import com.krystianwsul.checkme.domain.Instance
import com.krystianwsul.common.domain.CustomTime
import com.krystianwsul.common.domain.InstanceData
import com.krystianwsul.common.domain.InstanceData.Virtual
import com.krystianwsul.common.firebase.records.RemoteInstanceRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.CustomTimeKey
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
            createShown()

            shown!!.notified = value
        }

    override var notificationShown
        get() = shown?.notificationShown == true
        set(value) {
            createShown()

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
            shownFactory: ShownFactory,
            remoteProject: RemoteProject<T>,
            remoteTask: RemoteTask<T>,
            remoteInstanceRecord: RemoteInstanceRecord<T>,
            shown: Shown?,
            now: ExactTimeStamp) : super(shownFactory) {
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
            shownFactory: ShownFactory,
            remoteProject: RemoteProject<T>,
            remoteTask: RemoteTask<T>,
            scheduleDateTime: DateTime,
            shown: Shown?) : super(shownFactory) {
        this.remoteProject = remoteProject
        task = remoteTask
        instanceData = Virtual(task.id, scheduleDateTime)
        this.shown = shown
    }

    override fun setInstanceDateTime(ownerKey: String, dateTime: DateTime, now: ExactTimeStamp) {
        check(isRootInstance(now))

        createInstanceHierarchy(now)

        (instanceData as RemoteReal).instanceRecord.let {
            it.instanceDate = dateTime.date

            it.instanceJsonTime = project.getOrCopyTime(ownerKey, dateTime.time).let {
                @Suppress("UNCHECKED_CAST")
                when (it) {
                    is CustomTime -> JsonTime.Custom(it.customTimeKey.remoteCustomTimeId as T)
                    is NormalTime -> JsonTime.Normal(it.hourMinute)
                    else -> throw IllegalArgumentException()
                }
            }
        }

        createShown()

        shown!!.notified = false
    }

    private fun createShown() {
        if (shown != null)
            return

        shown = shownFactory.createShown(taskId, scheduleDateTime, task.remoteProject.id)
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

    private class RemoteReal<T : RemoteCustomTimeId>(private val remoteInstance: RemoteInstance<T>, remoteInstanceRecord: RemoteInstanceRecord<T>) : InstanceData.Real<String, RemoteCustomTimeId, RemoteInstanceRecord<T>>(remoteInstanceRecord) {

        override fun getCustomTime(customTimeId: RemoteCustomTimeId) = remoteInstance.remoteProject.getRemoteCustomTime(customTimeId)

        override fun getSignature() = "${remoteInstance.name} ${remoteInstance.instanceKey}"
    }
}
