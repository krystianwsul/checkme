package com.krystianwsul.common.domain

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.domain.schedules.Schedule
import com.krystianwsul.common.domain.schedules.ScheduleGroup
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.firebase.models.RemoteProject
import com.krystianwsul.common.firebase.models.RemoteTask
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.ScheduleKey
import com.krystianwsul.common.utils.TaskKey

abstract class Task {

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

    abstract fun getImage(deviceDbInfo: DeviceDbInfo): ImageState?

    abstract fun setImage(deviceDbInfo: DeviceDbInfo, imageState: ImageState?)

    fun current(exactTimeStamp: ExactTimeStamp): Boolean {
        val startExactTimeStamp = startExactTimeStamp
        val endExactTimeStamp = getEndExactTimeStamp()

        return startExactTimeStamp <= exactTimeStamp && (endExactTimeStamp == null || endExactTimeStamp > exactTimeStamp)
    }

    fun getParentName(now: ExactTimeStamp) = getParentTask(now)?.name ?: project.name

    abstract fun getScheduleTextMultiline(scheduleTextFactory: RemoteTask.ScheduleTextFactory, exactTimeStamp: ExactTimeStamp): String?

    abstract fun getScheduleText(scheduleTextFactory: RemoteTask.ScheduleTextFactory, exactTimeStamp: ExactTimeStamp, showParent: Boolean = false): String?

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

    protected abstract fun setMyEndExactTimeStamp(now: ExactTimeStamp, endData: EndData?)

    fun setEndData(
            endData: EndData,
            taskUndoData: TaskUndoData? = null,
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

        setMyEndExactTimeStamp(now, endData)
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

        setMyEndExactTimeStamp(now, null)
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

    fun getPastRootInstances(now: ExactTimeStamp): List<Instance> {
        val allInstances = HashMap<InstanceKey, Instance>()

        allInstances.putAll(existingInstances
                .values
                .filter { it.scheduleDateTime.timeStamp.toExactTimeStamp() <= now }
                .associateBy { it.instanceKey })

        allInstances.putAll(getInstances(null, now.plusOne(), now).associateBy { it.instanceKey })

        return allInstances.values
                .toList()
                .filter { it.isRootInstance(now) }
    }

    fun updateOldestVisible(now: ExactTimeStamp) {
        // 24 hack
        val instances = getPastRootInstances(now)

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

        ErrorLogger.instance.logException(OldestVisibleException5(message))

        setOldestVisible(date) // miejmy nadzieję że coś to później zapisze. nota bene: mogą wygenerować się instances dla wcześniej ukończonych czasów
    }

    protected abstract fun setOldestVisible(date: Date)

    fun getInstances(givenStartExactTimeStamp: ExactTimeStamp?, givenEndExactTimeStamp: ExactTimeStamp, now: ExactTimeStamp): List<Instance> {
        val startExactTimeStamp = listOfNotNull(
                givenStartExactTimeStamp,
                startExactTimeStamp,
                getOldestVisible()?.let { ExactTimeStamp(it, HourMilli(0, 0, 0, 0)) } // 24 hack
        ).max()!!

        val endExactTimeStamp = listOfNotNull(
                getEndExactTimeStamp(),
                givenEndExactTimeStamp
        ).min()!!

        val scheduleInstances = if (startExactTimeStamp >= endExactTimeStamp)
            listOf()
        else
            schedules.flatMap { it.getInstances(this, startExactTimeStamp, endExactTimeStamp) }

        val parentInstances = getParentTaskHierarchies().map { it.parentTask }
                .flatMap { it.getInstances(givenStartExactTimeStamp, givenEndExactTimeStamp, now) }
                .flatMap { it.getChildInstances(now) }
                .asSequence()
                .map { it.first }
                .filter { it.taskKey == taskKey }
                .toList()

        return scheduleInstances + parentInstances
    }

    fun hasInstances(now: ExactTimeStamp) = existingInstances.values.isNotEmpty() || getInstances(null, now, now).isNotEmpty()

    abstract fun getParentTaskHierarchies(): Set<TaskHierarchy>

    abstract fun delete()

    abstract fun setName(name: String, note: String?)

    fun updateSchedules(ownerKey: String, scheduleDatas: List<Pair<ScheduleData, Time>>, now: ExactTimeStamp) {
        val removeSchedules = ArrayList<Schedule>()
        val addScheduleDatas = ArrayList(scheduleDatas)

        val oldSchedules = getCurrentSchedules(now)
        val oldScheduleDatas = ScheduleGroup.getGroups(oldSchedules).map { it.scheduleData to it.schedules }
        for ((key, value) in oldScheduleDatas) {
            val existing = addScheduleDatas.singleOrNull { it.first == key }
            if (existing != null) {
                addScheduleDatas.remove(existing)
            } else {
                removeSchedules.addAll(value)
            }
        }

        removeSchedules.forEach { it.setEndExactTimeStamp(now) }

        if (addScheduleDatas.isNotEmpty())
            addSchedules(ownerKey, addScheduleDatas, now)
    }

    protected abstract fun addSchedules(ownerKey: String, scheduleDatas: List<Pair<ScheduleData, Time>>, now: ExactTimeStamp)

    abstract fun addChild(childTask: Task, now: ExactTimeStamp)

    abstract fun deleteSchedule(schedule: Schedule)

    private class OldestVisibleException5(message: String) : Exception(message)

    abstract fun belongsToRemoteProject(): Boolean

    abstract fun updateProject(projectUpdater: RemoteTask.ProjectUpdater, now: ExactTimeStamp, projectId: String): Task

    fun getHierarchyExactTimeStamp(now: ExactTimeStamp) = listOfNotNull(now, getEndExactTimeStamp()?.minusOne()).min()!!

    abstract fun getInstance(scheduleDateTime: DateTime): Instance

    abstract fun getChildTaskHierarchies(): Set<TaskHierarchy>

    fun getChildTaskHierarchies(exactTimeStamp: ExactTimeStamp) = getChildTaskHierarchies().asSequence()
            .filter { it.current(exactTimeStamp) && it.childTask.current(exactTimeStamp) }
            .sortedBy { it.ordinal }
            .toList()

    data class EndData(
            val exactTimeStamp: ExactTimeStamp,
            val deleteInstances: Boolean)
}
