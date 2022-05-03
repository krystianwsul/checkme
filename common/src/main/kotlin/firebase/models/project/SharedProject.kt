package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.customtime.SharedCustomTime
import com.krystianwsul.common.firebase.models.task.ProjectTask
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.models.users.ProjectUser
import com.krystianwsul.common.firebase.models.users.RootUser
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.project.SharedProjectRecord
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
    rootModelChangeManager: RootModelChangeManager,
) : Project<ProjectType.Shared>(
    AssignedToHelper.Shared,
    userCustomTimeProvider,
    rootTaskProvider,
    rootModelChangeManager,
) {

    override val projectKey = projectRecord.projectKey

    private val remoteUsers = projectRecord.userRecords
        .mapValues { ProjectUser(this, it.value) }
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

    override fun getProjectCustomTime(projectCustomTimeId: CustomTimeId.Project): SharedCustomTime {
        check(remoteCustomTimes.containsKey(projectCustomTimeId as CustomTimeId.Project.Shared))

        return remoteCustomTimes.getValue(projectCustomTimeId)
    }

    override fun getProjectCustomTime(projectCustomTimeKey: CustomTimeKey.Project<ProjectType.Shared>): SharedCustomTime =
        getProjectCustomTime(projectCustomTimeKey.customTimeId)

    override fun getAssignedTo(userKeys: Set<UserKey>) = remoteUsers.filterKeys { it in userKeys }
}