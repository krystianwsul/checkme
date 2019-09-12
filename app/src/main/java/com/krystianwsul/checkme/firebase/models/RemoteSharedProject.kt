package com.krystianwsul.checkme.firebase.models

import com.krystianwsul.checkme.domain.Instance
import com.krystianwsul.checkme.domain.TaskHierarchyContainer
import com.krystianwsul.checkme.firebase.RemoteProjectFactory // todo js
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
import com.krystianwsul.common.firebase.records.RemoteSharedProjectRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.RemoteCustomTimeId
import java.util.* // todo js

class RemoteSharedProject(
        remoteProjectFactory: RemoteProjectFactory,
        shownFactory: Instance.ShownFactory,
        override val remoteProjectRecord: RemoteSharedProjectRecord,
        deviceInfo: DeviceInfo,
        uuid: String,
        now: ExactTimeStamp) : RemoteProject<RemoteCustomTimeId.Shared>(shownFactory, remoteProjectFactory, uuid) {

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
                .map { RemoteTask(shownFactory, this, it, now) }
                .associateBy { it.id }
                .toMutableMap()

        remoteProjectRecord.remoteTaskHierarchyRecords
                .values
                .map { RemoteTaskHierarchy(this, it) }
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

    fun deleteCustomTime(remoteCustomTime: RemoteSharedCustomTime) {
        check(remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes.remove(remoteCustomTime.id)
    }

    fun getSharedTimeIfPresent(
            privateCustomTimeId: RemoteCustomTimeId.Private,
            ownerKey: String
    ) = remoteCustomTimes.values.singleOrNull { it.ownerKey == ownerKey && it.privateKey == privateCustomTimeId }

    override fun getRemoteCustomTime(remoteCustomTimeId: RemoteCustomTimeId): RemoteSharedCustomTime {
        check(remoteCustomTimes.containsKey(remoteCustomTimeId))

        return remoteCustomTimes.getValue(remoteCustomTimeId as RemoteCustomTimeId.Shared)
    }

    override fun getRemoteCustomTimeId(id: String) = RemoteCustomTimeId.Shared(id)

    private fun newRemoteCustomTime(customTimeJson: SharedCustomTimeJson): RemoteSharedCustomTime {
        val remoteCustomTimeRecord = remoteProjectRecord.newRemoteCustomTimeRecord(customTimeJson)

        val remoteCustomTime = RemoteSharedCustomTime(this, remoteCustomTimeRecord)

        check(!remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

        return remoteCustomTime
    }

    override fun getOrCreateCustomTime(ownerKey: String, remoteCustomTime: RemoteCustomTime<*>): RemoteSharedCustomTime {
        fun copy(): RemoteSharedCustomTime {
            val private = remoteCustomTime as? RemotePrivateCustomTime

            val customTimeJson = SharedCustomTimeJson(
                    remoteCustomTime.name,
                    remoteCustomTime.getHourMinute(DayOfWeek.SUNDAY).hour,
                    remoteCustomTime.getHourMinute(DayOfWeek.SUNDAY).minute,
                    remoteCustomTime.getHourMinute(DayOfWeek.MONDAY).hour,
                    remoteCustomTime.getHourMinute(DayOfWeek.MONDAY).minute,
                    remoteCustomTime.getHourMinute(DayOfWeek.TUESDAY).hour,
                    remoteCustomTime.getHourMinute(DayOfWeek.TUESDAY).minute,
                    remoteCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).hour,
                    remoteCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).minute,
                    remoteCustomTime.getHourMinute(DayOfWeek.THURSDAY).hour,
                    remoteCustomTime.getHourMinute(DayOfWeek.THURSDAY).minute,
                    remoteCustomTime.getHourMinute(DayOfWeek.FRIDAY).hour,
                    remoteCustomTime.getHourMinute(DayOfWeek.FRIDAY).minute,
                    remoteCustomTime.getHourMinute(DayOfWeek.SATURDAY).hour,
                    remoteCustomTime.getHourMinute(DayOfWeek.SATURDAY).minute,
                    private?.projectId,
                    private?.id?.value
            )

            return newRemoteCustomTime(customTimeJson)
        }

        return when (remoteCustomTime) {
            is RemotePrivateCustomTime -> getSharedTimeIfPresent(remoteCustomTime.id, ownerKey)
            is RemoteSharedCustomTime -> remoteCustomTime.takeIf { it.projectId == id }
            else -> throw IllegalArgumentException()
        } ?: copy()
    }
}