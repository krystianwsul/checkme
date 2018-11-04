package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.checkme.persistencemodel.LocalInstanceRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceData
import com.krystianwsul.checkme.utils.InstanceData.VirtualInstanceData
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.*


class LocalInstance : Instance {

    private var realInstanceData: InstanceData.RealInstanceData<Int, LocalInstanceRecord>? = null
    private var virtualInstanceData: VirtualInstanceData<Int>? = null

    val taskId: Int
        get() {
            return if (realInstanceData != null) {
                check(virtualInstanceData == null)

                realInstanceData!!.instanceRecord.taskId
            } else {
                virtualInstanceData!!.taskId
            }
        }

    override val taskKey get() = TaskKey(taskId)

    override val name get() = task.name

    override val scheduleDate: Date
        get() {
            return if (realInstanceData != null) {
                check(virtualInstanceData == null)

                Date(realInstanceData!!.instanceRecord.scheduleYear, realInstanceData!!.instanceRecord.scheduleMonth, realInstanceData!!.instanceRecord.scheduleDay)
            } else {
                virtualInstanceData!!.scheduleDateTime.date
            }
        }

    override val scheduleTime: Time
        get() {
            return if (realInstanceData != null) {
                check(virtualInstanceData == null)

                val customTimeId = realInstanceData!!.instanceRecord.scheduleCustomTimeId
                val hour = realInstanceData!!.instanceRecord.scheduleHour
                val minute = realInstanceData!!.instanceRecord.scheduleMinute

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
            return if (realInstanceData != null) {
                check(virtualInstanceData == null)

                check(realInstanceData!!.instanceRecord.instanceYear == null == (realInstanceData!!.instanceRecord.instanceMonth == null))
                check(realInstanceData!!.instanceRecord.instanceYear == null == (realInstanceData!!.instanceRecord.instanceDay == null))
                if (realInstanceData!!.instanceRecord.instanceYear != null)
                    Date(realInstanceData!!.instanceRecord.instanceYear!!, realInstanceData!!.instanceRecord.instanceMonth!!, realInstanceData!!.instanceRecord.instanceDay!!)
                else
                    scheduleDate
            } else {
                virtualInstanceData!!.scheduleDateTime.date
            }
        }

    override val instanceTime: Time
        get() {
            return if (realInstanceData != null) {
                check(virtualInstanceData == null)

                check(realInstanceData!!.instanceRecord.instanceHour == null == (realInstanceData!!.instanceRecord.instanceMinute == null))
                check(realInstanceData!!.instanceRecord.instanceHour == null || realInstanceData!!.instanceRecord.instanceCustomTimeId == null)

                when {
                    realInstanceData!!.instanceRecord.instanceCustomTimeId != null -> domainFactory.getCustomTime(CustomTimeKey.LocalCustomTimeKey(realInstanceData!!.instanceRecord.instanceCustomTimeId!!))
                    realInstanceData!!.instanceRecord.instanceHour != null -> NormalTime(realInstanceData!!.instanceRecord.instanceHour!!, realInstanceData!!.instanceRecord.instanceMinute!!)
                    else -> scheduleTime
                }
            } else {
                virtualInstanceData!!.scheduleDateTime.time
            }
        }

    override val done get() = realInstanceData?.instanceRecord?.done?.let { ExactTimeStamp(it) }

    override val notified get() = realInstanceData?.instanceRecord?.notified == true

    override val notificationShown get() = realInstanceData?.instanceRecord?.notificationShown == true

    override val scheduleCustomTimeKey: CustomTimeKey?
        get() {
            return if (realInstanceData != null) {
                check(virtualInstanceData == null)

                realInstanceData!!.instanceRecord.scheduleCustomTimeId?.let { CustomTimeKey.LocalCustomTimeKey(it) }
            } else {
                virtualInstanceData!!.scheduleDateTime
                        .time
                        .timePair
                        .customTimeKey
            }
        }

    override val scheduleHourMinute: HourMinute?
        get() {
            if (realInstanceData != null) {
                check(virtualInstanceData == null)

                val hour = realInstanceData!!.instanceRecord.scheduleHour
                val minute = realInstanceData!!.instanceRecord.scheduleMinute

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

    constructor(domainFactory: DomainFactory, localInstanceRecord: LocalInstanceRecord) : super(domainFactory) {
        realInstanceData = InstanceData.RealInstanceData(localInstanceRecord)
        virtualInstanceData = null
    }

    constructor(domainFactory: DomainFactory, taskId: Int, scheduleDateTime: DateTime) : super(domainFactory) {
        realInstanceData = null
        virtualInstanceData = VirtualInstanceData(taskId, scheduleDateTime)
    }

    override fun setInstanceDateTime(date: Date, timePair: TimePair, now: ExactTimeStamp) {
        check(isRootInstance(now))

        createInstanceHierarchy(now)

        realInstanceData!!.instanceRecord.let {
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

            realInstanceData!!.instanceRecord.done = now.long
            realInstanceData!!.instanceRecord.notified = false
        } else {
            realInstanceData!!.instanceRecord.done = null
        }
    }

    override fun createInstanceHierarchy(now: ExactTimeStamp) {
        check(realInstanceData == null != (virtualInstanceData == null))

        if (realInstanceData != null)
            return

        getParentInstance(now)?.createInstanceHierarchy(now)

        if (realInstanceData == null)
            createInstanceRecord(now)
    }

    private fun createInstanceRecord(now: ExactTimeStamp) {
        val localTask = task

        realInstanceData = InstanceData.RealInstanceData(domainFactory.localFactory.createInstanceRecord(localTask, this, scheduleDate, scheduleTimePair, now))

        virtualInstanceData = null
    }

    override fun setNotified(now: ExactTimeStamp) {
        createInstanceHierarchy(now)

        realInstanceData!!.instanceRecord.notified = true
    }

    override fun setNotificationShown(notificationShown: Boolean, now: ExactTimeStamp) {
        createInstanceHierarchy(now)

        realInstanceData!!.instanceRecord.notificationShown = notificationShown
    }

    override fun exists(): Boolean {
        check(realInstanceData == null != (virtualInstanceData == null))

        return realInstanceData != null
    }

    override fun delete() {
        checkNotNull(realInstanceData)

        domainFactory.localFactory.deleteInstance(this)

        realInstanceData!!.instanceRecord.delete()
    }

    override fun belongsToRemoteProject() = false

    override fun getNullableOrdinal() = realInstanceData?.instanceRecord?.ordinal

    override fun setOrdinal(ordinal: Double, now: ExactTimeStamp) {
        createInstanceHierarchy(now)

        realInstanceData!!.instanceRecord.ordinal = ordinal
    }

}
