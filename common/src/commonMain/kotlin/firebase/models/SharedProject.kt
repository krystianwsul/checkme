package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
import com.krystianwsul.common.firebase.json.SharedTaskJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*

class SharedProject(
        override val projectRecord: SharedProjectRecord,
        private val rootInstanceManagers: Map<TaskKey, RootInstanceManager<ProjectType.Shared>>,
        newRootInstanceManager: (TaskRecord<ProjectType.Shared>) -> RootInstanceManager<ProjectType.Shared>,
) : Project<ProjectType.Shared>(CopyScheduleHelper.Shared, AssignedToHelper.Shared, newRootInstanceManager) {

    override val projectKey = projectRecord.projectKey

    private val remoteUsers = projectRecord.userRecords
            .values
            .map { ProjectUser(this, it) }
            .associateBy { it.id }
            .toMutableMap()

    val users get() = remoteUsers.values

    override val remoteCustomTimes = mutableMapOf<CustomTimeId.Project.Shared, SharedCustomTime>()
    override val _tasks: MutableMap<String, Task<ProjectType.Shared>>
    override val taskHierarchyContainer = TaskHierarchyContainer<ProjectType.Shared>()

    override val customTimes get() = remoteCustomTimes.values

    init {
        for (remoteCustomTimeRecord in projectRecord.customTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = SharedCustomTime(this, remoteCustomTimeRecord)

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

    private fun addUser(rootUser: RootUser) {
        val id = rootUser.userKey

        check(!remoteUsers.containsKey(id))

        val remoteProjectUserRecord = projectRecord.newRemoteUserRecord(rootUser.userJson)
        val remoteProjectUser = ProjectUser(this, remoteProjectUserRecord)

        remoteUsers[id] = remoteProjectUser
    }

    fun deleteUser(projectUser: ProjectUser) {
        val id = projectUser.id
        check(remoteUsers.containsKey(id))

        remoteUsers.remove(id)
    }

    fun updateDeviceDbInfo(deviceDbInfo: DeviceDbInfo) {
        check(remoteUsers.containsKey(deviceDbInfo.key))

        remoteUsers.getValue(deviceDbInfo.key).apply {
            name = deviceDbInfo.name
            setToken(deviceDbInfo)
        }
    }

    fun updatePhotoUrl(deviceInfo: DeviceInfo, photoUrl: String) {
        val key = deviceInfo.key
        check(remoteUsers.containsKey(key))

        val remoteProjectUser = remoteUsers.getValue(key)

        remoteProjectUser.photoUrl = photoUrl
    }

    fun updateUsers(addedFriends: Set<RootUser>, removedFriends: Set<UserKey>) {
        for (addedFriend in addedFriends)
            addUser(addedFriend)

        for (removedFriend in removedFriends) {
            check(remoteUsers.containsKey(removedFriend))

            remoteUsers[removedFriend]!!.delete()
        }
    }

    fun deleteCustomTime(remoteCustomTime: SharedCustomTime) {
        check(remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes.remove(remoteCustomTime.id)
    }

    fun getSharedTimeIfPresent(
            privateCustomTimeId: CustomTimeKey.Project.Private,
            ownerKey: UserKey,
    ) = remoteCustomTimes.values.singleOrNull { it.ownerKey == ownerKey && it.privateKey == privateCustomTimeId.customTimeId }

    override fun getCustomTime(customTimeId: CustomTimeId.Project<*>): SharedCustomTime {
        check(remoteCustomTimes.containsKey(customTimeId as CustomTimeId.Project.Shared))

        return remoteCustomTimes.getValue(customTimeId)
    }

    override fun getCustomTime(customTimeKey: CustomTimeKey.Project<ProjectType.Shared>): SharedCustomTime =
            getCustomTime(customTimeKey.customTimeId)

    override fun getCustomTime(customTimeId: String) = getCustomTime(CustomTimeId.Project.Shared(customTimeId))

    private fun newRemoteCustomTime(customTimeJson: SharedCustomTimeJson): SharedCustomTime {
        val remoteCustomTimeRecord = projectRecord.newRemoteCustomTimeRecord(customTimeJson)

        val remoteCustomTime = SharedCustomTime(this, remoteCustomTimeRecord)

        check(!remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

        return remoteCustomTime
    }

    override fun getOrCreateCustomTime(
            ownerKey: UserKey,
            customTime: Time.Custom<*>,
            allowCopy: Boolean,
    ): SharedCustomTime {
        fun copy(): SharedCustomTime {
            if (!allowCopy) throw UnsupportedOperationException()

            val private = customTime as? PrivateCustomTime

            val customTimeJson = SharedCustomTimeJson(
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
                    customTime.getHourMinute(DayOfWeek.SATURDAY).minute,
                    private?.projectId?.key,
                    private?.id?.value
            )

            return newRemoteCustomTime(customTimeJson)
        }

        return when (customTime) {
            is PrivateCustomTime -> getSharedTimeIfPresent(customTime.key, ownerKey)
            is SharedCustomTime -> customTime.takeIf { it.projectId == projectKey }
            else -> throw IllegalArgumentException()
        } ?: copy()
    }

    override fun createChildTask(
            parentTask: Task<ProjectType.Shared>,
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            image: TaskJson.Image?,
            ordinal: Double?,
    ): Task<ProjectType.Shared> {
        val taskJson = SharedTaskJson(
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
    ) = projectRecord.newTaskRecord(SharedTaskJson(
            oldTask.name,
            now.long,
            now.offset,
            oldTask.endExactTimeStamp?.long,
            oldTask.note,
            instanceJsons,
            ordinal = oldTask.ordinal
    ))

    private fun newTask(taskJson: SharedTaskJson): Task<ProjectType.Shared> {
        val taskRecord = projectRecord.newTaskRecord(taskJson)

        val task = Task(
                this,
                taskRecord,
                rootInstanceManagers[taskRecord.taskKey] ?: newRootInstanceManager(taskRecord),
        )

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
    ) = newTask(SharedTaskJson(
            name,
            now.long,
            now.offset,
            note = note,
            image = image,
            ordinal = ordinal,
    ))

    override fun getAssignedTo(userKeys: Set<UserKey>) = remoteUsers.filterKeys { it in userKeys }
}