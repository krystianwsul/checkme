package com.krystianwsul.checkme.domainmodel

import android.content.Context
import android.util.Log
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.checkme.utils.ScheduleKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.HourMilli
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import java.util.*

abstract class Task(protected val kotlinDomainFactory: KotlinDomainFactory) {

    protected val domainFactory = kotlinDomainFactory.domainFactory

    abstract val startExactTimeStamp: ExactTimeStamp

    abstract val name: String

    abstract val note: String?

    abstract val taskKey: TaskKey

    abstract val schedules: Collection<Schedule>

    abstract fun getEndExactTimeStamp(): ExactTimeStamp?

    abstract fun getOldestVisible(): Date?

    abstract val existingInstances: Map<ScheduleKey, Instance>

    abstract val remoteNullableProject: RemoteProject?

    abstract val remoteNonNullProject: RemoteProject

    fun current(exactTimeStamp: ExactTimeStamp): Boolean {
        val startExactTimeStamp = startExactTimeStamp
        val endExactTimeStamp = getEndExactTimeStamp()

        return startExactTimeStamp <= exactTimeStamp && (endExactTimeStamp == null || endExactTimeStamp > exactTimeStamp)
    }

    fun getScheduleText(context: Context, exactTimeStamp: ExactTimeStamp): String? {
        check(current(exactTimeStamp))

        val currentSchedules = getCurrentSchedules(exactTimeStamp)

        if (isRootTask(exactTimeStamp)) {
            if (currentSchedules.isEmpty())
                return null

            check(currentSchedules.all { it.current(exactTimeStamp) })

            return currentSchedules.joinToString(", ") { it.getScheduleText(context) }
        } else {
            check(currentSchedules.isEmpty())
            return null
        }
    }

    protected fun getChildTaskHierarchies(exactTimeStamp: ExactTimeStamp): List<TaskHierarchy> {
        check(current(exactTimeStamp))

        return domainFactory.getChildTaskHierarchies(this, exactTimeStamp)
    }

    fun notDeleted(exactTimeStamp: ExactTimeStamp): Boolean {
        val endExactTimeStamp = getEndExactTimeStamp()

        return endExactTimeStamp == null || endExactTimeStamp > exactTimeStamp
    }

    fun isVisible(now: ExactTimeStamp): Boolean {
        if (current(now)) {
            val rootTask = getRootTask(now)

            val schedules = rootTask.getCurrentSchedules(now)

            if (schedules.isEmpty())
                return true

            if (schedules.any { it.isVisible(this, now) })
                return true
        }

        return false
    }// bo inheritance i testy

    private fun getRootTask(exactTimeStamp: ExactTimeStamp): Task {
        val parentTask = getParentTask(exactTimeStamp)
        return parentTask?.getRootTask(exactTimeStamp) ?: this
    }

    protected fun getCurrentSchedules(exactTimeStamp: ExactTimeStamp): List<Schedule> {
        check(current(exactTimeStamp))

        return schedules.filter { it.current(exactTimeStamp) }
    }

    fun isRootTask(exactTimeStamp: ExactTimeStamp): Boolean {
        check(current(exactTimeStamp))

        return getParentTask(exactTimeStamp) == null
    }

    protected abstract fun setMyEndExactTimeStamp(now: ExactTimeStamp)

    fun setEndExactTimeStamp(now: ExactTimeStamp) {
        check(current(now))

        val schedules = getCurrentSchedules(now)
        if (isRootTask(now)) {
            check(schedules.all { it.current(now) })

            schedules.forEach { it.setEndExactTimeStamp(now) }
        } else {
            check(schedules.isEmpty())
        }

        getChildTaskHierarchies(now).forEach { it.setEndExactTimeStamp(now) }

        val parentTaskHierarchy = domainFactory.getParentTaskHierarchy(this, now)
        if (parentTaskHierarchy != null) {
            check(parentTaskHierarchy.current(now))

            parentTaskHierarchy.setEndExactTimeStamp(now)
        }

        setMyEndExactTimeStamp(now)
    }

    abstract fun createChildTask(now: ExactTimeStamp, name: String, note: String?): Task

    fun getParentTask(exactTimeStamp: ExactTimeStamp): Task? {
        check(notDeleted(exactTimeStamp))

        return kotlinDomainFactory.getParentTask(this, exactTimeStamp)
    }

    protected fun updateOldestVisible(now: ExactTimeStamp) {
        // 24 hack
        val instances = kotlinDomainFactory.getPastInstances(this, now)

        val optional = instances.asSequence()
                .filter { it.isVisible(now) }
                .minBy { it.scheduleDateTime }

        var oldestVisible: Date

        if (optional != null) {
            oldestVisible = optional.scheduleDate

            if (oldestVisible > now.date)
                oldestVisible = now.date
        } else {
            oldestVisible = now.date
        }

        val oldOldestVisible = getOldestVisible()
        if (oldOldestVisible == null || oldOldestVisible != oldestVisible)
            setOldestVisible(oldestVisible)
    }

    fun correctOldestVisible(date: Date) {
        val oldestVisible = getOldestVisible()
        check(oldestVisible != null && date < oldestVisible)

        val message = "$name old oldest: $oldestVisible, new oldest: $date"

        Log.e("asdf", message)

        MyCrashlytics.logException(OldestVisibleException4(message))

        setOldestVisible(date) // miejmy nadzieję że coś to później zapisze. nota bene: mogą wygenerować się instances dla wcześniej ukończonych czasów
    }

    protected abstract fun setOldestVisible(date: Date)

    fun getInstances(givenStartExactTimeStamp: ExactTimeStamp?, givenEndExactTimeStamp: ExactTimeStamp, now: ExactTimeStamp): List<Instance> {
        var correctedStartExactTimeStamp = givenStartExactTimeStamp
        if (correctedStartExactTimeStamp == null) { // 24 hack
            val oldestVisible = getOldestVisible()
            if (oldestVisible != null) {
                val zero = HourMilli(0, 0, 0, 0)
                correctedStartExactTimeStamp = ExactTimeStamp(oldestVisible, zero)
            }
        }

        val myStartTimeStamp = startExactTimeStamp
        val myEndTimeStamp = getEndExactTimeStamp()

        val startExactTimeStamp: ExactTimeStamp
        val endExactTimeStamp: ExactTimeStamp

        startExactTimeStamp = if (correctedStartExactTimeStamp == null || correctedStartExactTimeStamp < myStartTimeStamp)
            myStartTimeStamp
        else
            correctedStartExactTimeStamp

        endExactTimeStamp = if (myEndTimeStamp == null || myEndTimeStamp > givenEndExactTimeStamp)
            givenEndExactTimeStamp
        else
            myEndTimeStamp

        val instances = ArrayList<Instance>()

        if (startExactTimeStamp >= endExactTimeStamp)
            return instances

        check(startExactTimeStamp < endExactTimeStamp)

        for (schedule in schedules)
            instances.addAll(schedule.getInstances(this, startExactTimeStamp, endExactTimeStamp))

        val taskHierarchies = getTaskHierarchiesByChildTaskKey(this.taskKey)

        instances.addAll(taskHierarchies.map { it.parentTask }
                .flatMap { it.getInstances(startExactTimeStamp, endExactTimeStamp, now) }
                .flatMap { instance -> instance.getChildInstances(now) }
                .asSequence()
                .map { it.first }
                .filter { it.taskKey == taskKey }
                .toList())

        return instances
    }

    abstract fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey): Set<TaskHierarchy>

    abstract fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy>

    abstract fun delete()

    abstract fun setName(name: String, note: String?)

    protected fun updateSchedules(newScheduleDatas: List<CreateTaskViewModel.ScheduleData>, now: ExactTimeStamp) {
        val removeSchedules = ArrayList<Schedule>()
        val addScheduleDatas = ArrayList(newScheduleDatas)

        val oldSchedules = getCurrentSchedules(now)
        val scheduleDatas = domainFactory.getScheduleDatas(oldSchedules, now).second
        for ((key, value) in scheduleDatas!!) {
            if (addScheduleDatas.contains(key)) {
                addScheduleDatas.remove(key)
            } else {
                removeSchedules.addAll(value)
            }
        }

        removeSchedules.forEach { it.setEndExactTimeStamp(now) }

        if (!addScheduleDatas.isEmpty())
            addSchedules(addScheduleDatas, now)
    }

    protected abstract fun addSchedules(scheduleDatas: List<CreateTaskViewModel.ScheduleData>, now: ExactTimeStamp)

    abstract fun addChild(childTask: Task, now: ExactTimeStamp)

    abstract fun deleteSchedule(schedule: Schedule)

    private class OldestVisibleException4 internal constructor(message: String) : Exception(message)

    abstract fun belongsToRemoteProject(): Boolean

    abstract fun updateProject(context: Context, now: ExactTimeStamp, projectId: String?): Task
}
