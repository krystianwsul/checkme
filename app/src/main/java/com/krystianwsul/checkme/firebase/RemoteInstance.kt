package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.utils.time.*


class RemoteInstance : Instance {

    private val remoteProject: RemoteProject

    private var remoteInstanceRecord: RemoteInstanceRecord? = null

    private var _taskId: String? = null

    private var _scheduleDateTime: DateTime? = null

    private var instanceShownRecord: InstanceShownRecord? = null

    private val taskId
        get() = if (remoteInstanceRecord != null) {
            check(TextUtils.isEmpty(_taskId))
            check(_scheduleDateTime == null)

            remoteInstanceRecord!!.taskId
        } else {
            check(!TextUtils.isEmpty(_taskId))
            checkNotNull(_scheduleDateTime)

            _taskId!!
        }

    override val scheduleDate
        get() = if (remoteInstanceRecord != null) {
            check(TextUtils.isEmpty(_taskId))
            check(_scheduleDateTime == null)

            Date(remoteInstanceRecord!!.scheduleYear, remoteInstanceRecord!!.scheduleMonth, remoteInstanceRecord!!.scheduleDay)
        } else {
            check(!TextUtils.isEmpty(_taskId))
            checkNotNull(_scheduleDateTime)

            _scheduleDateTime!!.date
        }

    override val scheduleTime: Time
        get() = if (remoteInstanceRecord != null) {
            check(TextUtils.isEmpty(_taskId))
            check(_scheduleDateTime == null)

            val customTimeId = remoteInstanceRecord!!.scheduleCustomTimeId
            val hour = remoteInstanceRecord!!.scheduleHour
            val minute = remoteInstanceRecord!!.scheduleMinute

            check(hour == null == (minute == null))
            check(customTimeId == null != (hour == null))

            customTimeId?.let { remoteProject.getRemoteCustomTime(it) }
                    ?: NormalTime(hour!!, minute!!)
        } else {
            check(!TextUtils.isEmpty(_taskId))
            checkNotNull(_scheduleDateTime)

            _scheduleDateTime!!.time
        }

    override val taskKey by lazy { task.taskKey }

    override val done get() = remoteInstanceRecord?.done?.let { ExactTimeStamp(it) }

    override val instanceDate
        get() = if (remoteInstanceRecord != null) {
            check(_taskId == null)
            check(_scheduleDateTime == null)

            if (remoteInstanceRecord!!.instanceYear != null) {
                checkNotNull(remoteInstanceRecord!!.instanceMonth)
                checkNotNull(remoteInstanceRecord!!.instanceDay)

                Date(remoteInstanceRecord!!.instanceYear!!, remoteInstanceRecord!!.instanceMonth!!, remoteInstanceRecord!!.instanceDay!!)
            } else {
                check(remoteInstanceRecord!!.instanceMonth == null)
                check(remoteInstanceRecord!!.instanceDay == null)

                scheduleDate
            }
        } else {
            checkNotNull(_taskId)
            checkNotNull(_scheduleDateTime)

            _scheduleDateTime!!.date
        }

    override val instanceTime
        get() = if (remoteInstanceRecord != null) {
            check(_taskId == null)
            check(_scheduleDateTime == null)

            check(remoteInstanceRecord!!.instanceHour == null == (remoteInstanceRecord!!.instanceMinute == null))
            check(remoteInstanceRecord!!.instanceHour == null || remoteInstanceRecord!!.instanceCustomTimeId == null)

            when {
                remoteInstanceRecord!!.instanceCustomTimeId != null -> remoteProject.getRemoteCustomTime(remoteInstanceRecord!!.instanceCustomTimeId!!)
                remoteInstanceRecord!!.instanceHour != null -> NormalTime(remoteInstanceRecord!!.instanceHour!!, remoteInstanceRecord!!.instanceMinute!!)
                else -> scheduleTime
            }
        } else {
            checkNotNull(_taskId)
            checkNotNull(_scheduleDateTime)

            _scheduleDateTime!!.time
        }

    override val name get() = task.name

    private val remoteFactory get() = kotlinDomainFactory.remoteProjectFactory!!

    override val notified get() = instanceShownRecord?.notified == true

    override val notificationShown get() = instanceShownRecord?.notificationShown == true

    override val scheduleCustomTimeKey
        get() = if (remoteInstanceRecord != null) {
            check(TextUtils.isEmpty(_taskId))
            check(_scheduleDateTime == null)

            val customTimeId = remoteInstanceRecord!!.scheduleCustomTimeId

            customTimeId?.let { kotlinDomainFactory.getCustomTimeKey(remoteProject.id, it) }
        } else {
            check(!TextUtils.isEmpty(_taskId))
            checkNotNull(_scheduleDateTime)

            val customTimeKey = _scheduleDateTime!!.time.timePair.customTimeKey
            if (customTimeKey == null) {
                null
            } else {
                if (!TextUtils.isEmpty(customTimeKey.remoteCustomTimeId)) {
                    check(!TextUtils.isEmpty(customTimeKey.remoteProjectId))

                    kotlinDomainFactory.getCustomTimeKey(customTimeKey.remoteProjectId!!, customTimeKey.remoteCustomTimeId!!)
                } else {
                    customTimeKey
                }
            }
        }

    override val scheduleHourMinute
        get() = if (remoteInstanceRecord != null) {
            check(TextUtils.isEmpty(_taskId))
            check(_scheduleDateTime == null)

            val hour = remoteInstanceRecord!!.scheduleHour
            val minute = remoteInstanceRecord!!.scheduleMinute

            if (hour == null) {
                check(minute == null)

                null
            } else {
                checkNotNull(minute)

                HourMinute(hour, minute)
            }
        } else {
            check(!TextUtils.isEmpty(_taskId))
            checkNotNull(_scheduleDateTime)

            _scheduleDateTime!!.time.timePair.hourMinute
        }

    override val task get() = remoteProject.getRemoteTaskForce(taskId)

    override val remoteNullableProject get() = task.remoteProject

    override val remoteNonNullProject get() = task.remoteProject

    override val remoteCustomTimeKey // scenario already covered by task/schedule relevance
        get() = remoteInstanceRecord?.instanceCustomTimeId?.let { Pair(remoteProject.id, it) }

    constructor(
            kotlinDomainFactory: KotlinDomainFactory,
            remoteProject: RemoteProject,
            remoteInstanceRecord: RemoteInstanceRecord,
            instanceShownRecord: InstanceShownRecord?,
            now: ExactTimeStamp) : super(kotlinDomainFactory) {
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

    constructor(
            kotlinDomainFactory: KotlinDomainFactory,
            remoteProject: RemoteProject,
            taskId: String,
            scheduleDateTime: DateTime,
            instanceShownRecord: InstanceShownRecord?) : super(kotlinDomainFactory) {
        check(!TextUtils.isEmpty(taskId))

        this.remoteProject = remoteProject
        remoteInstanceRecord = null
        _taskId = taskId
        _scheduleDateTime = scheduleDateTime
        this.instanceShownRecord = instanceShownRecord
    }

    override fun exists(): Boolean {
        check(remoteInstanceRecord == null != (_scheduleDateTime == null))
        check(_taskId == null == (_scheduleDateTime == null))

        return remoteInstanceRecord != null
    }

    override fun setInstanceDateTime(date: Date, timePair: TimePair, now: ExactTimeStamp) {
        check(isRootInstance(now))

        if (remoteInstanceRecord == null)
            createInstanceHierarchy(now)

        remoteInstanceRecord!!.setInstanceYear(date.year)
        remoteInstanceRecord!!.setInstanceMonth(date.month)
        remoteInstanceRecord!!.setInstanceDay(date.day)

        if (timePair.customTimeKey != null) {
            check(timePair.hourMinute == null)
            remoteInstanceRecord!!.instanceCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.customTimeKey, remoteProject)
            remoteInstanceRecord!!.instanceHour = null
            remoteInstanceRecord!!.instanceMinute = null
        } else {
            checkNotNull(timePair.hourMinute)

            remoteInstanceRecord!!.instanceCustomTimeId = null
            remoteInstanceRecord!!.instanceHour = timePair.hourMinute.hour
            remoteInstanceRecord!!.instanceMinute = timePair.hourMinute.minute
        }

        if (instanceShownRecord == null)
            createInstanceShownRecord()

        checkNotNull(instanceShownRecord)

        instanceShownRecord!!.notified = false
    }

    private fun createInstanceShownRecord() {
        check(instanceShownRecord == null)

        instanceShownRecord = kotlinDomainFactory.localFactory.createInstanceShownRecord(kotlinDomainFactory, taskId, scheduleDateTime, task.remoteProject.id)
    }

    override fun createInstanceHierarchy(now: ExactTimeStamp) {
        check(remoteInstanceRecord == null != (_scheduleDateTime == null))
        check(_taskId == null == (_scheduleDateTime == null))

        getParentInstance(now)?.createInstanceHierarchy(now)

        if (remoteInstanceRecord == null)
            createInstanceRecord()
    }

    private fun createInstanceRecord() {
        val task = task

        val scheduleDateTime = scheduleDateTime

        remoteInstanceRecord = task.createRemoteInstanceRecord(this, scheduleDateTime)

        _taskId = null
        _scheduleDateTime = null
    }

    override fun setNotificationShown(notificationShown: Boolean, now: ExactTimeStamp) {
        if (instanceShownRecord == null)
            createInstanceShownRecord()

        checkNotNull(instanceShownRecord)

        instanceShownRecord!!.notificationShown = notificationShown
    }

    override fun setDone(done: Boolean, now: ExactTimeStamp) {
        if (done) {
            if (remoteInstanceRecord == null)
                createInstanceHierarchy(now)

            checkNotNull(remoteInstanceRecord)

            remoteInstanceRecord!!.done = now.long

            instanceShownRecord?.notified = false
        } else {
            checkNotNull(remoteInstanceRecord)

            remoteInstanceRecord!!.done = null
        }
    }

    override fun setNotified(now: ExactTimeStamp) {
        if (instanceShownRecord == null)
            createInstanceShownRecord()

        check(instanceShownRecord != null)

        instanceShownRecord!!.notified = true
    }

    override fun delete() {
        checkNotNull(remoteInstanceRecord)

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
