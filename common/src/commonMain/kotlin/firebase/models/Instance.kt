package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*
import com.soywiz.klock.days

class Instance<T : RemoteCustomTimeId, U : ProjectKey> {

    companion object {

        fun getNotificationId(
                scheduleDate: Date,
                scheduleCustomTimeKey: CustomTimeKey<*, *>?,
                scheduleHourMinute: HourMinute?,
                taskKey: TaskKey
        ) = getNotificationId(
                scheduleDate,
                scheduleCustomTimeKey?.let { Pair(it.remoteProjectId.key, it.remoteCustomTimeId.value) },
                scheduleHourMinute,
                taskKey.run { Pair(remoteProjectId.key, remoteTaskId) }
        )

        /*
        I'm going to make some assumptions here:
            1. I won't live past a hundred years
            2. scheduleYear is between 2016 and 2088 (that way the algorithm should be fine during my lifetime)
            3. scheduleCustomTimeId is between 1 and 10,000
            4. hash looping past Integer.MAX_VALUE isn't likely to cause collisions
         */

        fun getNotificationId(
                scheduleDate: Date,
                scheduleCustomTimeData: Pair<String, String>?,
                scheduleHourMinute: HourMinute?,
                taskKey: Pair<String, String>
        ): Int {
            check(scheduleCustomTimeData == null != (scheduleHourMinute == null))

            var hash = scheduleDate.month
            hash += 12 * scheduleDate.day
            hash += 12 * 31 * (scheduleDate.year - 2015)

            if (scheduleCustomTimeData == null) {
                hash += 12 * 31 * 73 * (scheduleHourMinute!!.hour + 1)
                hash += 12 * 31 * 73 * 24 * (scheduleHourMinute.minute + 1)
            } else {
                hash += 12 * 31 * 73 * 24 * 60 * scheduleCustomTimeData.hashCode()
            }

            @Suppress("INTEGER_OVERFLOW")
            hash += 12 * 31 * 73 * 24 * 60 * 10000 * taskKey.hashCode()

            return hash
        }
    }

    private val remoteProject: Project<T, U>

    private var data: Data<T>

    private val shownHolder = ShownHolder()

    val instanceKey get() = InstanceKey(taskKey, scheduleKey)

    val scheduleKey get() = ScheduleKey(scheduleDate, TimePair(scheduleCustomTimeKey, scheduleHourMinute))

    val scheduleDate get() = data.scheduleDate

    private val scheduleTime get() = data.scheduleTime

    val instanceDate get() = data.instanceDate

    val instanceTime get() = data.instanceTime

    private val scheduleHourMinute
        get() = data.let {
            when (it) {
                is Data.Real<*> -> it.instanceRecord.let { record ->
                    record.scheduleHour?.let { HourMinute(it, record.scheduleMinute!!) }
                }
                is Data.Virtual<*> -> it.scheduleDateTime
                        .time
                        .timePair
                        .hourMinute
            }
        }

    val scheduleDateTime get() = DateTime(scheduleDate, scheduleTime)

    val taskKey by lazy { task.taskKey }

    val done get() = data.done?.let { ExactTimeStamp(it) }

    val instanceDateTime get() = DateTime(instanceDate, instanceTime)

    val name get() = task.name

    val instanceTimePair get() = TimePair(instanceCustomTimeKey, instanceHourMinute)

    @Suppress("UNCHECKED_CAST")
    val instanceCustomTimeKey
        get() = (instanceTime as? CustomTime<T, U>)?.key

    private val instanceHourMinute get() = (instanceTime as? NormalTime)?.hourMinute

    val notificationId get() = getNotificationId(scheduleDate, scheduleCustomTimeKey, scheduleHourMinute, taskKey)

    fun exists() = (data is Data.Real)

    fun getChildInstances(now: ExactTimeStamp): List<Pair<Instance<T, U>, TaskHierarchy<*, *>>> {
        val hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now).first

        val scheduleDateTime = scheduleDateTime

        return task.getChildTaskHierarchies()
                .filter { it.notDeleted(hierarchyExactTimeStamp) && it.childTask.notDeleted(hierarchyExactTimeStamp) }
                .map { Pair(it, it.childTask.getInstance(scheduleDateTime)) }
                .filter { it.second.getParentInstance(now)?.instanceKey == instanceKey }
                .associate { (taskHierarchy, childInstance) -> childInstance.instanceKey to Pair(childInstance, taskHierarchy) }
                .values
                .toList()
    }

    private fun getHierarchyExactTimeStamp(now: ExactTimeStamp) = listOfNotNull(
            Pair(now, "now"),
            Pair(scheduleDateTime.timeStamp.toExactTimeStamp(), "schedule"),
            task.getEndExactTimeStamp()?.let { Pair(it.minusOne(), "task end") },
            done?.let { Pair(it.minusOne(), "done") }
    ).minBy { it.first }!!

    fun isRootInstance(now: ExactTimeStamp) = getParentInstance(now) == null

    fun getDisplayData(now: ExactTimeStamp) = if (isRootInstance(now)) instanceDateTime else null

    private fun createInstanceHierarchy(now: ExactTimeStamp): Data.Real<*> {
        (data as? Data.Real)?.let {
            return it
        }

        getParentInstance(now)?.createInstanceHierarchy(now)

        return createInstanceRecord()
    }

    fun isVisible(now: ExactTimeStamp, hack24: Boolean): Boolean {
        val isVisible = isVisibleHelper(now, hack24)

        if (isVisible && isRootInstance(now)) { // root because oldest visible now checked only for task's own schedules
            val date = scheduleDate

            if (task.getOldestVisible()?.let { date < it } == true) {
                if (exists()) {
                    task.correctOldestVisible(date) // po pierwsze bo syf straszny, po drugie dlatego że edycja z root na child może dodać instances w przeszłości
                } else {
                    return false
                }
            }
        }

        return isVisible
    }

    private fun isVisibleHelper(now: ExactTimeStamp, hack24: Boolean): Boolean {
        if (data.hidden)
            return false

        if (task.run { !notDeleted(now) && getEndData()!!.deleteInstances && done == null }) // todo it doesn't make sense to update this after setting done, because of the 24 hour delay
            return false

        val parentInstance = getParentInstance(now)
        if (parentInstance != null) {
            return parentInstance.isVisible(now, hack24)
        } else {
            val done = done

            return if (done != null) {
                val cutoff = if (hack24) {
                    ExactTimeStamp(now.toDateTimeSoy() - 1.days)
                } else {
                    ExactTimeStamp.now
                }

                (done > cutoff)
            } else {
                true
            }
        }
    }

    fun getParentInstance(now: ExactTimeStamp): Instance<T, U>? {
        val hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now)

        val parentTask = task.getParentTask(hierarchyExactTimeStamp.first) ?: return null

        fun message(task: Task<*, *>) = "name: ${task.name}, start: ${task.startExactTimeStamp}, end: " + task.getEndExactTimeStamp()

        if (!parentTask.notDeleted(hierarchyExactTimeStamp.first)) {
            ErrorLogger.instance.logException(ParentInstanceException("instance: " + toString() + ", task: " + message(task) + ", parentTask: " + message(parentTask) + ", hierarchy: " + hierarchyExactTimeStamp))
            return null
        }

        return parentTask.getInstance(scheduleDateTime)
    }

    override fun toString() = super.toString() + " name: " + name + ", schedule time: " + scheduleDateTime + " instance time: " + instanceDateTime + ", done: " + done

    private class ParentInstanceException(message: String) : Exception(message)

    val ordinal get() = getNullableOrdinal() ?: task.startExactTimeStamp.long.toDouble()

    fun setOrdinal(ordinal: Double, now: ExactTimeStamp) {
        createInstanceHierarchy(now)

        (data as Data.Real).instanceRecord.ordinal = ordinal
    }

    fun hide(uuid: String, now: ExactTimeStamp) {
        check(!data.hidden)

        createInstanceHierarchy(now).instanceRecord.hidden = true

        task.updateOldestVisible(uuid, now)
    }

    val hidden get() = data.hidden

    fun getParentName(now: ExactTimeStamp) = getParentInstance(now)?.name ?: project.name

    fun getShown(shownFactory: ShownFactory) = shownHolder.getShown(shownFactory)

    /*
    Has the instance's notification been dismissed? Meaningful only if the instance is a root
    instance, in the past, and not done.  If either of the last two are changed, this flag gets
    reset.  As far as being a root instance, there's no simple way to catch that moment.
     */
    fun getNotified(shownFactory: ShownFactory) = getShown(shownFactory)?.notified == true

    fun setNotified(shownFactory: ShownFactory, notified: Boolean) {
        shownHolder.forceShown(shownFactory).notified = notified
    }

    // Is the notification visible?
    fun getNotificationShown(shownFactory: ShownFactory) = getShown(shownFactory)?.notificationShown == true

    fun setNotificationShown(shownFactory: ShownFactory, notificationShown: Boolean) {
        shownHolder.forceShown(shownFactory).notificationShown = notificationShown
    }

    val scheduleCustomTimeKey
        get() = data.let {
            when (it) {
                is Data.Real<T> -> it.instanceRecord
                        .scheduleKey
                        .scheduleTimePair
                        .customTimeKey
                is Data.Virtual<T> -> it.scheduleDateTime
                        .time
                        .timePair
                        .customTimeKey
            }
        }

    val task: Task<T, U>

    val project get() = remoteProject

    val customTimeKey // scenario already covered by task/schedule relevance
        get() = (data as? Data.Real<*>)?.instanceRecord
                ?.instanceJsonTime
                ?.let { (it as? JsonTime.Custom)?.let { Pair(remoteProject.id, it.id) } }

    constructor(
            project: Project<T, U>,
            task: Task<T, U>,
            instanceRecord: InstanceRecord<T>
    ) {
        this.remoteProject = project
        this.task = task
        val realInstanceData = Data.Real(this, instanceRecord)
        data = realInstanceData
    }

    constructor(
            project: Project<T, U>,
            task: Task<T, U>,
            scheduleDateTime: DateTime
    ) {
        this.remoteProject = project
        this.task = task
        data = Data.Virtual(scheduleDateTime)
    }

    fun fixNotificationShown(shownFactory: ShownFactory, now: ExactTimeStamp) {
        if (done != null || instanceDateTime.toExactTimeStamp() > now)
            getShown(shownFactory)?.notified = false
    }

    fun setInstanceDateTime(
            shownFactory: ShownFactory,
            ownerKey: UserKey,
            dateTime: DateTime,
            now: ExactTimeStamp
    ) {
        check(isRootInstance(now))

        createInstanceHierarchy(now)

        @Suppress("UNCHECKED_CAST")
        (data as Data.Real<T>).instanceRecord.let {
            it.instanceDate = dateTime.date

            it.instanceJsonTime = project.getOrCopyTime(ownerKey, dateTime.time).let {
                @Suppress("UNCHECKED_CAST")
                when (it) {
                    is CustomTime<*, *> -> JsonTime.Custom(it.key.remoteCustomTimeId as T)
                    is NormalTime -> JsonTime.Normal(it.hourMinute)
                    else -> throw IllegalArgumentException()
                }
            }
        }

        shownHolder.forceShown(shownFactory).notified = false
    }

    private fun createInstanceRecord() = Data.Real(
            this,
            task.createRemoteInstanceRecord(this, scheduleDateTime)
    ).also { data = it }

    fun setDone(uuid: String, shownFactory: ShownFactory, done: Boolean, now: ExactTimeStamp) {
        if (done) {
            createInstanceHierarchy(now).instanceRecord.done = now.long

            getShown(shownFactory)?.notified = false
        } else {
            (data as Data.Real<*>).instanceRecord.done = null
        }

        task.updateOldestVisible(uuid, now)
    }

    fun delete() {
        check(data is Data.Real<*>)

        task.deleteInstance(this)

        (data as Data.Real<*>).instanceRecord.delete()
    }

    fun belongsToRemoteProject() = true

    private fun getNullableOrdinal() = (data as? Data.Real<*>)?.instanceRecord?.ordinal

    // todo use for all CreateTaskActivity schedule hints.  Either filter by current, or add non-current to create task data
    fun getCreateTaskTimePair(ownerKey: UserKey): TimePair {
        val instanceTimePair = instanceTime.timePair
        val shared = instanceTimePair.customTimeKey as? CustomTimeKey.Shared

        return if (shared != null) {
            val sharedCustomTime = remoteProject.getRemoteCustomTime(shared.remoteCustomTimeId) as SharedCustomTime

            if (sharedCustomTime.ownerKey == ownerKey) {
                val privateCustomTimeKey = CustomTimeKey.Private(ownerKey.toPrivateProjectKey(), sharedCustomTime.privateKey!!)

                TimePair(privateCustomTimeKey)
            } else {
                val hourMinute = sharedCustomTime.getHourMinute(instanceDate.dayOfWeek)

                TimePair(hourMinute)
            }
        } else {
            instanceTimePair
        }
    }

    private sealed class Data<T : RemoteCustomTimeId> {

        abstract val scheduleDate: Date
        abstract val scheduleTime: Time

        abstract val instanceDate: Date
        abstract val instanceTime: Time

        abstract val done: Long?

        abstract val hidden: Boolean

        class Real<T : RemoteCustomTimeId>(
                private val instance: Instance<T, *>,
                val instanceRecord: InstanceRecord<T>
        ) : Data<T>() {

            fun getCustomTime(customTimeId: T) = instance.remoteProject.getRemoteCustomTime(customTimeId)

            override val scheduleDate get() = instanceRecord.let { Date(it.scheduleYear, it.scheduleMonth, it.scheduleDay) }

            override val scheduleTime
                get() = instanceRecord.run {
                    scheduleCustomTimeId?.let { getCustomTime(it) }
                            ?: NormalTime(scheduleHour!!, scheduleMinute!!)
                }

            override val instanceDate get() = instanceRecord.instanceDate ?: scheduleDate

            override val instanceTime
                get() = instanceRecord.instanceJsonTime
                        ?.let {
                            when (it) {
                                is JsonTime.Custom -> getCustomTime(it.id)
                                is JsonTime.Normal -> NormalTime(it.hourMinute)
                            }
                        }
                        ?: scheduleTime

            override val done get() = instanceRecord.done

            override val hidden get() = instanceRecord.hidden
        }

        class Virtual<T : RemoteCustomTimeId>(val scheduleDateTime: DateTime) : Data<T>() {

            override val scheduleDate by lazy { scheduleDateTime.date }

            override val scheduleTime = scheduleDateTime.time

            override val instanceDate by lazy { scheduleDate }

            override val instanceTime = scheduleTime

            override val done: Long? = null

            override val hidden = false
        }
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

    interface Shown {

        var notified: Boolean
        var notificationShown: Boolean
    }

    interface ShownFactory {

        fun createShown(remoteTaskId: String, scheduleDateTime: DateTime, projectId: ProjectKey): Shown

        fun getShown(
                projectId: ProjectKey,
                taskId: String,
                scheduleYear: Int,
                scheduleMonth: Int,
                scheduleDay: Int,
                scheduleCustomTimeId: RemoteCustomTimeId?,
                scheduleHour: Int?,
                scheduleMinute: Int?
        ): Shown?

        fun getShown(taskKey: TaskKey, scheduleDateTime: DateTime): Shown? {
            val (remoteCustomTimeId, hour, minute) = scheduleDateTime.time
                    .timePair
                    .destructureRemote()

            return getShown(
                    taskKey.remoteProjectId,
                    taskKey.remoteTaskId,
                    scheduleDateTime.date.year,
                    scheduleDateTime.date.month,
                    scheduleDateTime.date.day,
                    remoteCustomTimeId,
                    hour,
                    minute
            )
        }
    }
}
