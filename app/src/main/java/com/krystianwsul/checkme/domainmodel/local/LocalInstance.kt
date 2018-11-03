package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.checkme.persistencemodel.InstanceRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.*


class LocalInstance : Instance {
    private var localInstanceRecord: InstanceRecord? = null

    private var mTaskId: Int? = null

    private var mScheduleDateTime: DateTime? = null

    val taskId: Int
        get() {
            return if (localInstanceRecord != null) {
                check(mTaskId == null)
                check(mScheduleDateTime == null)

                localInstanceRecord!!.taskId
            } else {
                check(mTaskId != null)
                check(mScheduleDateTime != null)

                mTaskId!!
            }
        }

    override val taskKey get() = TaskKey(taskId)

    override val name get() = task.name

    override val scheduleDate: Date
        get() {
            return if (localInstanceRecord != null) {
                check(mTaskId == null)
                check(mScheduleDateTime == null)

                Date(localInstanceRecord!!.scheduleYear, localInstanceRecord!!.scheduleMonth, localInstanceRecord!!.scheduleDay)
            } else {
                check(mTaskId != null)
                check(mScheduleDateTime != null)

                mScheduleDateTime!!.date
            }
        }

    override val scheduleTime: Time
        get() {
            if (localInstanceRecord != null) {
                check(mTaskId == null)
                check(mScheduleDateTime == null)

                val customTimeId = localInstanceRecord!!.scheduleCustomTimeId
                val hour = localInstanceRecord!!.scheduleHour
                val minute = localInstanceRecord!!.scheduleMinute

                check(hour == null == (minute == null))
                check(customTimeId == null != (hour == null))

                return customTimeId?.let { domainFactory.getCustomTime(CustomTimeKey.LocalCustomTimeKey(it)) }
                        ?: NormalTime(hour!!, minute!!)
            } else {
                checkNotNull(mTaskId)
                checkNotNull(mScheduleDateTime)

                return mScheduleDateTime!!.time
            }
        }

    override val instanceDate: Date
        get() {
            if (localInstanceRecord != null) {
                check(mTaskId == null)
                check(mScheduleDateTime == null)

                check(localInstanceRecord!!.instanceYear == null == (localInstanceRecord!!.instanceMonth == null))
                check(localInstanceRecord!!.instanceYear == null == (localInstanceRecord!!.instanceDay == null))
                return if (localInstanceRecord!!.instanceYear != null)
                    Date(localInstanceRecord!!.instanceYear!!, localInstanceRecord!!.instanceMonth!!, localInstanceRecord!!.instanceDay!!)
                else
                    scheduleDate
            } else {
                check(mTaskId != null)
                check(mScheduleDateTime != null)

                return mScheduleDateTime!!.date
            }
        }

    override val instanceTime: Time
        get() {
            if (localInstanceRecord != null) {
                check(mTaskId == null)
                check(mScheduleDateTime == null)

                check(localInstanceRecord!!.instanceHour == null == (localInstanceRecord!!.instanceMinute == null))
                check(localInstanceRecord!!.instanceHour == null || localInstanceRecord!!.instanceCustomTimeId == null)

                return when {
                    localInstanceRecord!!.instanceCustomTimeId != null -> domainFactory.getCustomTime(CustomTimeKey.LocalCustomTimeKey(localInstanceRecord!!.instanceCustomTimeId!!))
                    localInstanceRecord!!.instanceHour != null -> NormalTime(localInstanceRecord!!.instanceHour!!, localInstanceRecord!!.instanceMinute!!)
                    else -> scheduleTime
                }
            } else {
                checkNotNull(mTaskId)
                checkNotNull(mScheduleDateTime)

                return mScheduleDateTime!!.time
            }
        }

    override val done get() = localInstanceRecord?.done?.let { ExactTimeStamp(it) }

    val hierarchyTime get() = localInstanceRecord!!.hierarchyTime

    override val notified get() = localInstanceRecord?.notified == true

    override val notificationShown get() = localInstanceRecord?.notificationShown == true

    override val scheduleCustomTimeKey: CustomTimeKey?
        get() {
            return if (localInstanceRecord != null) {
                check(mTaskId == null)
                check(mScheduleDateTime == null)

                localInstanceRecord!!.scheduleCustomTimeId?.let { CustomTimeKey.LocalCustomTimeKey(it) }
            } else {
                checkNotNull(mTaskId)

                mScheduleDateTime!!.time.timePair.customTimeKey
            }
        }

    override val scheduleHourMinute: HourMinute?
        get() {
            if (localInstanceRecord != null) {
                check(mTaskId == null)
                check(mScheduleDateTime == null)

                val hour = localInstanceRecord!!.scheduleHour
                val minute = localInstanceRecord!!.scheduleMinute

                return if (hour == null) {
                    check(minute == null)

                    null
                } else {
                    checkNotNull(minute)

                    HourMinute(hour, minute)
                }
            } else {
                check(mTaskId != null)
                check(mScheduleDateTime != null)

                return mScheduleDateTime!!.time.timePair.hourMinute
            }
        }

    override val task get() = domainFactory.localFactory.getTaskForce(taskId)

    override val remoteNullableProject: RemoteProject? = null

    override val remoteNonNullProject get() = throw UnsupportedOperationException()

    override val remoteCustomTimeKey: Pair<String, String>? = null

    constructor(domainFactory: DomainFactory, instanceRecord: InstanceRecord) : super(domainFactory) {
        localInstanceRecord = instanceRecord

        mTaskId = null
        mScheduleDateTime = null
    }

    constructor(domainFactory: DomainFactory, taskId: Int, scheduleDateTime: DateTime) : super(domainFactory) {
        localInstanceRecord = null

        mTaskId = taskId
        mScheduleDateTime = scheduleDateTime
    }

    override fun setInstanceDateTime(date: Date, timePair: TimePair, now: ExactTimeStamp) {
        check(isRootInstance(now))

        createInstanceHierarchy(now)

        localInstanceRecord!!.let {
            it.instanceYear = date.year
            it.instanceMonth = date.month
            it.instanceDay = date.day

            if (timePair.customTimeKey != null) {
                check(timePair.hourMinute == null)

                it.instanceCustomTimeId = (timePair.customTimeKey as CustomTimeKey.LocalCustomTimeKey).localCustomTimeId
                it.instanceHour = null
                it.instanceMinute = null
            } else {
                checkNotNull(timePair.hourMinute)

                it.instanceCustomTimeId = null
                it.instanceHour = timePair.hourMinute.hour
                it.instanceMinute = timePair.hourMinute.minute
            }

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
        check(localInstanceRecord == null != (mScheduleDateTime == null))
        check(mTaskId == null == (mScheduleDateTime == null))

        if (localInstanceRecord != null)
            return

        getParentInstance(now)?.createInstanceHierarchy(now)

        if (localInstanceRecord == null)
            createInstanceRecord(now)
    }

    private fun createInstanceRecord(now: ExactTimeStamp) {
        val localTask = task

        localInstanceRecord = domainFactory.localFactory.createInstanceRecord(localTask, this, scheduleDate, scheduleTimePair, now)

        mTaskId = null
        mScheduleDateTime = null
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
        check(localInstanceRecord == null != (mScheduleDateTime == null))
        check(mTaskId == null == (mScheduleDateTime == null))

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
