package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
import com.krystianwsul.common.firebase.records.RemoteSharedProjectRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.UserKey

class SharedProject(
        override val remoteProjectRecord: RemoteSharedProjectRecord
) : Project<RemoteCustomTimeId.Shared, ProjectKey.Shared>() {

    private val remoteUsers = remoteProjectRecord.remoteUserRecords
            .values
            .map { ProjectUser(this, it) }
            .associateBy { it.id }
            .toMutableMap()

    val users get() = remoteUsers.values

    override val remoteCustomTimes = HashMap<RemoteCustomTimeId.Shared, SharedCustomTime>()
    override val remoteTasks: MutableMap<String, Task<RemoteCustomTimeId.Shared, ProjectKey.Shared>>
    override val taskHierarchyContainer = TaskHierarchyContainer<RemoteCustomTimeId.Shared, ProjectKey.Shared>()

    override val customTimes get() = remoteCustomTimes.values

    init {
        for (remoteCustomTimeRecord in remoteProjectRecord.remoteCustomTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = SharedCustomTime(this, remoteCustomTimeRecord)

            remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime
        }

        remoteTasks = remoteProjectRecord.remoteTaskRecords
                .values
                .map { Task(this, it) }
                .associateBy { it.id }
                .toMutableMap()

        remoteProjectRecord.remoteTaskHierarchyRecords
                .values
                .map { TaskHierarchy(this, it) }
                .forEach { taskHierarchyContainer.add(it.id, it) }
    }

    private fun addUser(rootUser: RootUser) {
        val id = rootUser.id

        check(!remoteUsers.containsKey(id))

        val remoteProjectUserRecord = remoteProjectRecord.newRemoteUserRecord(rootUser.userJson)
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

        val remoteProjectUser = remoteUsers[deviceDbInfo.key]!!

        remoteProjectUser.setToken(deviceDbInfo)
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
            privateCustomTimeId: RemoteCustomTimeId.Private,
            ownerKey: UserKey
    ) = remoteCustomTimes.values.singleOrNull { it.ownerKey == ownerKey && it.privateKey == privateCustomTimeId }

    override fun getRemoteCustomTime(remoteCustomTimeId: RemoteCustomTimeId): SharedCustomTime {
        check(remoteCustomTimes.containsKey(remoteCustomTimeId as RemoteCustomTimeId.Shared))

        return remoteCustomTimes.getValue(remoteCustomTimeId)
    }

    override fun getRemoteCustomTimeId(id: String) = RemoteCustomTimeId.Shared(id)

    private fun newRemoteCustomTime(customTimeJson: SharedCustomTimeJson): SharedCustomTime {
        val remoteCustomTimeRecord = remoteProjectRecord.newRemoteCustomTimeRecord(customTimeJson)

        val remoteCustomTime = SharedCustomTime(this, remoteCustomTimeRecord)

        check(!remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

        return remoteCustomTime
    }

    override fun getOrCreateCustomTime(ownerKey: UserKey, customTime: CustomTime<*, *>): SharedCustomTime {
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
                    private?.id?.value
            )

            return newRemoteCustomTime(customTimeJson)
        }

        return when (customTime) {
            is PrivateCustomTime -> getSharedTimeIfPresent(customTime.id, ownerKey)
            is SharedCustomTime -> customTime.takeIf { it.projectId == id }
            else -> throw IllegalArgumentException()
        } ?: copy()
    }
}