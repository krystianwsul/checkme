package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.CustomTime
import com.krystianwsul.common.domain.Instance
import com.krystianwsul.common.domain.InstanceData
import com.krystianwsul.common.domain.InstanceData.Virtual
import com.krystianwsul.common.firebase.records.RemoteInstanceRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemoteInstance<T : RemoteCustomTimeId> : Instance {

    private val remoteProject: RemoteProject<T>

    override var instanceData: InstanceData<String, RemoteCustomTimeId, RemoteInstanceRecord<T>>

    private val shownHolder = ShownHolder()

    override fun getShown(shownFactory: ShownFactory) = shownHolder.getShown(shownFactory)

    override fun getNotified(shownFactory: ShownFactory) = getShown(shownFactory)?.notified == true

    override fun setNotified(shownFactory: ShownFactory, notified: Boolean) {
        shownHolder.forceShown(shownFactory).notified = notified
    }

    override fun getNotificationShown(shownFactory: ShownFactory) = getShown(shownFactory)?.notificationShown == true

    override fun setNotificationShown(shownFactory: ShownFactory, notificationShown: Boolean) {
        shownHolder.forceShown(shownFactory).notificationShown = notificationShown
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
            remoteProject: RemoteProject<T>,
            remoteTask: RemoteTask<T>,
            remoteInstanceRecord: RemoteInstanceRecord<T>
    ) { // todo to primary constructor
        this.remoteProject = remoteProject
        task = remoteTask
        val realInstanceData = RemoteReal(this, remoteInstanceRecord)
        instanceData = realInstanceData
    }

    constructor(
            remoteProject: RemoteProject<T>,
            remoteTask: RemoteTask<T>,
            scheduleDateTime: DateTime
    ) {
        this.remoteProject = remoteProject
        task = remoteTask
        instanceData = Virtual(task.id, scheduleDateTime)
    }

    fun fixNotificationShown(shownFactory: ShownFactory, now: ExactTimeStamp) {
        if (done != null || instanceDateTime.toExactTimeStamp() > now)
            getShown(shownFactory)?.notified = false
    }

    override fun setInstanceDateTime(shownFactory: ShownFactory, ownerKey: String, dateTime: DateTime, now: ExactTimeStamp) {
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

        shownHolder.forceShown(shownFactory).notified = false
    }

    override fun createInstanceRecord(now: ExactTimeStamp): InstanceData.Real<String, RemoteCustomTimeId, RemoteInstanceRecord<T>> = RemoteReal(this, task.createRemoteInstanceRecord(this, scheduleDateTime)).also {
        instanceData = it
    }

    override fun setDone(uuid: String, shownFactory: ShownFactory, done: Boolean, now: ExactTimeStamp) {
        if (done) {
            createInstanceHierarchy(now).instanceRecord.done = now.long

            getShown(shownFactory)?.notified = false
        } else {
            (instanceData as RemoteReal).instanceRecord.done = null
        }

        task.updateOldestVisible(uuid, now)
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

    private inner class ShownHolder {

        private var first = true
        private var shown: Shown? = null

        fun getShown(shownFactory: ShownFactory): Shown? {
            if (first)
                shown = shownFactory.getShown(taskKey, scheduleDateTime)
            return shown
        }

        fun forceShown(shownFactory: ShownFactory): Shown {
            if (getShown(shownFactory) == null)
                shown = shownFactory.createShown(taskKey.remoteTaskId, scheduleDateTime, taskKey.remoteProjectId)
            return shown!!
        }
    }
}
