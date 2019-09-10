package com.krystianwsul.checkme.domainmodel

import android.util.Log
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.schedules.Schedule
import com.krystianwsul.checkme.domainmodel.schedules.ScheduleGroup
import com.krystianwsul.checkme.firebase.models.ImageState
import com.krystianwsul.checkme.firebase.models.RemoteProject
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMilli
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ScheduleKey
import com.krystianwsul.common.utils.TaskKey
import java.util.*

abstract class Task(protected val domainFactory: DomainFactory) {

    abstract val startExactTimeStamp: ExactTimeStamp

    abstract val name: String

    abstract val note: String?

    abstract val taskKey: TaskKey

    abstract val schedules: Collection<Schedule>

    abstract fun getEndData(): EndData?

    fun getEndExactTimeStamp() = getEndData()?.exactTimeStamp

    abstract fun getOldestVisible(): Date?

    abstract val existingInstances: Map<ScheduleKey, Instance>

    abstract val project: RemoteProject<*>

    abstract val imageJson: TaskJson.Image?

    abstract var image: ImageState?

    fun current(exactTimeStamp: ExactTimeStamp): Boolean {
        val startExactTimeStamp = startExactTimeStamp
        val endExactTimeStamp = getEndExactTimeStamp()

        return startExactTimeStamp <= exactTimeStamp && (endExactTimeStamp == null || endExactTimeStamp > exactTimeStamp)
    }

    fun getParentName(now: ExactTimeStamp) = getParentTask(now)?.name ?: project.name

    abstract fun getScheduleTextMultiline(exactTimeStamp: ExactTimeStamp): String?

    abstract fun getScheduleText(exactTimeStamp: ExactTimeStamp, showParent: Boolean = false): String?

    fun notDeleted(exactTimeStamp: ExactTimeStamp): Boolean {
        val endExactTimeStamp = getEndExactTimeStamp()

        return endExactTimeStamp == null || endExactTimeStamp > exactTimeStamp
    }

    fun isVisible(now: ExactTimeStamp, hack24: Boolean): Boolean {
        if (!current(now))
            return false

        val rootTask = getRootTask(now)
        val schedules = rootTask.getCurrentSchedules(now)

        if (schedules.isEmpty())
            return true

        if (schedules.any { it.isVisible(this, now, hack24) })
            return true

        return false
    }// bo inheritance i testy

    private fun getRootTask(exactTimeStamp: ExactTimeStamp): Task = getParentTask(exactTimeStamp)?.getRootTask(exactTimeStamp)
            ?: this

    fun getCurrentSchedules(exactTimeStamp: ExactTimeStamp): List<Schedule> {
        check(current(exactTimeStamp))

        return schedules.filter { it.current(exactTimeStamp) }
    }

    fun isRootTask(exactTimeStamp: ExactTimeStamp): Boolean {
        check(current(exactTimeStamp))

        return getParentTask(exactTimeStamp) == null
    }

    protected abstract fun setMyEndExactTimeStamp(endData: EndData?)

    fun setEndData(
            endData: EndData,
            taskUndoData: DomainFactory.TaskUndoData? = null,
            recursive: Boolean = false) {
        val now = endData.exactTimeStamp

        check(current(now))

        taskUndoData?.taskKeys?.add(taskKey)

        val schedules = getCurrentSchedules(now)
        if (isRootTask(now)) {
            check(schedules.all { it.current(now) })

            schedules.forEach {
                taskUndoData?.scheduleIds?.add(it.scheduleId)

                it.setEndExactTimeStamp(now)
            }
        } else {
            check(schedules.isEmpty())
        }

        getChildTaskHierarchies(now).forEach {
            it.childTask.setEndData(endData, taskUndoData, true)

            taskUndoData?.taskHierarchyKeys?.add(it.taskHierarchyKey)

            it.setEndExactTimeStamp(now)
        }

        if (!recursive) {
            getParentTaskHierarchy(now)?.let {
                check(it.current(now))

                taskUndoData?.taskHierarchyKeys?.add(it.taskHierarchyKey)

                it.setEndExactTimeStamp(now)
            }
        }

        setMyEndExactTimeStamp(endData)
    }

    fun getParentTaskHierarchy(exactTimeStamp: ExactTimeStamp): TaskHierarchy? {
        val taskHierarchies = if (current(exactTimeStamp)) {
            check(notDeleted(exactTimeStamp))

            getParentTaskHierarchies().filter { it.current(exactTimeStamp) }
        } else {
            // jeśli child task jeszcze nie istnieje, ale będzie utworzony jako child, zwróć ów przyszły hierarchy
            // żeby można było dodawać child instances do past parent instance

            check(notDeleted(exactTimeStamp))

            getParentTaskHierarchies().filter { it.startExactTimeStamp == startExactTimeStamp }
        }

        return if (taskHierarchies.isEmpty()) {
            null
        } else {
            taskHierarchies.single()
        }
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        check(!current(now))

        setMyEndExactTimeStamp(null)
    }

    abstract fun createChildTask(now: ExactTimeStamp, name: String, note: String?, image: TaskJson.Image?): Task

    fun getParentTask(exactTimeStamp: ExactTimeStamp): Task? {
        check(notDeleted(exactTimeStamp))

        return getParentTaskHierarchy(exactTimeStamp)?.let {
            check(it.notDeleted(exactTimeStamp))

            it.parentTask.also {
                check(it.notDeleted(exactTimeStamp))
            }
        }
    }

    fun getPastInstances(now: ExactTimeStamp): List<Instance> {
        val allInstances = HashMap<InstanceKey, Instance>()

        allInstances.putAll(existingInstances
                .values
                .filter { it.scheduleDateTime.timeStamp.toExactTimeStamp() <= now }
                .associateBy { it.instanceKey })

        allInstances.putAll(getInstances(null, now.plusOne(), now).associateBy { it.instanceKey })

        return allInstances.values.toList()
    }

    fun updateOldestVisible(now: ExactTimeStamp) {
        // 24 hack
        val instances = getPastInstances(now)

        val optional = instances.asSequence()
                .filter { it.isVisible(now, true) }
                .minBy { it.scheduleDateTime }

        var oldestVisible: Date

        if (optional != null) {
            oldestVisible = optional.scheduleDate

            if (oldestVisible > now.date)
                oldestVisible = now.date
        } else {
            oldestVisible = now.date
        }

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

        val taskHierarchies = getParentTaskHierarchies()

        instances.addAll(taskHierarchies.map { it.parentTask }
                .flatMap { it.getInstances(startExactTimeStamp, endExactTimeStamp, now) }
                .flatMap { it.getChildInstances(now) }
                .asSequence()
                .map { it.first }
                .filter { it.taskKey == taskKey }
                .toList())

        return instances
    }

    fun hasInstances(now: ExactTimeStamp) = existingInstances.values.isNotEmpty() || getInstances(null, now, now).isNotEmpty()

    abstract fun getParentTaskHierarchies(): Set<TaskHierarchy>

    abstract fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy>

    abstract fun delete()

    abstract fun setName(name: String, note: String?)

    fun updateSchedules(newScheduleDatas: List<CreateTaskViewModel.ScheduleData>, now: ExactTimeStamp) {
        val removeSchedules = ArrayList<Schedule>()
        val addScheduleDatas = ArrayList(newScheduleDatas)

        val oldSchedules = getCurrentSchedules(now)
        val scheduleDatas = ScheduleGroup.getGroups(oldSchedules).map { it.scheduleData to it.schedules }
        for ((key, value) in scheduleDatas) {
            if (addScheduleDatas.contains(key)) {
                addScheduleDatas.remove(key)
            } else {
                removeSchedules.addAll(value)
            }
        }

        removeSchedules.forEach { it.setEndExactTimeStamp(now) }

        if (addScheduleDatas.isNotEmpty())
            addSchedules(addScheduleDatas, now)
    }

    protected abstract fun addSchedules(scheduleDatas: List<CreateTaskViewModel.ScheduleData>, now: ExactTimeStamp)

    abstract fun addChild(childTask: Task, now: ExactTimeStamp)

    abstract fun deleteSchedule(schedule: Schedule)

    private class OldestVisibleException4(message: String) : Exception(message)

    abstract fun belongsToRemoteProject(): Boolean

    abstract fun updateProject(now: ExactTimeStamp, projectId: String): Task

    fun getHierarchyExactTimeStamp(now: ExactTimeStamp) = listOfNotNull(now, getEndExactTimeStamp()?.minusOne()).min()!!

    abstract fun getInstance(scheduleDateTime: DateTime): Instance

    fun getChildTaskHierarchies(exactTimeStamp: ExactTimeStamp) = getTaskHierarchiesByParentTaskKey(taskKey).asSequence()
            .filter { it.current(exactTimeStamp) && it.childTask.current(exactTimeStamp) }
            .sortedBy { it.ordinal }
            .toList()

    data class EndData(
            val exactTimeStamp: ExactTimeStamp,
            val deleteInstances: Boolean)
}
