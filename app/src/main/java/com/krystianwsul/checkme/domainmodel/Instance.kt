package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import java.util.*
import kotlin.collections.HashMap

abstract class Instance(protected val domainFactory: DomainFactory) {

    companion object {

        fun getNotificationId(scheduleDate: Date, scheduleCustomTimeKey: CustomTimeKey?, scheduleHourMinute: HourMinute?, taskKey: TaskKey): Int {
            check(scheduleCustomTimeKey == null != (scheduleHourMinute == null))

            var hash = scheduleDate.month
            hash += 12 * scheduleDate.day
            hash += 12 * 31 * (scheduleDate.year - 2015)

            if (scheduleCustomTimeKey == null) {
                hash += 12 * 31 * 73 * (scheduleHourMinute!!.hour + 1)
                hash += 12 * 31 * 73 * 24 * (scheduleHourMinute.minute + 1)
            } else {
                hash += 12 * 31 * 73 * 24 * 60 * scheduleCustomTimeKey.hashCode()
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

    private val scheduleTime get() = instanceData.getScheduleTime(domainFactory)

    val instanceDate get() = instanceData.instanceDate

    val instanceTime get() = instanceData.getInstanceTime(domainFactory)

    abstract val scheduleCustomTimeKey: CustomTimeKey?

    private val scheduleHourMinute
        get() = instanceData.let {
            when (it) {
                is InstanceData.RealInstanceData<*, *, *> -> it.instanceRecord.let { record -> record.scheduleHour?.let { HourMinute(it, record.scheduleMinute!!) } }
                is InstanceData.VirtualInstanceData<*, *, *> -> it.scheduleDateTime
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

    val instanceCustomTimeKey get() = (instanceTime as? CustomTime)?.customTimeKey

    private val instanceHourMinute get() = (instanceTime as? NormalTime)?.hourMinute

    abstract val notified: Boolean

    /*
    I'm going to make some assumptions here:
        1. I won't live past a hundred years
        2. scheduleYear is between 2016 and 2088 (that way the algorithm should be fine during my lifetime)
        3. scheduleCustomTimeId is between 1 and 10,000
        4. hash looping past Integer.MAX_VALUE isn't likely to cause collisions
     */

    val notificationId get() = getNotificationId(scheduleDate, scheduleCustomTimeKey, scheduleHourMinute, taskKey)

    protected val scheduleTimePair get() = TimePair(scheduleCustomTimeKey, scheduleHourMinute)

    abstract val notificationShown: Boolean

    abstract val remoteNullableProject: RemoteProject<*>?

    abstract val remoteNonNullProject: RemoteProject<*>

    abstract val remoteCustomTimeKey: Pair<String, RemoteCustomTimeId>?

    abstract val nullableInstanceShownRecord: InstanceShownRecord?

    fun exists() = (instanceData is InstanceData.RealInstanceData)

    fun getChildInstances(now: ExactTimeStamp): List<Pair<Instance, TaskHierarchy>> {
        val hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now).first

        val task = task

        val scheduleDateTime = scheduleDateTime

        val taskHierarchies = task.getTaskHierarchiesByParentTaskKey(task.taskKey)
        val childInstances = HashMap<InstanceKey, Pair<Instance, TaskHierarchy>>()
        for (taskHierarchy in taskHierarchies) {
            checkNotNull(taskHierarchy)

            val childTaskKey = taskHierarchy.childTaskKey

            if (taskHierarchy.notDeleted(hierarchyExactTimeStamp) && taskHierarchy.childTask.notDeleted(hierarchyExactTimeStamp)) {
                val childInstance = domainFactory.getInstance(childTaskKey, scheduleDateTime)

                val parentInstance = childInstance.getParentInstance(now)
                if (parentInstance?.instanceKey == instanceKey)
                    childInstances[childInstance.instanceKey] = Pair(childInstance, taskHierarchy)
            }
        }

        return ArrayList(childInstances.values)
    }

    private fun getHierarchyExactTimeStamp(now: ExactTimeStamp): Pair<ExactTimeStamp, String> {
        val exactTimeStamps = mutableListOf(Pair(now, "now"))

        task.getEndExactTimeStamp()?.let { exactTimeStamps.add(Pair(it.minusOne(), "task end")) }

        done?.let { exactTimeStamps.add(Pair(it.minusOne(), "done")) }

        exactTimeStamps.add(Pair(scheduleDateTime.timeStamp.toExactTimeStamp(), "schedule"))

        return exactTimeStamps.minBy { it.first }!!
    }

    fun isRootInstance(now: ExactTimeStamp) = getParentInstance(now) == null

    fun getDisplayText(now: ExactTimeStamp) = if (isRootInstance(now)) instanceDateTime.getDisplayText() else null

    abstract fun setInstanceDateTime(date: Date, timePair: TimePair, now: ExactTimeStamp)

    fun createInstanceHierarchy(now: ExactTimeStamp) {
        if (instanceData is InstanceData.RealInstanceData)
            return

        getParentInstance(now)?.createInstanceHierarchy(now)

        createInstanceRecord(now)
    }

    protected abstract fun createInstanceRecord(now: ExactTimeStamp)

    abstract fun setNotificationShown(notificationShown: Boolean, now: ExactTimeStamp)

    abstract fun setDone(done: Boolean, now: ExactTimeStamp)

    abstract fun setNotified(now: ExactTimeStamp)

    fun isVisible(now: ExactTimeStamp): Boolean {
        val isVisible = isVisibleHelper(now)

        if (isVisible) {
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

    private fun isVisibleHelper(now: ExactTimeStamp): Boolean {
        val calendar = now.calendar
        calendar.add(Calendar.DAY_OF_YEAR, -1) // 24 hack
        val twentyFourHoursAgo = ExactTimeStamp(calendar)

        val parentInstance = getParentInstance(now)
        return parentInstance?.isVisible(now) ?: done?.let { it > twentyFourHoursAgo } ?: true
    }

    fun getParentInstance(now: ExactTimeStamp): Instance? {
        val hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now)

        val task = task

        val parentTask = task.getParentTask(hierarchyExactTimeStamp.first) ?: return null

        fun message(task: Task) = "name: ${task.name}, start: ${task.startExactTimeStamp}, end: " + task.getEndExactTimeStamp()

        if (!parentTask.current(hierarchyExactTimeStamp.first)) {
            MyCrashlytics.logException(ParentInstanceException("instance: " + toString() + ", task: " + message(task) + ", parentTask: " + message(parentTask) + ", hierarchy: " + hierarchyExactTimeStamp))
            return null
        }

        return domainFactory.getInstance(parentTask.taskKey, scheduleDateTime)
    }

    abstract fun delete()

    override fun toString() = super.toString() + " name: " + name + ", schedule time: " + scheduleDateTime + " instance time: " + instanceDateTime + ", done: " + done

    abstract fun belongsToRemoteProject(): Boolean

    private class ParentInstanceException(message: String) : Exception(message)

    protected abstract fun getNullableOrdinal(): Double?

    val ordinal get() = getNullableOrdinal() ?: task.startExactTimeStamp.long.toDouble()

    fun setOrdinal(ordinal: Double, now: ExactTimeStamp) {
        createInstanceHierarchy(now)

        (instanceData as InstanceData.RealInstanceData).instanceRecord.ordinal = ordinal
    }
}
