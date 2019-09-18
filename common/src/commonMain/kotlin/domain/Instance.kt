package com.krystianwsul.common.domain

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.models.RemoteProject
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*
import com.soywiz.klock.days

abstract class Instance {

    companion object {

        fun getNotificationId(scheduleDate: Date, scheduleCustomTimeKey: CustomTimeKey<*>?, scheduleHourMinute: HourMinute?, taskKey: TaskKey) = getNotificationId(scheduleDate, scheduleCustomTimeKey?.let { Pair(it.remoteProjectId, it.remoteCustomTimeId.value) }, scheduleHourMinute, taskKey)

        /*
        I'm going to make some assumptions here:
            1. I won't live past a hundred years
            2. scheduleYear is between 2016 and 2088 (that way the algorithm should be fine during my lifetime)
            3. scheduleCustomTimeId is between 1 and 10,000
            4. hash looping past Integer.MAX_VALUE isn't likely to cause collisions
         */

        fun getNotificationId(scheduleDate: Date, scheduleCustomTimeData: Pair<String, String>?, scheduleHourMinute: HourMinute?, taskKey: TaskKey): Int {
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

    protected abstract val instanceData: InstanceData<*, *, *>

    val instanceKey get() = InstanceKey(taskKey, scheduleKey)

    val scheduleKey get() = ScheduleKey(scheduleDate, TimePair(scheduleCustomTimeKey, scheduleHourMinute))

    val scheduleDate get() = instanceData.scheduleDate

    private val scheduleTime get() = instanceData.scheduleTime

    val instanceDate get() = instanceData.instanceDate

    val instanceTime get() = instanceData.instanceTime

    abstract val scheduleCustomTimeKey: CustomTimeKey<*>?

    private val scheduleHourMinute
        get() = instanceData.let {
            when (it) {
                is InstanceData.Real<*, *, *> -> it.instanceRecord.let { record -> record.scheduleHour?.let { HourMinute(it, record.scheduleMinute!!) } }
                is InstanceData.Virtual<*, *, *> -> it.scheduleDateTime
                        .time
                        .timePair
                        .hourMinute
            }
        }

    val scheduleDateTime get() = DateTime(scheduleDate, scheduleTime)

    val taskKey by lazy { task.taskKey }

    val done get() = instanceData.done?.let { ExactTimeStamp(it) }

    val instanceDateTime get() = DateTime(instanceDate, instanceTime)

    val name get() = task.name

    abstract val task: Task

    val instanceTimePair get() = TimePair(instanceCustomTimeKey, instanceHourMinute)

    private val instanceCustomTimeKey get() = (instanceTime as? CustomTime)?.customTimeKey

    private val instanceHourMinute get() = (instanceTime as? NormalTime)?.hourMinute

    /*
    Has the instance's notification been dismissed? Meaningful only if the instance is a root
    instance, in the past, and not done.  If either of the last two are changed, this flag gets
    reset.  As far as being a root instance, there's no simple way to catch that moment.
     */
    abstract fun getNotified(shownFactory: ShownFactory): Boolean

    abstract fun getNotificationShown(shownFactory: ShownFactory): Boolean // Is the notification visible?

    val notificationId get() = getNotificationId(scheduleDate, scheduleCustomTimeKey, scheduleHourMinute, taskKey)

    abstract val project: RemoteProject<*>

    abstract val customTimeKey: Pair<String, RemoteCustomTimeId>?

    abstract fun getShown(shownFactory: ShownFactory): Shown?

    abstract fun setNotified(shownFactory: ShownFactory, notified: Boolean)

    abstract fun setNotificationShown(shownFactory: ShownFactory, notificationShown: Boolean)

    fun exists() = (instanceData is InstanceData.Real)

    fun getChildInstances(now: ExactTimeStamp): List<Pair<Instance, TaskHierarchy>> {
        val hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now).first

        val task = task

        val scheduleDateTime = scheduleDateTime

        val taskHierarchies = task.getChildTaskHierarchies()
        val childInstances = HashMap<InstanceKey, Pair<Instance, TaskHierarchy>>()
        for (taskHierarchy in taskHierarchies) {
            if (taskHierarchy.notDeleted(hierarchyExactTimeStamp) && taskHierarchy.childTask.notDeleted(hierarchyExactTimeStamp)) {
                val childInstance = taskHierarchy.childTask.getInstance(scheduleDateTime)

                val parentInstance = childInstance.getParentInstance(now)
                if (parentInstance?.instanceKey == instanceKey)
                    childInstances[childInstance.instanceKey] = Pair(childInstance, taskHierarchy)
            }
        }

        return ArrayList(childInstances.values)
    }

    private fun getHierarchyExactTimeStamp(now: ExactTimeStamp) = listOfNotNull(
            Pair(now, "now"),
            Pair(scheduleDateTime.timeStamp.toExactTimeStamp(), "schedule"),
            task.getEndExactTimeStamp()?.let { Pair(it.minusOne(), "task end") },
            done?.let { Pair(it.minusOne(), "done") }).minBy { it.first }!!

    fun isRootInstance(now: ExactTimeStamp) = getParentInstance(now) == null

    fun getDisplayData(now: ExactTimeStamp) = if (isRootInstance(now)) instanceDateTime else null

    abstract fun setInstanceDateTime(shownFactory: ShownFactory, ownerKey: String, dateTime: DateTime, now: ExactTimeStamp)

    fun createInstanceHierarchy(now: ExactTimeStamp): InstanceData.Real<*, *, *> {
        (instanceData as? InstanceData.Real)?.let {
            return it
        }

        getParentInstance(now)?.createInstanceHierarchy(now)

        return createInstanceRecord(now)
    }

    protected abstract fun createInstanceRecord(now: ExactTimeStamp): InstanceData.Real<*, *, *>

    abstract fun setDone(uuid: String, shownFactory: ShownFactory, done: Boolean, now: ExactTimeStamp)

    fun isVisible(now: ExactTimeStamp, hack24: Boolean): Boolean {
        val isVisible = isVisibleHelper(now, hack24)

        if (isVisible && isRootInstance(now)) { // root because oldest visible now checked only for task's own schedules
            val task = task

            val oldestVisible = task.getOldestVisible()
            val date = scheduleDate

            if (oldestVisible != null && date < oldestVisible) {
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
        if (instanceData.hidden)
            return false

        if (task.run { !notDeleted(now) && getEndData()!!.deleteInstances && done == null })
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

    fun getParentInstance(now: ExactTimeStamp): Instance? {
        val hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now)

        val task = task

        val parentTask = task.getParentTask(hierarchyExactTimeStamp.first) ?: return null

        fun message(task: Task) = "name: ${task.name}, start: ${task.startExactTimeStamp}, end: " + task.getEndExactTimeStamp()

        if (!parentTask.current(hierarchyExactTimeStamp.first)) {
            ErrorLogger.instance.logException(ParentInstanceException("instance: " + toString() + ", task: " + message(task) + ", parentTask: " + message(parentTask) + ", hierarchy: " + hierarchyExactTimeStamp))
            return null
        }

        return parentTask.getInstance(scheduleDateTime)
    }

    abstract fun delete()

    override fun toString() = super.toString() + " name: " + name + ", schedule time: " + scheduleDateTime + " instance time: " + instanceDateTime + ", done: " + done

    abstract fun belongsToRemoteProject(): Boolean

    private class ParentInstanceException(message: String) : Exception(message)

    protected abstract fun getNullableOrdinal(): Double?

    val ordinal get() = getNullableOrdinal() ?: task.startExactTimeStamp.long.toDouble()

    fun setOrdinal(ordinal: Double, now: ExactTimeStamp) {
        createInstanceHierarchy(now)

        (instanceData as InstanceData.Real).instanceRecord.ordinal = ordinal
    }

    fun hide(uuid: String, now: ExactTimeStamp) {
        check(!instanceData.hidden)

        createInstanceHierarchy(now).instanceRecord.hidden = true

        task.updateOldestVisible(uuid, now)
    }

    val hidden get() = instanceData.hidden

    abstract fun getCreateTaskTimePair(ownerKey: String): TimePair // todo use for all CreateTaskActivity schedule hints.  Either filter by current, or add non-current to create task data

    fun getParentName(now: ExactTimeStamp) = getParentInstance(now)?.name ?: project.name

    interface Shown {

        var notified: Boolean
        var notificationShown: Boolean
    }

    interface ShownFactory {

        fun createShown(remoteTaskId: String, scheduleDateTime: DateTime, projectId: String): Shown

        fun getShown(
                projectId: String,
                taskId: String,
                scheduleYear: Int,
                scheduleMonth: Int,
                scheduleDay: Int,
                scheduleCustomTimeId: RemoteCustomTimeId?,
                scheduleHour: Int?,
                scheduleMinute: Int?): Shown?

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
                    minute)
        }
    }
}
