package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.json.SharedCustomTimeJson
import com.krystianwsul.checkme.firebase.records.RemoteSharedProjectRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.TaskHierarchyContainer
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import java.util.*

class RemoteSharedProject(
        domainFactory: DomainFactory,
        privateProject: RemotePrivateProject,
        override val remoteProjectRecord: RemoteSharedProjectRecord,
        userInfo: UserInfo,
        uuid: String,
        now: ExactTimeStamp) : RemoteProject<RemoteCustomTimeId.Shared>(domainFactory, uuid) {

    private val remoteUsers = remoteProjectRecord.remoteUserRecords
            .values
            .map { RemoteProjectUser(this, it) }
            .associateBy { it.id }
            .toMutableMap()

    val users get() = remoteUsers.values

    override val remoteCustomTimes = HashMap<RemoteCustomTimeId.Shared, RemoteSharedCustomTime>()
    override val remoteTasks: MutableMap<String, RemoteTask<RemoteCustomTimeId.Shared>>
    override val remoteTaskHierarchies = TaskHierarchyContainer<String, RemoteTaskHierarchy<RemoteCustomTimeId.Shared>>()

    override val customTimes get() = remoteCustomTimes.values

    init {
        for (remoteCustomTimeRecord in remoteProjectRecord.remoteCustomTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = RemoteSharedCustomTime(this, remoteCustomTimeRecord)

            remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

            if (remoteCustomTimeRecord.ownerId == domainFactory.uuid && domainFactory.localFactory.hasLocalCustomTime(remoteCustomTimeRecord.localId)) {
                val localCustomTime = domainFactory.localFactory.getLocalCustomTime(remoteCustomTimeRecord.localId)

                localCustomTime.updateRemoteCustomTimeRecord(remoteCustomTimeRecord, privateProject)
            }
        }

        remoteTasks = remoteProjectRecord.remoteTaskRecords
                .values
                .map { RemoteTask(domainFactory, this, it, now) }
                .associateBy { it.id }
                .toMutableMap()

        remoteProjectRecord.remoteTaskHierarchyRecords
                .values
                .map { RemoteTaskHierarchy(domainFactory, this, it) }
                .forEach { remoteTaskHierarchies.add(it.id, it) }

        updateUserInfo(userInfo, uuid)
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

    fun updateUserInfo(userInfo: UserInfo, uuid: String) {
        val key = userInfo.key
        check(remoteUsers.containsKey(key))

        val remoteProjectUser = remoteUsers[key]!!

        remoteProjectUser.name = userInfo.name
        remoteProjectUser.setToken(userInfo.token, uuid)
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

    override fun getRemoteCustomTimeIfPresent(localCustomTimeId: Int) = remoteCustomTimes.values.singleOrNull { it.remoteCustomTimeRecord.let { it.ownerId == uuid && it.localId == localCustomTimeId } }

    fun getSharedTimeIfPresent(privateCustomTimeId: RemoteCustomTimeId.Private) = remoteCustomTimes.values
            .singleOrNull {
                it.ownerKey == domainFactory.remoteProjectFactory
                        .remotePrivateProject
                        .id
                        && it.privateKey == privateCustomTimeId
            }

    override fun getRemoteCustomTimeKey(customTimeKey: CustomTimeKey): CustomTimeKey.RemoteCustomTimeKey<RemoteCustomTimeId.Shared> {
        val privateProject = domainFactory.remoteProjectFactory.remotePrivateProject

        return when (customTimeKey) {
            is CustomTimeKey.LocalCustomTimeKey -> {
                val localCustomTimeId = customTimeKey.localCustomTimeId

                val localCustomTime = domainFactory.localFactory.getLocalCustomTime(localCustomTimeId)

                val remoteCustomTime = getRemoteCustomTimeIfPresent(localCustomTimeId)
                if (remoteCustomTime != null) {
                    remoteCustomTime.customTimeKey
                } else {
                    val privateCustomTime = privateProject.getRemoteCustomTimeIfPresent(localCustomTimeId)!!

                    val customTimeJson = SharedCustomTimeJson(domainFactory.uuid, localCustomTime.id, localCustomTime.name, localCustomTime.getHourMinute(DayOfWeek.SUNDAY).hour, localCustomTime.getHourMinute(DayOfWeek.SUNDAY).minute, localCustomTime.getHourMinute(DayOfWeek.MONDAY).hour, localCustomTime.getHourMinute(DayOfWeek.MONDAY).minute, localCustomTime.getHourMinute(DayOfWeek.TUESDAY).hour, localCustomTime.getHourMinute(DayOfWeek.TUESDAY).minute, localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).hour, localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).minute, localCustomTime.getHourMinute(DayOfWeek.THURSDAY).hour, localCustomTime.getHourMinute(DayOfWeek.THURSDAY).minute, localCustomTime.getHourMinute(DayOfWeek.FRIDAY).hour, localCustomTime.getHourMinute(DayOfWeek.FRIDAY).minute, localCustomTime.getHourMinute(DayOfWeek.SATURDAY).hour, localCustomTime.getHourMinute(DayOfWeek.SATURDAY).minute, privateProject.id, privateCustomTime.id.value)

                    newRemoteCustomTime(customTimeJson).customTimeKey
                }
            }
            is CustomTimeKey.RemoteCustomTimeKey<*> -> {
                when (customTimeKey.remoteCustomTimeId) {
                    is RemoteCustomTimeId.Private -> {
                        val remotePrivateCustomTime = privateProject.getRemoteCustomTime(customTimeKey.remoteCustomTimeId)

                        val sharedCustomTime = getSharedTimeIfPresent(remotePrivateCustomTime.id)
                        if (sharedCustomTime != null) {
                            sharedCustomTime.customTimeKey
                        } else {
                            val customTimeJson = SharedCustomTimeJson(domainFactory.uuid, remotePrivateCustomTime.remoteCustomTimeRecord.localId, remotePrivateCustomTime.name, remotePrivateCustomTime.getHourMinute(DayOfWeek.SUNDAY).hour, remotePrivateCustomTime.getHourMinute(DayOfWeek.SUNDAY).minute, remotePrivateCustomTime.getHourMinute(DayOfWeek.MONDAY).hour, remotePrivateCustomTime.getHourMinute(DayOfWeek.MONDAY).minute, remotePrivateCustomTime.getHourMinute(DayOfWeek.TUESDAY).hour, remotePrivateCustomTime.getHourMinute(DayOfWeek.TUESDAY).minute, remotePrivateCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).hour, remotePrivateCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).minute, remotePrivateCustomTime.getHourMinute(DayOfWeek.THURSDAY).hour, remotePrivateCustomTime.getHourMinute(DayOfWeek.THURSDAY).minute, remotePrivateCustomTime.getHourMinute(DayOfWeek.FRIDAY).hour, remotePrivateCustomTime.getHourMinute(DayOfWeek.FRIDAY).minute, remotePrivateCustomTime.getHourMinute(DayOfWeek.SATURDAY).hour, remotePrivateCustomTime.getHourMinute(DayOfWeek.SATURDAY).minute, privateProject.id, remotePrivateCustomTime.id.value)

                            newRemoteCustomTime(customTimeJson).customTimeKey
                        }
                    }
                    is RemoteCustomTimeId.Shared -> {
                        check(customTimeKey.remoteProjectId == id)

                        customTimeKey as CustomTimeKey.RemoteCustomTimeKey<RemoteCustomTimeId.Shared>
                    }
                }
            }
        }
    }

    override fun getRemoteCustomTime(remoteCustomTimeId: RemoteCustomTimeId): RemoteSharedCustomTime {
        check(remoteCustomTimes.containsKey(remoteCustomTimeId))

        return remoteCustomTimes.getValue(remoteCustomTimeId as RemoteCustomTimeId.Shared)
    }

    override fun getRemoteCustomTimeId(id: String) = RemoteCustomTimeId.Shared(id)
}