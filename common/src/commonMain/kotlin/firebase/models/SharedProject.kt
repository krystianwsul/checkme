package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.customtimes.SharedCustomTimeJson
import com.krystianwsul.common.firebase.json.tasks.SharedTaskJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.project.SharedProjectRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey

class SharedProject(
        override val projectRecord: SharedProjectRecord,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
) : Project<ProjectType.Shared>(
        CopyScheduleHelper.Shared,
        AssignedToHelper.Shared,
        userCustomTimeProvider,
) {

    override val projectKey = projectRecord.projectKey

    private val remoteUsers = projectRecord.userRecords
            .values
            .map { ProjectUser(this, it) }
            .associateBy { it.id }
            .toMutableMap()

    val users get() = remoteUsers.values

    override val remoteCustomTimes = mutableMapOf<CustomTimeId.Project.Shared, SharedCustomTime>()
    override val _tasks: MutableMap<String, Task>
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
                .map { Task(this, it) }
                .associateBy { it.id }
                .toMutableMap()

        projectRecord.taskHierarchyRecords
                .values
                .map { ProjectTaskHierarchy(this, it) }
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

    override fun deleteCustomTime(remoteCustomTime: Time.Custom.Project<ProjectType.Shared>) {
        check(remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes.remove(remoteCustomTime.id)
    }

    fun getSharedTimeIfPresent(
            privateCustomTimeId: CustomTimeKey.Project.Private,
            ownerKey: UserKey,
    ) = remoteCustomTimes.values.singleOrNull { it.ownerKey == ownerKey && it.privateKey == privateCustomTimeId.customTimeId }

    override fun getProjectCustomTime(projectCustomTimeId: CustomTimeId.Project): SharedCustomTime {
        check(remoteCustomTimes.containsKey(projectCustomTimeId as CustomTimeId.Project.Shared))

        return remoteCustomTimes.getValue(projectCustomTimeId)
    }

    override fun getProjectCustomTime(projectCustomTimeKey: CustomTimeKey.Project<ProjectType.Shared>): SharedCustomTime =
            getProjectCustomTime(projectCustomTimeKey.customTimeId)

    private fun newRemoteCustomTime(customTimeJson: SharedCustomTimeJson): SharedCustomTime {
        val remoteCustomTimeRecord = projectRecord.newRemoteCustomTimeRecord(customTimeJson)

        val remoteCustomTime = SharedCustomTime(this, remoteCustomTimeRecord)

        check(!remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

        return remoteCustomTime
    }

    override fun getOrCreateCustomTimeOld(ownerKey: UserKey, customTime: Time.Custom.Project<*>): SharedCustomTime {
        fun copy(): SharedCustomTime {
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
                    private?.id?.value,
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
            parentTask: Task,
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            image: TaskJson.Image?,
            ordinal: Double?,
    ): Task {
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
            oldTask: Task,
            now: ExactTimeStamp.Local,
            instanceJsons: MutableMap<String, InstanceJson>,
    ) = projectRecord.newTaskRecord(SharedTaskJson(
            oldTask.name,
            now.long,
            now.offset,
            oldTask.endExactTimeStamp?.long,
            oldTask.note,
            instanceJsons,
            ordinal = oldTask.ordinal,
    ))

    private fun newTask(taskJson: SharedTaskJson): Task {
        val taskRecord = projectRecord.newTaskRecord(taskJson)

        val task = Task(this, taskRecord)
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