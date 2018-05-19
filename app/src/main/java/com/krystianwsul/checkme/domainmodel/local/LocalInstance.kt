package com.krystianwsul.checkme.domainmodel.local

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.checkme.persistencemodel.InstanceRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.*
import junit.framework.Assert

class LocalInstance : Instance {
    private var mInstanceRecord: InstanceRecord? = null

    private var mTaskId: Int? = null

    private var mScheduleDateTime: DateTime? = null

    val taskId: Int
        get() {
            return if (mInstanceRecord != null) {
                Assert.assertTrue(mTaskId == null)
                Assert.assertTrue(mScheduleDateTime == null)

                mInstanceRecord!!.taskId
            } else {
                Assert.assertTrue(mTaskId != null)
                Assert.assertTrue(mScheduleDateTime != null)

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
                Assert.assertTrue(mTaskId == null)
                Assert.assertTrue(mScheduleDateTime == null)

                Date(mInstanceRecord!!.scheduleYear, mInstanceRecord!!.scheduleMonth, mInstanceRecord!!.scheduleDay)
            } else {
                Assert.assertTrue(mTaskId != null)
                Assert.assertTrue(mScheduleDateTime != null)

                mScheduleDateTime!!.date
            }
        }

    override val scheduleTime: Time
        get() {
            if (mInstanceRecord != null) {
                Assert.assertTrue(mTaskId == null)
                Assert.assertTrue(mScheduleDateTime == null)

                val customTimeId = mInstanceRecord!!.scheduleCustomTimeId
                val hour = mInstanceRecord!!.scheduleHour
                val minute = mInstanceRecord!!.scheduleMinute

                Assert.assertTrue(hour == null == (minute == null))
                Assert.assertTrue(customTimeId == null != (hour == null))

                return if (customTimeId != null) {
                    domainFactory.getCustomTime(CustomTimeKey(customTimeId))
                } else {
                    NormalTime(hour!!, minute!!)
                }
            } else {
                Assert.assertTrue(mTaskId != null)
                Assert.assertTrue(mScheduleDateTime != null)

                return mScheduleDateTime!!.time
            }
        }

    override val instanceDate: Date
        get() {
            if (mInstanceRecord != null) {
                Assert.assertTrue(mTaskId == null)
                Assert.assertTrue(mScheduleDateTime == null)

                Assert.assertTrue(mInstanceRecord!!.instanceYear == null == (mInstanceRecord!!.instanceMonth == null))
                Assert.assertTrue(mInstanceRecord!!.instanceYear == null == (mInstanceRecord!!.instanceDay == null))
                return if (mInstanceRecord!!.instanceYear != null)
                    Date(mInstanceRecord!!.instanceYear!!, mInstanceRecord!!.instanceMonth!!, mInstanceRecord!!.instanceDay!!)
                else
                    scheduleDate
            } else {
                Assert.assertTrue(mTaskId != null)
                Assert.assertTrue(mScheduleDateTime != null)

                return mScheduleDateTime!!.date
            }
        }

    override val instanceTime: Time
        get() {
            if (mInstanceRecord != null) {
                Assert.assertTrue(mTaskId == null)
                Assert.assertTrue(mScheduleDateTime == null)

                Assert.assertTrue(mInstanceRecord!!.instanceHour == null == (mInstanceRecord!!.instanceMinute == null))
                Assert.assertTrue(mInstanceRecord!!.instanceHour == null || mInstanceRecord!!.instanceCustomTimeId == null)

                return when {
                    mInstanceRecord!!.instanceCustomTimeId != null -> domainFactory.getCustomTime(CustomTimeKey(mInstanceRecord!!.instanceCustomTimeId!!))
                    mInstanceRecord!!.instanceHour != null -> NormalTime(mInstanceRecord!!.instanceHour!!, mInstanceRecord!!.instanceMinute!!)
                    else -> scheduleTime
                }
            } else {
                Assert.assertTrue(mTaskId != null)
                Assert.assertTrue(mScheduleDateTime != null)

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
            Assert.assertTrue(mInstanceRecord != null)

            return mInstanceRecord!!.hierarchyTime
        }

    override val notified: Boolean
        get() = mInstanceRecord != null && mInstanceRecord!!.notified

    override val notificationShown: Boolean
        get() = mInstanceRecord != null && mInstanceRecord!!.notificationShown

    override val scheduleCustomTimeKey: CustomTimeKey?
        get() {
            if (mInstanceRecord != null) {
                Assert.assertTrue(mTaskId == null)
                Assert.assertTrue(mScheduleDateTime == null)

                val customTimeId = mInstanceRecord!!.scheduleCustomTimeId

                return if (customTimeId != null) {
                    CustomTimeKey(customTimeId)
                } else {
                    null
                }
            } else {
                Assert.assertTrue(mTaskId != null)
                Assert.assertTrue(mScheduleDateTime != null)

                return mScheduleDateTime!!.time.timePair.customTimeKey
            }
        }

    override val scheduleHourMinute: HourMinute?
        get() {
            if (mInstanceRecord != null) {
                Assert.assertTrue(mTaskId == null)
                Assert.assertTrue(mScheduleDateTime == null)

                val hour = mInstanceRecord!!.scheduleHour
                val minute = mInstanceRecord!!.scheduleMinute

                return if (hour == null) {
                    Assert.assertTrue(minute == null)

                    null
                } else {
                    Assert.assertTrue(minute != null)

                    HourMinute(hour, minute!!)
                }
            } else {
                Assert.assertTrue(mTaskId != null)
                Assert.assertTrue(mScheduleDateTime != null)

                return mScheduleDateTime!!.time.timePair.hourMinute
            }
        }

    override val task: LocalTask
        get() = domainFactory.localFactory.getTaskForce(taskId)

    override val remoteNullableProject: RemoteProject?
        get() = null

    override val remoteNonNullProject: RemoteProject
        get() = throw UnsupportedOperationException()

    override val remoteCustomTimeKey: Pair<String, String>?
        get() = null

    internal constructor(domainFactory: DomainFactory, instanceRecord: InstanceRecord) : super(domainFactory) {

        mInstanceRecord = instanceRecord

        mTaskId = null
        mScheduleDateTime = null
    }

    constructor(domainFactory: DomainFactory, taskId: Int, scheduleDateTime: DateTime) : super(domainFactory) {

        mInstanceRecord = null

        mTaskId = taskId
        mScheduleDateTime = scheduleDateTime
    }

    override fun setInstanceDateTime(date: Date, timePair: TimePair, now: ExactTimeStamp) {
        Assert.assertTrue(isRootInstance(now))

        if (mInstanceRecord == null)
            createInstanceHierarchy(now)

        mInstanceRecord!!.instanceYear = date.year
        mInstanceRecord!!.instanceMonth = date.month
        mInstanceRecord!!.instanceDay = date.day

        if (timePair.customTimeKey != null) {
            Assert.assertTrue(timePair.hourMinute == null)
            Assert.assertTrue(timePair.customTimeKey.localCustomTimeId != null)
            Assert.assertTrue(TextUtils.isEmpty(timePair.customTimeKey.remoteCustomTimeId))

            mInstanceRecord!!.instanceCustomTimeId = timePair.customTimeKey.localCustomTimeId
            mInstanceRecord!!.instanceHour = null
            mInstanceRecord!!.instanceMinute = null
        } else {
            Assert.assertTrue(timePair.hourMinute != null)

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

            Assert.assertTrue(mInstanceRecord != null)

            mInstanceRecord!!.done = now.long
            mInstanceRecord!!.notified = false
        } else {
            Assert.assertTrue(mInstanceRecord != null)
            mInstanceRecord!!.done = null
        }
    }

    override fun createInstanceHierarchy(now: ExactTimeStamp) {
        Assert.assertTrue(mInstanceRecord == null != (mScheduleDateTime == null))
        Assert.assertTrue(mTaskId == null == (mScheduleDateTime == null))

        val parentInstance = getParentInstance(now)
        parentInstance?.createInstanceHierarchy(now)

        if (mInstanceRecord == null)
            createInstanceRecord(now)
    }

    private fun createInstanceRecord(now: ExactTimeStamp) {
        val localTask = task

        mInstanceRecord = domainFactory.localFactory.createInstanceRecord(localTask, this, scheduleDate, scheduleTimePair, now)

        mTaskId = null
        mScheduleDateTime = null
    }

    override fun setNotified(now: ExactTimeStamp) {
        if (mInstanceRecord == null)
            createInstanceHierarchy(now)

        Assert.assertTrue(mInstanceRecord != null)
        mInstanceRecord!!.notified = true
    }

    override fun setNotificationShown(notificationShown: Boolean, now: ExactTimeStamp) {
        if (mInstanceRecord == null)
            createInstanceHierarchy(now)

        Assert.assertTrue(mInstanceRecord != null)

        mInstanceRecord!!.notificationShown = notificationShown
    }

    override fun exists(): Boolean {
        Assert.assertTrue(mInstanceRecord == null != (mScheduleDateTime == null))
        Assert.assertTrue(mTaskId == null == (mScheduleDateTime == null))

        return mInstanceRecord != null
    }

    override fun delete() {
        Assert.assertTrue(mInstanceRecord != null)

        domainFactory.localFactory.deleteInstance(this)

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
