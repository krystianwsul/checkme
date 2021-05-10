package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.tasks.SharedTaskJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.CopyScheduleHelper
import com.krystianwsul.common.firebase.models.ProjectUser
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.models.customtime.SharedCustomTime
import com.krystianwsul.common.firebase.models.task.ProjectTask
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.project.SharedProjectRecord
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
    rootTaskProvider: RootTaskProvider,
) : Project<ProjectType.Shared>(
    CopyScheduleHelper.Shared,
    AssignedToHelper.Shared,
    userCustomTimeProvider,
    rootTaskProvider,
) {

    override val projectKey = projectRecord.projectKey

    private val remoteUsers = projectRecord.userRecords
        .values
        .map { ProjectUser(this, it) }
        .associateBy { it.id }
        .toMutableMap()

    val users get() = remoteUsers.values

    override val remoteCustomTimes = mutableMapOf<CustomTimeId.Project.Shared, SharedCustomTime>()
    override val _tasks: MutableMap<String, ProjectTask>
    override val taskHierarchyContainer = TaskHierarchyContainer()

    override val customTimes get() = remoteCustomTimes.values

    init {
        for (remoteCustomTimeRecord in projectRecord.customTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = SharedCustomTime(this, remoteCustomTimeRecord)

            remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime
        }

        _tasks = projectRecord.taskRecords
            .values
            .map { ProjectTask(this, it) }
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
    ) =
        remoteCustomTimes.values.singleOrNull { it.ownerKey == ownerKey && it.privateKey == privateCustomTimeId.customTimeId }

    override fun getProjectCustomTime(projectCustomTimeId: CustomTimeId.Project): SharedCustomTime {
        check(remoteCustomTimes.containsKey(projectCustomTimeId as CustomTimeId.Project.Shared))

        return remoteCustomTimes.getValue(projectCustomTimeId)
    }

    override fun getProjectCustomTime(projectCustomTimeKey: CustomTimeKey.Project<ProjectType.Shared>): SharedCustomTime =
        getProjectCustomTime(projectCustomTimeKey.customTimeId)

    override fun copyTaskRecord(
        oldTask: ProjectTask,
        now: ExactTimeStamp.Local,
        instanceJsons: MutableMap<String, InstanceJson>,
    ) = projectRecord.newTaskRecord(
        SharedTaskJson(
            oldTask.name,
            now.long,
            now.offset,
            oldTask.endExactTimeStamp?.long,
            oldTask.note,
            instanceJsons,
            ordinal = oldTask.ordinal,
        )
    )

    private fun newTask(taskJson: SharedTaskJson): ProjectTask {
        val taskRecord = projectRecord.newTaskRecord(taskJson)

        val task = ProjectTask(this, taskRecord)
        check(!_tasks.containsKey(task.id))

        _tasks[task.id] = task

        return task
    }

    override fun createTask(
        // todo task edit
        now: ExactTimeStamp.Local,
        image: TaskJson.Image?,
        name: String,
        note: String?,
        ordinal: Double?,
    ) = newTask(
        SharedTaskJson(
            name,
            now.long,
            now.offset,
            note = note,
            image = image,
            ordinal = ordinal,
        )
    )

    override fun getAssignedTo(userKeys: Set<UserKey>) = remoteUsers.filterKeys { it in userKeys }
}