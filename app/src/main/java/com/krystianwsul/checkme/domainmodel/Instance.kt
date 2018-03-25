package com.krystianwsul.checkme.domainmodel

import android.content.Context
import android.support.v4.util.Pair
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.ScheduleKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import io.reactivex.annotations.Nullable
import java.util.*
import kotlin.collections.HashMap

abstract class Instance protected constructor(protected val domainFactory: DomainFactory) {

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

    val instanceKey get() = InstanceKey(taskKey, scheduleKey)

    val scheduleKey get() = ScheduleKey(scheduleDate, TimePair(scheduleCustomTimeKey, scheduleHourMinute))

    abstract val scheduleDate: Date

    abstract val scheduleCustomTimeKey: CustomTimeKey?

    protected abstract val scheduleHourMinute: HourMinute?

    protected abstract val scheduleTime: Time

    val scheduleDateTime get() = DateTime(scheduleDate, scheduleTime)

    abstract val taskKey: TaskKey

    abstract val done: ExactTimeStamp?

    val instanceDateTime get() = DateTime(instanceDate, instanceTime)

    protected abstract val instanceTime: Time

    abstract val name: String

    abstract val task: Task

    val instanceTimePair get() = TimePair(instanceCustomTimeKey, instanceHourMinute)

    protected val instanceCustomTimeKey get() = (instanceTime as? CustomTime)?.customTimeKey

    private val instanceHourMinute get() = (instanceTime as? NormalTime)?.hourMinute

    abstract val instanceDate: Date

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

    abstract val remoteNullableProject: RemoteProject?

    abstract val remoteNonNullProject: RemoteProject

    abstract val remoteCustomTimeKey: Pair<String, String>?

    abstract fun exists(): Boolean

    fun getChildInstances(now: ExactTimeStamp): List<Pair<Instance, TaskHierarchy>> {
        val hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now)

        val task = task

        val scheduleDateTime = scheduleDateTime

        val taskHierarchies = task.getTaskHierarchiesByParentTaskKey(task.taskKey)
        val childInstances = HashMap<InstanceKey, Pair<Instance, TaskHierarchy>>()
        for (taskHierarchy in taskHierarchies) {
            checkNotNull(taskHierarchy)

            val childTaskKey = taskHierarchy!!.childTaskKey

            if (taskHierarchy.notDeleted(hierarchyExactTimeStamp) && taskHierarchy.childTask.notDeleted(hierarchyExactTimeStamp)) {
                val childInstance = domainFactory.getInstance(childTaskKey, scheduleDateTime)

                val parentInstance = childInstance.getParentInstance(now)
                if (parentInstance?.instanceKey == instanceKey)
                    childInstances[childInstance.instanceKey] = Pair.create(childInstance, taskHierarchy)
            }
        }

        return ArrayList(childInstances.values)
    }

    private fun getHierarchyExactTimeStamp(now: ExactTimeStamp): ExactTimeStamp {
        val exactTimeStamps = ArrayList<ExactTimeStamp>()

        exactTimeStamps.add(now)

        task.endExactTimeStamp?.let { exactTimeStamps.add(it.minusOne()) }

        done?.let { exactTimeStamps.add(it.minusOne()) }

        exactTimeStamps.add(scheduleDateTime.timeStamp.toExactTimeStamp())

        return Collections.min(exactTimeStamps)
    }

    fun isRootInstance(now: ExactTimeStamp) = getParentInstance(now) == null

    fun getDisplayText(context: Context, now: ExactTimeStamp) = if (isRootInstance(now)) instanceDateTime.getDisplayText(context) else null

    abstract fun setInstanceDateTime(date: Date, timePair: TimePair, now: ExactTimeStamp)

    abstract fun createInstanceHierarchy(now: ExactTimeStamp)

    abstract fun setNotificationShown(notificationShown: Boolean, now: ExactTimeStamp)

    abstract fun setDone(done: Boolean, now: ExactTimeStamp)

    abstract fun setNotified(now: ExactTimeStamp)

    protected fun isVisible(now: ExactTimeStamp): Boolean {
        val isVisible = isVisibleHelper(now)

        if (isVisible) {
            val task = task

            val oldestVisible = task.oldestVisible
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

        val parentTask = task.getParentTask(hierarchyExactTimeStamp) ?: return null

        fun Task.message() = "name: $name, start: $startExactTimeStamp, end: $endExactTimeStamp"

        if (!parentTask.current(hierarchyExactTimeStamp))
            throw ParentInstanceException("instance: " + toString() + ", task: " + task.message() + ", parentTask: " + parentTask.message() + ", hierarchy: " + hierarchyExactTimeStamp)

        return domainFactory.getInstance(parentTask.taskKey, scheduleDateTime)
    }

    abstract fun delete()

    override fun toString() = super.toString() + " name: " + name + ", schedule time: " + scheduleDateTime + " instance time: " + instanceDateTime + ", done: " + done

    abstract fun belongsToRemoteProject(): Boolean

    private class ParentInstanceException(message: String) : Exception(message)

    @Nullable
    protected abstract fun getNullableOrdinal(): Double?

    val ordinal get() = getNullableOrdinal() ?: task.startExactTimeStamp.long.toDouble()
}
