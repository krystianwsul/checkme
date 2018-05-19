package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.utils.time.*
import junit.framework.Assert

class RemoteInstance : Instance {

    private val remoteProject: RemoteProject

    private var remoteInstanceRecord: RemoteInstanceRecord? = null

    private var _taskId: String? = null

    private var _scheduleDateTime: DateTime? = null

    private var instanceShownRecord: InstanceShownRecord? = null

    private val taskId
        get() = if (remoteInstanceRecord != null) {
            Assert.assertTrue(TextUtils.isEmpty(_taskId))
            Assert.assertTrue(_scheduleDateTime == null)

            remoteInstanceRecord!!.taskId
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(_taskId))
            Assert.assertTrue(_scheduleDateTime != null)

            _taskId!!
        }

    override val scheduleDate
        get() = if (remoteInstanceRecord != null) {
            Assert.assertTrue(TextUtils.isEmpty(_taskId))
            Assert.assertTrue(_scheduleDateTime == null)

            Date(remoteInstanceRecord!!.scheduleYear, remoteInstanceRecord!!.scheduleMonth, remoteInstanceRecord!!.scheduleDay)
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(_taskId))
            Assert.assertTrue(_scheduleDateTime != null)

            _scheduleDateTime!!.date
        }

    override val scheduleTime: Time
        get() = if (remoteInstanceRecord != null) {
            Assert.assertTrue(TextUtils.isEmpty(_taskId))
            Assert.assertTrue(_scheduleDateTime == null)

            val customTimeId = remoteInstanceRecord!!.scheduleCustomTimeId
            val hour = remoteInstanceRecord!!.scheduleHour
            val minute = remoteInstanceRecord!!.scheduleMinute

            Assert.assertTrue(hour == null == (minute == null))
            Assert.assertTrue(customTimeId == null != (hour == null))

            customTimeId?.let { remoteProject.getRemoteCustomTime(it) }
                    ?: NormalTime(hour!!, minute!!)
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(_taskId))
            Assert.assertTrue(_scheduleDateTime != null)

            _scheduleDateTime!!.time
        }

    override val taskKey by lazy { task.taskKey }

    override val done get() = remoteInstanceRecord?.done?.let { ExactTimeStamp(it) }

    override val instanceDate
        get() = if (remoteInstanceRecord != null) {
            Assert.assertTrue(_taskId == null)
            Assert.assertTrue(_scheduleDateTime == null)

            if (remoteInstanceRecord!!.instanceYear != null) {
                Assert.assertTrue(remoteInstanceRecord!!.instanceMonth != null)
                Assert.assertTrue(remoteInstanceRecord!!.instanceDay != null)

                Date(remoteInstanceRecord!!.instanceYear!!, remoteInstanceRecord!!.instanceMonth!!, remoteInstanceRecord!!.instanceDay!!)
            } else {
                Assert.assertTrue(remoteInstanceRecord!!.instanceMonth == null)
                Assert.assertTrue(remoteInstanceRecord!!.instanceDay == null)

                scheduleDate
            }
        } else {
            Assert.assertTrue(_taskId != null)
            Assert.assertTrue(_scheduleDateTime != null)

            _scheduleDateTime!!.date
        }

    override val instanceTime
        get() = if (remoteInstanceRecord != null) {
            Assert.assertTrue(_taskId == null)
            Assert.assertTrue(_scheduleDateTime == null)

            Assert.assertTrue(remoteInstanceRecord!!.instanceHour == null == (remoteInstanceRecord!!.instanceMinute == null))
            Assert.assertTrue(remoteInstanceRecord!!.instanceHour == null || remoteInstanceRecord!!.instanceCustomTimeId == null)

            when {
                remoteInstanceRecord!!.instanceCustomTimeId != null -> remoteProject.getRemoteCustomTime(remoteInstanceRecord!!.instanceCustomTimeId!!)
                remoteInstanceRecord!!.instanceHour != null -> NormalTime(remoteInstanceRecord!!.instanceHour!!, remoteInstanceRecord!!.instanceMinute!!)
                else -> scheduleTime
            }
        } else {
            Assert.assertTrue(_taskId != null)
            Assert.assertTrue(_scheduleDateTime != null)

            _scheduleDateTime!!.time
        }

    override val name get() = task.name

    private val remoteFactory get() = domainFactory.remoteFactory!!

    override val notified get() = instanceShownRecord?.notified == true

    override val notificationShown get() = instanceShownRecord?.notificationShown == true

    override val scheduleCustomTimeKey
        get() = if (remoteInstanceRecord != null) {
            Assert.assertTrue(TextUtils.isEmpty(_taskId))
            Assert.assertTrue(_scheduleDateTime == null)

            val customTimeId = remoteInstanceRecord!!.scheduleCustomTimeId

            customTimeId?.let { domainFactory.getCustomTimeKey(remoteProject.id, it) }
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(_taskId))
            Assert.assertTrue(_scheduleDateTime != null)

            val customTimeKey = _scheduleDateTime!!.time.timePair.customTimeKey
            if (customTimeKey == null) {
                null
            } else {
                if (!TextUtils.isEmpty(customTimeKey.remoteCustomTimeId)) {
                    Assert.assertTrue(!TextUtils.isEmpty(customTimeKey.remoteProjectId))

                    domainFactory.getCustomTimeKey(customTimeKey.remoteProjectId!!, customTimeKey.remoteCustomTimeId!!)
                } else {
                    customTimeKey
                }
            }
        }

    override val scheduleHourMinute
        get() = if (remoteInstanceRecord != null) {
            Assert.assertTrue(TextUtils.isEmpty(_taskId))
            Assert.assertTrue(_scheduleDateTime == null)

            val hour = remoteInstanceRecord!!.scheduleHour
            val minute = remoteInstanceRecord!!.scheduleMinute

            if (hour == null) {
                Assert.assertTrue(minute == null)

                null
            } else {
                Assert.assertTrue(minute != null)

                HourMinute(hour, minute!!)
            }
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(_taskId))
            Assert.assertTrue(_scheduleDateTime != null)

            _scheduleDateTime!!.time.timePair.hourMinute
        }

    override val task get() = remoteProject.getRemoteTaskForce(taskId)

    override val remoteNullableProject get() = task.remoteProject

    override val remoteNonNullProject get() = task.remoteProject

    override val remoteCustomTimeKey // scenario already covered by task/schedule relevance
        get() = remoteInstanceRecord?.instanceCustomTimeId?.let { Pair(remoteProject.id, it) }

    constructor(domainFactory: DomainFactory, remoteProject: RemoteProject, remoteInstanceRecord: RemoteInstanceRecord, instanceShownRecord: InstanceShownRecord?, now: ExactTimeStamp) : super(domainFactory) {
        this.remoteProject = remoteProject
        this.remoteInstanceRecord = remoteInstanceRecord
        _taskId = null
        _scheduleDateTime = null
        this.instanceShownRecord = instanceShownRecord

        val date = instanceDate
        val instanceTimeStamp = ExactTimeStamp(date, instanceTime.getHourMinute(date.dayOfWeek).toHourMilli())
        if (this.instanceShownRecord != null && (this.remoteInstanceRecord!!.done != null || instanceTimeStamp > now))
            this.instanceShownRecord!!.notified = false
    }

    constructor(domainFactory: DomainFactory, remoteProject: RemoteProject, taskId: String, scheduleDateTime: DateTime, instanceShownRecord: InstanceShownRecord?) : super(domainFactory) {
        Assert.assertTrue(!TextUtils.isEmpty(taskId))

        this.remoteProject = remoteProject
        remoteInstanceRecord = null
        _taskId = taskId
        _scheduleDateTime = scheduleDateTime
        this.instanceShownRecord = instanceShownRecord
    }

    override fun exists(): Boolean {
        Assert.assertTrue(remoteInstanceRecord == null != (_scheduleDateTime == null))
        Assert.assertTrue(_taskId == null == (_scheduleDateTime == null))

        return remoteInstanceRecord != null
    }

    override fun setInstanceDateTime(date: Date, timePair: TimePair, now: ExactTimeStamp) {
        Assert.assertTrue(isRootInstance(now))

        if (remoteInstanceRecord == null)
            createInstanceHierarchy(now)

        remoteInstanceRecord!!.setInstanceYear(date.year)
        remoteInstanceRecord!!.setInstanceMonth(date.month)
        remoteInstanceRecord!!.setInstanceDay(date.day)

        if (timePair.customTimeKey != null) {
            Assert.assertTrue(timePair.hourMinute == null)
            remoteInstanceRecord!!.instanceCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.customTimeKey, remoteProject)
            remoteInstanceRecord!!.instanceHour = null
            remoteInstanceRecord!!.instanceMinute = null
        } else {
            Assert.assertTrue(timePair.hourMinute != null)

            remoteInstanceRecord!!.instanceCustomTimeId = null
            remoteInstanceRecord!!.instanceHour = timePair.hourMinute!!.hour
            remoteInstanceRecord!!.instanceMinute = timePair.hourMinute.minute
        }

        if (instanceShownRecord == null)
            createInstanceShownRecord()

        Assert.assertTrue(instanceShownRecord != null)

        instanceShownRecord!!.notified = false
    }

    private fun createInstanceShownRecord() {
        Assert.assertTrue(instanceShownRecord == null)

        instanceShownRecord = domainFactory.localFactory.createInstanceShownRecord(domainFactory, taskId, scheduleDateTime, task.remoteProject.id)
    }

    override fun createInstanceHierarchy(now: ExactTimeStamp) {
        Assert.assertTrue(remoteInstanceRecord == null != (_scheduleDateTime == null))
        Assert.assertTrue(_taskId == null == (_scheduleDateTime == null))

        val parentInstance = getParentInstance(now)
        parentInstance?.createInstanceHierarchy(now)

        if (remoteInstanceRecord == null)
            createInstanceRecord(now)
    }

    private fun createInstanceRecord(now: ExactTimeStamp) {
        val task = task

        val scheduleDateTime = scheduleDateTime

        val remoteProjectFactory = domainFactory.remoteFactory
        Assert.assertTrue(remoteProjectFactory != null)

        remoteInstanceRecord = task.createRemoteInstanceRecord(this, scheduleDateTime, now)

        _taskId = null
        _scheduleDateTime = null
    }

    override fun setNotificationShown(notificationShown: Boolean, now: ExactTimeStamp) {
        if (instanceShownRecord == null)
            createInstanceShownRecord()

        Assert.assertTrue(instanceShownRecord != null)

        instanceShownRecord!!.notificationShown = notificationShown
    }

    override fun setDone(done: Boolean, now: ExactTimeStamp) {
        if (done) {
            if (remoteInstanceRecord == null)
                createInstanceHierarchy(now)

            Assert.assertTrue(remoteInstanceRecord != null)

            remoteInstanceRecord!!.done = now.long

            if (instanceShownRecord != null)
                instanceShownRecord!!.notified = false
        } else {
            Assert.assertTrue(remoteInstanceRecord != null)

            remoteInstanceRecord!!.done = null
        }
    }

    override fun setNotified(now: ExactTimeStamp) {
        if (instanceShownRecord == null)
            createInstanceShownRecord()

        Assert.assertTrue(instanceShownRecord != null)

        instanceShownRecord!!.notified = true
    }

    override fun delete() {
        Assert.assertTrue(remoteInstanceRecord != null)

        val remoteProjectFactory = domainFactory.remoteFactory
        Assert.assertTrue(remoteProjectFactory != null)

        task.deleteInstance(this)

        remoteInstanceRecord!!.delete()
    }

    override fun belongsToRemoteProject() = true

    override fun getNullableOrdinal() = remoteInstanceRecord?.ordinal

    override fun setOrdinal(ordinal: Double, now: ExactTimeStamp) {
        if (remoteInstanceRecord == null)
            createInstanceHierarchy(now)

        remoteInstanceRecord!!.setOrdinal(ordinal)
    }
}
