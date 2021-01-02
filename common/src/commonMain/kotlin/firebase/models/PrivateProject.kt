package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.firebase.json.PrivateTaskJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*

class PrivateProject(
        override val projectRecord: PrivateProjectRecord,
        rootInstanceManagers: Map<TaskKey, RootInstanceManager<ProjectType.Private>>,
        newRootInstanceManager: (TaskRecord<ProjectType.Private>) -> RootInstanceManager<ProjectType.Private>,
) : Project<ProjectType.Private>(CopyScheduleHelper.Private, AssignedToHelper.Private, newRootInstanceManager) {

    override val projectKey = projectRecord.projectKey

    override val remoteCustomTimes = HashMap<CustomTimeId.Private, PrivateCustomTime>()
    override val _tasks: MutableMap<String, Task<ProjectType.Private>>
    override val taskHierarchyContainer = TaskHierarchyContainer<ProjectType.Private>()

    override val customTimes get() = remoteCustomTimes.values

    init {
        for (remoteCustomTimeRecord in projectRecord.customTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = PrivateCustomTime(this, remoteCustomTimeRecord)

            remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime
        }

        _tasks = projectRecord.taskRecords
                .values
                .map {
                    val rootInstanceManager = rootInstanceManagers[it.taskKey] ?: newRootInstanceManager(it)

                    Task(this, it, rootInstanceManager)
                }
                .associateBy { it.id }
                .toMutableMap()

        projectRecord.taskHierarchyRecords
                .values
                .map { TaskHierarchy(this, it) }
                .forEach { taskHierarchyContainer.add(it.id, it) }

        initializeInstanceHierarchyContainers()
    }

    fun newRemoteCustomTime(customTimeJson: PrivateCustomTimeJson): PrivateCustomTime {
        val remoteCustomTimeRecord = projectRecord.newRemoteCustomTimeRecord(customTimeJson)

        val remoteCustomTime = PrivateCustomTime(this, remoteCustomTimeRecord)

        check(!remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

        return remoteCustomTime
    }

    fun deleteCustomTime(remoteCustomTime: PrivateCustomTime) {
        check(remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes.remove(remoteCustomTime.id)
    }

    override fun getCustomTime(customTimeId: CustomTimeId<*>): PrivateCustomTime {
        check(remoteCustomTimes.containsKey(customTimeId as CustomTimeId.Private))

        return remoteCustomTimes.getValue(customTimeId)
    }

    override fun getCustomTime(customTimeKey: CustomTimeKey<ProjectType.Private>): PrivateCustomTime = getCustomTime(customTimeKey.customTimeId)
    override fun getCustomTime(customTimeId: String) = getCustomTime(CustomTimeId.Private(customTimeId))

    override fun getOrCreateCustomTime(ownerKey: UserKey, customTime: Time.Custom<*>) = when (customTime) {
        is PrivateCustomTime -> customTime
        is SharedCustomTime -> {
            if (customTime.ownerKey?.toPrivateProjectKey() == projectKey) {
                customTimes.single { it.id == customTime.privateKey }
            } else {
                val customTimeJson = PrivateCustomTimeJson(
                        customTime.name,
                        customTime.getHourMinute(DayOfWeek.SUNDAY).hour,
                        customTime.getHourMinute(DayOfWeek.SUNDAY).minute,
                        customTime.getHourMinute(DayOfWeek.MONDAY).hour,
                        customTime.getHourMinute(DayOfWeek.MONDAY).minute,
                        customTime.getHourMinute(DayOfWeek.TUESDAY).hour,
                        customTime.getHourMinute(DayOfWeek.TUESDAY).minute,
                        customTime.getHourMinute(DayOfWeek.WEDNESDAY).hour,
                        customTime.getHourMinute(DayOfWeek.WEDNESDAY).minute,
                        customTime.getHourMinute(DayOfWeek.THURSDAY).hour,
                        customTime.getHourMinute(DayOfWeek.THURSDAY).minute,
                        customTime.getHourMinute(DayOfWeek.FRIDAY).hour,
                        customTime.getHourMinute(DayOfWeek.FRIDAY).minute,
                        customTime.getHourMinute(DayOfWeek.SATURDAY).hour,
                        customTime.getHourMinute(DayOfWeek.SATURDAY).minute
                )

                newRemoteCustomTime(customTimeJson)
            }
        }
        else -> throw IllegalArgumentException()
    }

    override fun createChildTask(
            parentTask: Task<ProjectType.Private>,
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            image: TaskJson.Image?,
            ordinal: Double?,
    ): Task<ProjectType.Private> {
        val taskJson = PrivateTaskJson(
                name,
                now.long,
                now.offset,
                null,
                note,
                image = image,
                ordinal = ordinal
        )

        val childTask = newTask(taskJson)

        createTaskHierarchy(parentTask, childTask, now)

        return childTask
    }

    override fun copyTaskRecord(
            oldTask: Task<*>,
            now: ExactTimeStamp.Local,
            instanceJsons: MutableMap<String, InstanceJson>,
    ) = projectRecord.newTaskRecord(PrivateTaskJson(
            oldTask.name,
            now.long,
            now.offset,
            oldTask.endExactTimeStamp?.long,
            oldTask.note,
            instanceJsons,
            ordinal = oldTask.ordinal
    ))

    fun newTask(taskJson: PrivateTaskJson): Task<ProjectType.Private> {
        val taskRecord = projectRecord.newTaskRecord(taskJson)

        val task = Task(this, taskRecord, newRootInstanceManager(taskRecord))
        check(!_tasks.containsKey(task.id))

        _tasks[task.id] = task

        return task
    }

    override fun createTask(
            now: ExactTimeStamp.Local,
            image: TaskJson.Image?,
            name: String,
            note: String?,
            ordinal: Double?,
    ) = newTask(PrivateTaskJson(
            name,
            now.long,
            now.offset,
            note = note,
            image = image,
            ordinal = ordinal
    ))

    override fun getAssignedTo(userKeys: Set<UserKey>) = mapOf<UserKey, ProjectUser>()
}