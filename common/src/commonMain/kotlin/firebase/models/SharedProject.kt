package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey

class SharedProject(
        override val projectRecord: SharedProjectRecord
) : Project<ProjectType.Shared>() {

    override val id = projectRecord.projectKey

    private val remoteUsers = projectRecord.remoteUserRecords
            .values
            .map { ProjectUser(this, it) }
            .associateBy { it.id }
            .toMutableMap()

    val users get() = remoteUsers.values

    override val remoteCustomTimes = mutableMapOf<CustomTimeId.Shared, SharedCustomTime>()
    override val remoteTasks: MutableMap<String, Task<ProjectType.Shared>>
    override val taskHierarchyContainer = TaskHierarchyContainer<ProjectType.Shared>()

    override val customTimes get() = remoteCustomTimes.values

    init {
        for (remoteCustomTimeRecord in projectRecord.customTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = SharedCustomTime(this, remoteCustomTimeRecord)

            remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime
        }

        remoteTasks = projectRecord.remoteTaskRecords
                .values
                .map { Task(this, it) }
                .associateBy { it.id }
                .toMutableMap()

        projectRecord.remoteTaskHierarchyRecords
                .values
                .map { TaskHierarchy(this, it) }
                .forEach { taskHierarchyContainer.add(it.id, it) }
    }

    private fun addUser(rootUser: RootUser) {
        val id = rootUser.id

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
            privateCustomTimeId: CustomTimeKey.Private,
            ownerKey: UserKey
    ) = remoteCustomTimes.values.singleOrNull { it.ownerKey == ownerKey && it.privateKey == privateCustomTimeId.customTimeId }

    override fun getRemoteCustomTime(customTimeId: CustomTimeId<*>): SharedCustomTime {
        check(remoteCustomTimes.containsKey(customTimeId as CustomTimeId.Shared))

        return remoteCustomTimes.getValue(customTimeId)
    }

    override fun getRemoteCustomTime(customTimeKey: CustomTimeKey<ProjectType.Shared>): SharedCustomTime = getRemoteCustomTime(customTimeKey.customTimeId)
    override fun getRemoteCustomTime(customTimeId: String) = getRemoteCustomTime(CustomTimeId.Shared(customTimeId))

    private fun newRemoteCustomTime(customTimeJson: SharedCustomTimeJson): SharedCustomTime {
        val remoteCustomTimeRecord = projectRecord.newRemoteCustomTimeRecord(customTimeJson)

        val remoteCustomTime = SharedCustomTime(this, remoteCustomTimeRecord)

        check(!remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

        return remoteCustomTime
    }

    override fun getOrCreateCustomTime(ownerKey: UserKey, customTime: Time.Custom<*>): SharedCustomTime {
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
            is PrivateCustomTime -> getSharedTimeIfPresent(customTime.key, ownerKey)
            is SharedCustomTime -> customTime.takeIf { it.projectId == id }
            else -> throw IllegalArgumentException()
        } ?: copy()
    }
}