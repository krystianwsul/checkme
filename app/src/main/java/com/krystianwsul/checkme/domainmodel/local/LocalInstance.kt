package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.checkme.persistencemodel.InstanceRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.VirtualInstanceData
import com.krystianwsul.checkme.utils.time.*


class LocalInstance : Instance {

    private var localInstanceRecord: InstanceRecord? = null
    private var virtualInstanceData: VirtualInstanceData<Int>? = null

    val taskId: Int
        get() {
            return if (localInstanceRecord != null) {
                check(virtualInstanceData == null)

                localInstanceRecord!!.taskId
            } else {
                virtualInstanceData!!.taskId
            }
        }

    override val taskKey get() = TaskKey(taskId)

    override val name get() = task.name

    override val scheduleDate: Date
        get() {
            return if (localInstanceRecord != null) {
                check(virtualInstanceData == null)

                Date(localInstanceRecord!!.scheduleYear, localInstanceRecord!!.scheduleMonth, localInstanceRecord!!.scheduleDay)
            } else {
                virtualInstanceData!!.scheduleDateTime.date
            }
        }

    override val scheduleTime: Time
        get() {
            return if (localInstanceRecord != null) {
                check(virtualInstanceData == null)

                val customTimeId = localInstanceRecord!!.scheduleCustomTimeId
                val hour = localInstanceRecord!!.scheduleHour
                val minute = localInstanceRecord!!.scheduleMinute

                check(hour == null == (minute == null))
                check(customTimeId == null != (hour == null))

                customTimeId?.let { domainFactory.getCustomTime(CustomTimeKey.LocalCustomTimeKey(it)) }
                        ?: NormalTime(hour!!, minute!!)
            } else {
                virtualInstanceData!!.scheduleDateTime.time
            }
        }

    override val instanceDate: Date
        get() {
            return if (localInstanceRecord != null) {
                check(virtualInstanceData == null)

                check(localInstanceRecord!!.instanceYear == null == (localInstanceRecord!!.instanceMonth == null))
                check(localInstanceRecord!!.instanceYear == null == (localInstanceRecord!!.instanceDay == null))
                if (localInstanceRecord!!.instanceYear != null)
                    Date(localInstanceRecord!!.instanceYear!!, localInstanceRecord!!.instanceMonth!!, localInstanceRecord!!.instanceDay!!)
                else
                    scheduleDate
            } else {
                virtualInstanceData!!.scheduleDateTime.date
            }
        }

    override val instanceTime: Time
        get() {
            return if (localInstanceRecord != null) {
                check(virtualInstanceData == null)

                check(localInstanceRecord!!.instanceHour == null == (localInstanceRecord!!.instanceMinute == null))
                check(localInstanceRecord!!.instanceHour == null || localInstanceRecord!!.instanceCustomTimeId == null)

                when {
                    localInstanceRecord!!.instanceCustomTimeId != null -> domainFactory.getCustomTime(CustomTimeKey.LocalCustomTimeKey(localInstanceRecord!!.instanceCustomTimeId!!))
                    localInstanceRecord!!.instanceHour != null -> NormalTime(localInstanceRecord!!.instanceHour!!, localInstanceRecord!!.instanceMinute!!)
                    else -> scheduleTime
                }
            } else {
                virtualInstanceData!!.scheduleDateTime.time
            }
        }

    override val done get() = localInstanceRecord?.done?.let { ExactTimeStamp(it) }

    override val notified get() = localInstanceRecord?.notified == true

    override val notificationShown get() = localInstanceRecord?.notificationShown == true

    override val scheduleCustomTimeKey: CustomTimeKey?
        get() {
            return if (localInstanceRecord != null) {
                check(virtualInstanceData == null)

                localInstanceRecord!!.scheduleCustomTimeId?.let { CustomTimeKey.LocalCustomTimeKey(it) }
            } else {
                virtualInstanceData!!.scheduleDateTime
                        .time
                        .timePair
                        .customTimeKey
            }
        }

    override val scheduleHourMinute: HourMinute?
        get() {
            if (localInstanceRecord != null) {
                check(virtualInstanceData == null)

                val hour = localInstanceRecord!!.scheduleHour
                val minute = localInstanceRecord!!.scheduleMinute

                return if (hour == null) {
                    check(minute == null)

                    null
                } else {
                    HourMinute(hour, minute!!)
                }
            } else {
                return virtualInstanceData!!.scheduleDateTime
                        .time
                        .timePair
                        .hourMinute
            }
        }

    override val task get() = domainFactory.localFactory.getTaskForce(taskId)

    override val remoteNullableProject: RemoteProject? = null

    override val remoteNonNullProject get() = throw UnsupportedOperationException()

    override val remoteCustomTimeKey: Pair<String, String>? = null

    constructor(domainFactory: DomainFactory, instanceRecord: InstanceRecord) : super(domainFactory) {
        localInstanceRecord = instanceRecord
        virtualInstanceData = null
    }

    constructor(domainFactory: DomainFactory, taskId: Int, scheduleDateTime: DateTime) : super(domainFactory) {
        localInstanceRecord = null
        virtualInstanceData = VirtualInstanceData(taskId, scheduleDateTime)
    }

    override fun setInstanceDateTime(date: Date, timePair: TimePair, now: ExactTimeStamp) {
        check(isRootInstance(now))

        createInstanceHierarchy(now)

        localInstanceRecord!!.let {
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

            localInstanceRecord!!.done = now.long
            localInstanceRecord!!.notified = false
        } else {
            localInstanceRecord!!.done = null
        }
    }

    override fun createInstanceHierarchy(now: ExactTimeStamp) {
        check(localInstanceRecord == null != (virtualInstanceData == null))

        if (localInstanceRecord != null)
            return

        getParentInstance(now)?.createInstanceHierarchy(now)

        if (localInstanceRecord == null)
            createInstanceRecord(now)
    }

    private fun createInstanceRecord(now: ExactTimeStamp) {
        val localTask = task

        localInstanceRecord = domainFactory.localFactory.createInstanceRecord(localTask, this, scheduleDate, scheduleTimePair, now)

        virtualInstanceData = null
    }

    override fun setNotified(now: ExactTimeStamp) {
        createInstanceHierarchy(now)

        localInstanceRecord!!.notified = true
    }

    override fun setNotificationShown(notificationShown: Boolean, now: ExactTimeStamp) {
        createInstanceHierarchy(now)

        localInstanceRecord!!.notificationShown = notificationShown
    }

    override fun exists(): Boolean {
        check(localInstanceRecord == null != (virtualInstanceData == null))

        return localInstanceRecord != null
    }

    override fun delete() {
        checkNotNull(localInstanceRecord)

        domainFactory.localFactory.deleteInstance(this)

        localInstanceRecord!!.delete()
    }

    override fun belongsToRemoteProject() = false

    override fun getNullableOrdinal() = localInstanceRecord?.ordinal

    override fun setOrdinal(ordinal: Double, now: ExactTimeStamp) {
        createInstanceHierarchy(now)

        localInstanceRecord!!.ordinal = ordinal
    }

}
