package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.persistencemodel.LocalInstanceRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceData
import com.krystianwsul.checkme.utils.InstanceData.VirtualInstanceData
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DateTime
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.TimePair


class LocalInstance : Instance {

    override var instanceData: InstanceData<Int, LocalInstanceRecord>

    val taskId
        get() = instanceData.let {
            when (it) {
                is InstanceData.RealInstanceData<Int, LocalInstanceRecord> -> it.instanceRecord.taskId
                is VirtualInstanceData<Int, LocalInstanceRecord> -> it.taskId
            }
        }

    override val notified get() = (instanceData as? InstanceData.RealInstanceData<Int, LocalInstanceRecord>)?.instanceRecord?.notified == true

    override val notificationShown get() = (instanceData as? InstanceData.RealInstanceData<Int, LocalInstanceRecord>)?.instanceRecord?.notificationShown == true

    override val scheduleCustomTimeKey
        get() = instanceData.let {
            when (it) {
                is InstanceData.RealInstanceData<Int, LocalInstanceRecord> -> it.instanceRecord
                        .scheduleCustomTimeId
                        ?.let { CustomTimeKey.LocalCustomTimeKey(it) }
                is VirtualInstanceData<Int, LocalInstanceRecord> -> it.scheduleDateTime
                        .time
                        .timePair
                        .customTimeKey
            }
        }

    override val task get() = domainFactory.localFactory.getTaskForce(taskId)

    override val remoteNullableProject: RemoteProject? = null

    override val remoteNonNullProject get() = throw UnsupportedOperationException()

    override val remoteCustomTimeKey: Pair<String, String>? = null

    override val nullableInstanceShownRecord: InstanceShownRecord? = null

    constructor(domainFactory: DomainFactory, localInstanceRecord: LocalInstanceRecord) : super(domainFactory) {
        instanceData = LocalRealInstanceData(localInstanceRecord)
    }

    constructor(domainFactory: DomainFactory, taskId: Int, scheduleDateTime: DateTime) : super(domainFactory) {
        instanceData = VirtualInstanceData(taskId, scheduleDateTime)
    }

    override fun setInstanceDateTime(date: Date, timePair: TimePair, now: ExactTimeStamp) {
        check(isRootInstance(now))

        createInstanceHierarchy(now)

        (instanceData as LocalRealInstanceData).instanceRecord.let {
            it.instanceYear = date.year
            it.instanceMonth = date.month
            it.instanceDay = date.day

            val (customTimeId, hour, minute) = timePair.destructureLocal()
            it.instanceCustomTimeId = customTimeId
            it.instanceHour = hour
            it.instanceMinute = minute

            it.notified = false
        }
    }

    override fun setDone(done: Boolean, now: ExactTimeStamp) {
        if (done) {
            createInstanceHierarchy(now)

            (instanceData as LocalRealInstanceData).instanceRecord.let {
                it.done = now.long
                it.notified = false
            }
        } else {
            (instanceData as LocalRealInstanceData).instanceRecord.done = null
        }
    }

    override fun createInstanceRecord(now: ExactTimeStamp) {
        val localTask = task

        instanceData = LocalRealInstanceData(domainFactory.localFactory.createInstanceRecord(localTask, this, scheduleDate, scheduleTimePair, now))
    }

    override fun setNotified(now: ExactTimeStamp) {
        createInstanceHierarchy(now)

        (instanceData as LocalRealInstanceData).instanceRecord.notified = true
    }

    override fun setNotificationShown(notificationShown: Boolean, now: ExactTimeStamp) {
        createInstanceHierarchy(now)

        (instanceData as LocalRealInstanceData).instanceRecord.notificationShown = notificationShown
    }

    override fun delete() {
        check(instanceData is LocalRealInstanceData)

        domainFactory.localFactory.deleteInstance(this)

        (instanceData as LocalRealInstanceData).instanceRecord.delete()
    }

    override fun belongsToRemoteProject() = false

    override fun getNullableOrdinal() = (instanceData as? LocalRealInstanceData)?.instanceRecord?.ordinal

    private inner class LocalRealInstanceData(localInstanceRecord: LocalInstanceRecord) : InstanceData.RealInstanceData<Int, LocalInstanceRecord>(localInstanceRecord) {

        override fun getCustomTime(customTimeId: Int) = domainFactory.getCustomTime(CustomTimeKey.LocalCustomTimeKey(customTimeId))

        override fun getSignature() = name + " " + instanceKey.toString()
    }
}
