package com.krystianwsul.checkme.firebase.models

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.TaskHierarchyContainer
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
import com.krystianwsul.common.firebase.records.RemoteSharedProjectRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.RemoteCustomTimeId
import java.util.*

class RemoteSharedProject(
        domainFactory: DomainFactory,
        override val remoteProjectRecord: RemoteSharedProjectRecord,
        deviceInfo: DeviceInfo,
        uuid: String,
        now: ExactTimeStamp) : RemoteProject<RemoteCustomTimeId.Shared>(domainFactory.remoteProjectFactory, domainFactory, uuid) {

    private val remoteUsers = remoteProjectRecord.remoteUserRecords
            .values
            .map { RemoteProjectUser(this, it) }
            .associateBy { it.id }
            .toMutableMap()

    val users get() = remoteUsers.values

    override val remoteCustomTimes = HashMap<RemoteCustomTimeId.Shared, RemoteSharedCustomTime>()
    override val remoteTasks: MutableMap<String, RemoteTask<RemoteCustomTimeId.Shared>>
    override val remoteTaskHierarchyContainer = TaskHierarchyContainer<String, RemoteTaskHierarchy<RemoteCustomTimeId.Shared>>()

    override val customTimes get() = remoteCustomTimes.values

    init {
        for (remoteCustomTimeRecord in remoteProjectRecord.remoteCustomTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = RemoteSharedCustomTime(this, remoteCustomTimeRecord)

            remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime
        }

        remoteTasks = remoteProjectRecord.remoteTaskRecords
                .values
                .map { RemoteTask(domainFactory, this, it, now) }
                .associateBy { it.id }
                .toMutableMap()

        remoteProjectRecord.remoteTaskHierarchyRecords
                .values
                .map { RemoteTaskHierarchy(domainFactory, this, it) }
                .forEach { remoteTaskHierarchyContainer.add(it.id, it) }

        updateUserInfo(deviceInfo.key, uuid, deviceInfo.token)
    }

    private fun addUser(remoteRootUser: RemoteRootUser) {
        val id = remoteRootUser.id

        check(!remoteUsers.containsKey(id))

        val remoteProjectUserRecord = remoteProjectRecord.newRemoteUserRecord(remoteRootUser.userJson)
        val remoteProjectUser = RemoteProjectUser(this, remoteProjectUserRecord)

        remoteUsers[id] = remoteProjectUser
    }

    fun deleteUser(remoteProjectUser: RemoteProjectUser) {
        val id = remoteProjectUser.id
        check(remoteUsers.containsKey(id))

        remoteUsers.remove(id)
    }

    fun updateUserInfo(key: String, uuid: String, token: String?) {
        check(remoteUsers.containsKey(key))

        val remoteProjectUser = remoteUsers[key]!!

        remoteProjectUser.setToken(token, uuid)
    }

    fun updatePhotoUrl(deviceInfo: DeviceInfo, photoUrl: String) {
        val key = deviceInfo.key
        check(remoteUsers.containsKey(key))

        val remoteProjectUser = remoteUsers.getValue(key)

        remoteProjectUser.photoUrl = photoUrl
    }

    override fun updateRecordOf(addedFriends: Set<RemoteRootUser>, removedFriends: Set<String>) {
        remoteProjectRecord.updateRecordOf(addedFriends.asSequence().map { it.id }.toSet(), removedFriends)

        for (addedFriend in addedFriends)
            addUser(addedFriend)

        for (removedFriend in removedFriends) {
            check(remoteUsers.containsKey(removedFriend))

            remoteUsers[removedFriend]!!.delete()
        }
    }

    private fun newRemoteCustomTime(customTimeJson: SharedCustomTimeJson): RemoteSharedCustomTime {
        val remoteCustomTimeRecord = remoteProjectRecord.newRemoteCustomTimeRecord(customTimeJson)

        val remoteCustomTime = RemoteSharedCustomTime(this, remoteCustomTimeRecord)

        check(!remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

        return remoteCustomTime
    }

    fun deleteCustomTime(remoteCustomTime: RemoteSharedCustomTime) {
        check(remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes.remove(remoteCustomTime.id)
    }

    fun getSharedTimeIfPresent(privateCustomTimeId: RemoteCustomTimeId.Private) = remoteCustomTimes.values
            .singleOrNull {
                it.ownerKey == domainFactory.remoteProjectFactory
                        .remotePrivateProject
                        .id
                        && it.privateKey == privateCustomTimeId
            }

    override fun getRemoteCustomTimeKey(customTimeKey: CustomTimeKey<*>): CustomTimeKey.Shared {
        val privateProject = domainFactory.remoteProjectFactory.remotePrivateProject

        return when (customTimeKey) {
            is CustomTimeKey.Private -> {
                        val remotePrivateCustomTime = privateProject.getRemoteCustomTime(customTimeKey.remoteCustomTimeId)

                        val sharedCustomTime = getSharedTimeIfPresent(remotePrivateCustomTime.id)
                        if (sharedCustomTime != null) {
                            sharedCustomTime.customTimeKey
                        } else {
                            val customTimeJson = SharedCustomTimeJson(remotePrivateCustomTime.name, remotePrivateCustomTime.getHourMinute(DayOfWeek.SUNDAY).hour, remotePrivateCustomTime.getHourMinute(DayOfWeek.SUNDAY).minute, remotePrivateCustomTime.getHourMinute(DayOfWeek.MONDAY).hour, remotePrivateCustomTime.getHourMinute(DayOfWeek.MONDAY).minute, remotePrivateCustomTime.getHourMinute(DayOfWeek.TUESDAY).hour, remotePrivateCustomTime.getHourMinute(DayOfWeek.TUESDAY).minute, remotePrivateCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).hour, remotePrivateCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).minute, remotePrivateCustomTime.getHourMinute(DayOfWeek.THURSDAY).hour, remotePrivateCustomTime.getHourMinute(DayOfWeek.THURSDAY).minute, remotePrivateCustomTime.getHourMinute(DayOfWeek.FRIDAY).hour, remotePrivateCustomTime.getHourMinute(DayOfWeek.FRIDAY).minute, remotePrivateCustomTime.getHourMinute(DayOfWeek.SATURDAY).hour, remotePrivateCustomTime.getHourMinute(DayOfWeek.SATURDAY).minute, privateProject.id, remotePrivateCustomTime.id.value)

                            newRemoteCustomTime(customTimeJson).customTimeKey
                        }
                    }
            is CustomTimeKey.Shared -> {
                        check(customTimeKey.remoteProjectId == id)

                customTimeKey
            }
        }
    }

    override fun getRemoteCustomTime(remoteCustomTimeId: RemoteCustomTimeId): RemoteSharedCustomTime {
        check(remoteCustomTimes.containsKey(remoteCustomTimeId))

        return remoteCustomTimes.getValue(remoteCustomTimeId as RemoteCustomTimeId.Shared)
    }

    override fun getRemoteCustomTimeId(id: String) = RemoteCustomTimeId.Shared(id)
}