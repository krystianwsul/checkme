package com.krystianwsul.checkme.domainmodel.local

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.checkme.persistencemodel.InstanceRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.*


class LocalInstance : Instance {
    private var mInstanceRecord: InstanceRecord? = null

    private var mTaskId: Int? = null

    private var mScheduleDateTime: DateTime? = null

    val taskId: Int
        get() {
            return if (mInstanceRecord != null) {
                check(mTaskId == null)
                check(mScheduleDateTime == null)

                mInstanceRecord!!.taskId
            } else {
                check(mTaskId != null)
                check(mScheduleDateTime != null)

                mTaskId!!
            }
        }

    override val taskKey: TaskKey
        get() = TaskKey(taskId)

    override val name: String
        get() = task.name

    override val scheduleDate: Date
        get() {
            return if (mInstanceRecord != null) {
                check(mTaskId == null)
                check(mScheduleDateTime == null)

                Date(mInstanceRecord!!.scheduleYear, mInstanceRecord!!.scheduleMonth, mInstanceRecord!!.scheduleDay)
            } else {
                check(mTaskId != null)
                check(mScheduleDateTime != null)

                mScheduleDateTime!!.date
            }
        }

    override val scheduleTime: Time
        get() {
            if (mInstanceRecord != null) {
                check(mTaskId == null)
                check(mScheduleDateTime == null)

                val customTimeId = mInstanceRecord!!.scheduleCustomTimeId
                val hour = mInstanceRecord!!.scheduleHour
                val minute = mInstanceRecord!!.scheduleMinute

                check(hour == null == (minute == null))
                check(customTimeId == null != (hour == null))

                return customTimeId?.let { kotlinDomainFactory.getCustomTime(CustomTimeKey(it)) }
                        ?: NormalTime(hour!!, minute!!)
            } else {
                checkNotNull(mTaskId)
                checkNotNull(mScheduleDateTime)

                return mScheduleDateTime!!.time
            }
        }

    override val instanceDate: Date
        get() {
            if (mInstanceRecord != null) {
                check(mTaskId == null)
                check(mScheduleDateTime == null)

                check(mInstanceRecord!!.instanceYear == null == (mInstanceRecord!!.instanceMonth == null))
                check(mInstanceRecord!!.instanceYear == null == (mInstanceRecord!!.instanceDay == null))
                return if (mInstanceRecord!!.instanceYear != null)
                    Date(mInstanceRecord!!.instanceYear!!, mInstanceRecord!!.instanceMonth!!, mInstanceRecord!!.instanceDay!!)
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
            if (mInstanceRecord != null) {
                check(mTaskId == null)
                check(mScheduleDateTime == null)

                check(mInstanceRecord!!.instanceHour == null == (mInstanceRecord!!.instanceMinute == null))
                check(mInstanceRecord!!.instanceHour == null || mInstanceRecord!!.instanceCustomTimeId == null)

                return when {
                    mInstanceRecord!!.instanceCustomTimeId != null -> kotlinDomainFactory.getCustomTime(CustomTimeKey(mInstanceRecord!!.instanceCustomTimeId!!))
                    mInstanceRecord!!.instanceHour != null -> NormalTime(mInstanceRecord!!.instanceHour!!, mInstanceRecord!!.instanceMinute!!)
                    else -> scheduleTime
                }
            } else {
                checkNotNull(mTaskId)
                checkNotNull(mScheduleDateTime)

                return mScheduleDateTime!!.time
            }
        }

    override val done: ExactTimeStamp?
        get() {
            if (mInstanceRecord == null)
                return null

            val done = mInstanceRecord!!.done
            return if (done != null)
                ExactTimeStamp(done)
            else
                null
        }

    val hierarchyTime: Long
        get() {
            check(mInstanceRecord != null)

            return mInstanceRecord!!.hierarchyTime
        }

    override val notified: Boolean
        get() = mInstanceRecord != null && mInstanceRecord!!.notified

    override val notificationShown: Boolean
        get() = mInstanceRecord != null && mInstanceRecord!!.notificationShown

    override val scheduleCustomTimeKey: CustomTimeKey?
        get() {
            if (mInstanceRecord != null) {
                check(mTaskId == null)
                check(mScheduleDateTime == null)

                val customTimeId = mInstanceRecord!!.scheduleCustomTimeId

                return if (customTimeId != null) {
                    CustomTimeKey(customTimeId)
                } else {
                    null
                }
            } else {
                check(mTaskId != null)
                check(mScheduleDateTime != null)

                return mScheduleDateTime!!.time.timePair.customTimeKey
            }
        }

    override val scheduleHourMinute: HourMinute?
        get() {
            if (mInstanceRecord != null) {
                check(mTaskId == null)
                check(mScheduleDateTime == null)

                val hour = mInstanceRecord!!.scheduleHour
                val minute = mInstanceRecord!!.scheduleMinute

                return if (hour == null) {
                    check(minute == null)

                    null
                } else {
                    check(minute != null)

                    HourMinute(hour, minute!!)
                }
            } else {
                check(mTaskId != null)
                check(mScheduleDateTime != null)

                return mScheduleDateTime!!.time.timePair.hourMinute
            }
        }

    override val task: LocalTask
        get() = kotlinDomainFactory.localFactory.getTaskForce(taskId)

    override val remoteNullableProject: RemoteProject?
        get() = null

    override val remoteNonNullProject: RemoteProject
        get() = throw UnsupportedOperationException()

    override val remoteCustomTimeKey: Pair<String, String>?
        get() = null

    constructor(kotlinDomainFactory: KotlinDomainFactory, instanceRecord: InstanceRecord) : super(kotlinDomainFactory) {
        mInstanceRecord = instanceRecord

        mTaskId = null
        mScheduleDateTime = null
    }

    constructor(kotlinDomainFactory: KotlinDomainFactory, taskId: Int, scheduleDateTime: DateTime) : super(kotlinDomainFactory) {

        mInstanceRecord = null

        mTaskId = taskId
        mScheduleDateTime = scheduleDateTime
    }

    override fun setInstanceDateTime(date: Date, timePair: TimePair, now: ExactTimeStamp) {
        check(isRootInstance(now))

        if (mInstanceRecord == null)
            createInstanceHierarchy(now)

        mInstanceRecord!!.instanceYear = date.year
        mInstanceRecord!!.instanceMonth = date.month
        mInstanceRecord!!.instanceDay = date.day

        if (timePair.customTimeKey != null) {
            check(timePair.hourMinute == null)
            check(timePair.customTimeKey.localCustomTimeId != null)
            check(TextUtils.isEmpty(timePair.customTimeKey.remoteCustomTimeId))

            mInstanceRecord!!.instanceCustomTimeId = timePair.customTimeKey.localCustomTimeId
            mInstanceRecord!!.instanceHour = null
            mInstanceRecord!!.instanceMinute = null
        } else {
            check(timePair.hourMinute != null)

            mInstanceRecord!!.instanceCustomTimeId = null
            mInstanceRecord!!.instanceHour = timePair.hourMinute!!.hour
            mInstanceRecord!!.instanceMinute = timePair.hourMinute.minute
        }

        mInstanceRecord!!.notified = false
    }

    override fun setDone(done: Boolean, now: ExactTimeStamp) {
        if (done) {
            if (mInstanceRecord == null)
                createInstanceHierarchy(now)

            check(mInstanceRecord != null)

            mInstanceRecord!!.done = now.long
            mInstanceRecord!!.notified = false
        } else {
            check(mInstanceRecord != null)
            mInstanceRecord!!.done = null
        }
    }

    override fun createInstanceHierarchy(now: ExactTimeStamp) {
        check(mInstanceRecord == null != (mScheduleDateTime == null))
        check(mTaskId == null == (mScheduleDateTime == null))

        val parentInstance = getParentInstance(now)
        parentInstance?.createInstanceHierarchy(now)

        if (mInstanceRecord == null)
            createInstanceRecord(now)
    }

    private fun createInstanceRecord(now: ExactTimeStamp) {
        val localTask = task

        mInstanceRecord = kotlinDomainFactory.localFactory.createInstanceRecord(localTask, this, scheduleDate, scheduleTimePair, now)

        mTaskId = null
        mScheduleDateTime = null
    }

    override fun setNotified(now: ExactTimeStamp) {
        if (mInstanceRecord == null)
            createInstanceHierarchy(now)

        check(mInstanceRecord != null)
        mInstanceRecord!!.notified = true
    }

    override fun setNotificationShown(notificationShown: Boolean, now: ExactTimeStamp) {
        if (mInstanceRecord == null)
            createInstanceHierarchy(now)

        check(mInstanceRecord != null)

        mInstanceRecord!!.notificationShown = notificationShown
    }

    override fun exists(): Boolean {
        check(mInstanceRecord == null != (mScheduleDateTime == null))
        check(mTaskId == null == (mScheduleDateTime == null))

        return mInstanceRecord != null
    }

    override fun delete() {
        checkNotNull(mInstanceRecord)

        kotlinDomainFactory.localFactory.deleteInstance(this)

        mInstanceRecord!!.delete()
    }

    override fun belongsToRemoteProject() = false

    override fun getNullableOrdinal() = mInstanceRecord?.ordinal

    override fun setOrdinal(ordinal: Double, now: ExactTimeStamp) {
        if (mInstanceRecord == null)
            createInstanceHierarchy(now)

        mInstanceRecord!!.ordinal = ordinal
    }
}
