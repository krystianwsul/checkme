package com.krystianwsul.common.domain

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.domain.schedules.Schedule
import com.krystianwsul.common.domain.schedules.ScheduleGroup
import com.krystianwsul.common.domain.schedules.SingleSchedule
import com.krystianwsul.common.domain.schedules.SingleScheduleBridge
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.firebase.models.RemoteTask
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

abstract class Task {

    abstract val startExactTimeStamp: ExactTimeStamp

    abstract val name: String

    abstract val note: String?

    abstract val taskKey: TaskKey

    abstract val schedules: Collection<Schedule>

    abstract fun getEndData(): EndData?

    fun getEndExactTimeStamp() = getEndData()?.exactTimeStamp

    abstract fun getOldestVisible(): Date?

    abstract val existingInstances: Map<ScheduleKey, Instance<*, *>>

    abstract val project: Project<*, *>

    abstract val imageJson: TaskJson.Image?

    abstract fun getImage(deviceDbInfo: DeviceDbInfo): ImageState?

    abstract fun setImage(deviceDbInfo: DeviceDbInfo, imageState: ImageState?)

    fun current(exactTimeStamp: ExactTimeStamp) = startExactTimeStamp <= exactTimeStamp && notDeleted(exactTimeStamp)

    fun getParentName(now: ExactTimeStamp) = getParentTask(now)?.name ?: project.name

    abstract fun getScheduleTextMultiline(scheduleTextFactory: RemoteTask.ScheduleTextFactory, exactTimeStamp: ExactTimeStamp): String?

    abstract fun getScheduleText(scheduleTextFactory: RemoteTask.ScheduleTextFactory, exactTimeStamp: ExactTimeStamp, showParent: Boolean = false): String?

    fun notDeleted(exactTimeStamp: ExactTimeStamp) = getEndExactTimeStamp()?.let { it > exactTimeStamp } != false

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

    private fun getRootTask(
            exactTimeStamp: ExactTimeStamp
    ): Task = getParentTask(exactTimeStamp)?.getRootTask(exactTimeStamp) ?: this

    fun getCurrentSchedules(exactTimeStamp: ExactTimeStamp): List<Schedule> {
        check(current(exactTimeStamp))

        val currentSchedules = schedules.filter { it.current(exactTimeStamp) }

        getSingleSchedule(exactTimeStamp)?.let { singleSchedule ->
            val instance = singleSchedule.getInstance(this)

            if (instance.scheduleDate != instance.instanceDate || instance.scheduleDateTime.time.timePair != instance.instanceTimePair)
                return listOf(SingleSchedule(this as RemoteTask<*, *>, MockSingleScheduleBridge(singleSchedule, instance)))
        }

        return currentSchedules
    }

    private fun getSingleSchedule(exactTimeStamp: ExactTimeStamp): SingleSchedule? {
        return schedules.singleOrNull { it.current(exactTimeStamp) } as? SingleSchedule
    }

    private class MockSingleScheduleBridge(
            private val singleSchedule: SingleSchedule,
            private val instance: Instance
    ) : SingleScheduleBridge by singleSchedule.singleScheduleBridge {

        override val customTimeKey get() = instance.instanceTimePair.customTimeKey

        override val year get() = instance.instanceDate.year

        override val month get() = instance.instanceDate.month

        override val day get() = instance.instanceDate.day

        override val hour
            get() = instance.instanceTime
                    .timePair
                    .hourMinute
                    ?.hour

        override val minute
            get() = instance.instanceTime
                    .timePair
                    .hourMinute
                    ?.minute

        override val remoteCustomTimeKey
            get() = instance.instanceTime
                    .timePair
                    .customTimeKey
                    ?.let { Pair(it.remoteProjectId, it.remoteCustomTimeId) }

        override val timePair
            get() = customTimeKey?.let { TimePair(it) } ?: TimePair(HourMinute(hour!!, minute!!))
    }

    fun isRootTask(exactTimeStamp: ExactTimeStamp): Boolean {
        check(current(exactTimeStamp))

        return getParentTask(exactTimeStamp) == null
    }

    protected abstract fun setMyEndExactTimeStamp(uuid: String, now: ExactTimeStamp, endData: EndData?)

    fun setEndData(
            uuid: String,
            endData: EndData,
            taskUndoData: TaskUndoData? = null,
            recursive: Boolean = false
    ) {
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
            it.childTask.setEndData(uuid, endData, taskUndoData, true)

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

        setMyEndExactTimeStamp(uuid, now, endData)
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

    fun clearEndExactTimeStamp(uuid: String, now: ExactTimeStamp) {
        check(!current(now))

        setMyEndExactTimeStamp(uuid, now, null)
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

    fun getPastRootInstances(now: ExactTimeStamp): List<Instance<*, *>> {
        val allInstances = mutableMapOf<InstanceKey, Instance<*, *>>()

        allInstances.putAll(existingInstances
                .values
                .filter { it.scheduleDateTime.timeStamp.toExactTimeStamp() <= now }
                .associateBy { it.instanceKey })

        allInstances.putAll(getInstances(null, now.plusOne(), now).associateBy { it.instanceKey })

        return allInstances.values
                .toList()
                .filter { it.isRootInstance(now) }
    }

    // there might be an issue here when moving task across projects
    fun updateOldestVisible(uuid: String, now: ExactTimeStamp) {
        // 24 hack
        val optional = getPastRootInstances(now).filter { it.isVisible(now, true) }.minBy { it.scheduleDateTime }

        val oldestVisible = listOfNotNull(optional?.scheduleDate, now.date).min()!!

        setOldestVisible(uuid, oldestVisible)
    }

    fun correctOldestVisible(date: Date) {
        val oldestVisible = getOldestVisible()
        check(oldestVisible != null && date < oldestVisible)

        ErrorLogger.instance.logException(OldestVisibleException6("$name real oldest: $oldestVisible, correct oldest: $date"))
    }

    protected abstract fun setOldestVisible(uuid: String, date: Date)

    fun getInstances(
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenEndExactTimeStamp: ExactTimeStamp,
            now: ExactTimeStamp
    ): List<Instance<*, *>> {
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
            schedules.flatMap { it.getInstances(this, startExactTimeStamp, endExactTimeStamp).toList() }

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

    fun updateSchedules(
            ownerKey: UserKey,
            shownFactory: Instance.ShownFactory,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            now: ExactTimeStamp
    ) {
        val removeSchedules = mutableListOf<Schedule>()
        val addScheduleDatas = scheduleDatas.toMutableList()

        val oldSchedules = getCurrentSchedules(now)
        val oldScheduleDatas = ScheduleGroup.getGroups(oldSchedules).map { it.scheduleData to it.schedules }
        for ((key, value) in oldScheduleDatas) {
            val existing = addScheduleDatas.singleOrNull { it.first == key }
            if (existing != null)
                addScheduleDatas.remove(existing)
            else
                removeSchedules.addAll(value)
        }

        val singleRemoveSchedule = removeSchedules.singleOrNull() as? SingleSchedule
        val singleAddSchedulePair = addScheduleDatas.singleOrNull()?.takeIf { it.first is ScheduleData.Single }
        val oldSingleSchedule = getSingleSchedule(now)

        if (singleRemoveSchedule != null &&
                singleAddSchedulePair != null &&
                singleRemoveSchedule.scheduleId == oldSingleSchedule?.scheduleId
        ) {
            oldSingleSchedule.getInstance(this).setInstanceDateTime(
                    shownFactory,
                    ownerKey,
                    singleAddSchedulePair.run { DateTime((first as ScheduleData.Single).date, second) },
                    now
            )
        } else {
            removeSchedules.forEach { it.setEndExactTimeStamp(now) }
            addSchedules(ownerKey, addScheduleDatas, now)
        }
    }

    protected abstract fun addSchedules(
            ownerKey: UserKey,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            now: ExactTimeStamp
    )

    abstract fun addChild(childTask: Task, now: ExactTimeStamp)

    abstract fun deleteSchedule(schedule: Schedule)

    private class OldestVisibleException6(message: String) : Exception(message)

    abstract fun belongsToRemoteProject(): Boolean

    abstract fun updateProject(
            projectUpdater: RemoteTask.ProjectUpdater,
            now: ExactTimeStamp,
            projectId: ProjectKey
    ): Task

    fun getHierarchyExactTimeStamp(now: ExactTimeStamp) = listOfNotNull(now, getEndExactTimeStamp()?.minusOne()).min()!!

    abstract fun getInstance(scheduleDateTime: DateTime): Instance<*, *>

    abstract fun getChildTaskHierarchies(): Set<TaskHierarchy>

    fun getChildTaskHierarchies(exactTimeStamp: ExactTimeStamp) = getChildTaskHierarchies().filter {
        it.current(exactTimeStamp) && it.childTask.current(exactTimeStamp)
    }.sortedBy { it.ordinal }

    data class EndData(
            val exactTimeStamp: ExactTimeStamp,
            val deleteInstances: Boolean
    )
}
