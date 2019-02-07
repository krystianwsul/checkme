package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.json.SharedCustomTimeJson
import com.krystianwsul.checkme.firebase.records.RemoteSharedProjectRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.TaskHierarchyContainer
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import java.util.*

class RemoteSharedProject(
        domainFactory: DomainFactory,
        override val remoteProjectRecord: RemoteSharedProjectRecord,
        userInfo: UserInfo,
        uuid: String,
        now: ExactTimeStamp) : RemoteProject(domainFactory, uuid) {

    private val remoteUsers = remoteProjectRecord.remoteUserRecords
            .values
            .map { RemoteProjectUser(this, it) }
            .associateBy { it.id }
            .toMutableMap()

    val users get() = remoteUsers.values

    override val remoteCustomTimes = HashMap<String, RemoteSharedCustomTime>()
    override val remoteTasks: MutableMap<String, RemoteTask>
    override val remoteTaskHierarchies = TaskHierarchyContainer<String, RemoteTaskHierarchy>()

    init {
        for (remoteCustomTimeRecord in remoteProjectRecord.remoteCustomTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = RemoteSharedCustomTime(this, remoteCustomTimeRecord)

            remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

            if (remoteCustomTimeRecord.ownerId == domainFactory.uuid && domainFactory.localFactory.hasLocalCustomTime(remoteCustomTimeRecord.localId)) {
                val localCustomTime = domainFactory.localFactory.getLocalCustomTime(remoteCustomTimeRecord.localId)

                localCustomTime.updateRemoteCustomTimeRecord(remoteCustomTimeRecord)
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

    override fun getRemoteCustomTimeId(localCustomTimeKey: CustomTimeKey.LocalCustomTimeKey): String {
        val localCustomTimeId = localCustomTimeKey.localCustomTimeId

        val localCustomTime = domainFactory.localFactory.getLocalCustomTime(localCustomTimeId)

        val remoteCustomTime = getRemoteCustomTimeIfPresent(localCustomTimeId)
        if (remoteCustomTime != null)
            return remoteCustomTime.id

        val customTimeJson = SharedCustomTimeJson(domainFactory.uuid, localCustomTime.id, localCustomTime.name, localCustomTime.getHourMinute(DayOfWeek.SUNDAY).hour, localCustomTime.getHourMinute(DayOfWeek.SUNDAY).minute, localCustomTime.getHourMinute(DayOfWeek.MONDAY).hour, localCustomTime.getHourMinute(DayOfWeek.MONDAY).minute, localCustomTime.getHourMinute(DayOfWeek.TUESDAY).hour, localCustomTime.getHourMinute(DayOfWeek.TUESDAY).minute, localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).hour, localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).minute, localCustomTime.getHourMinute(DayOfWeek.THURSDAY).hour, localCustomTime.getHourMinute(DayOfWeek.THURSDAY).minute, localCustomTime.getHourMinute(DayOfWeek.FRIDAY).hour, localCustomTime.getHourMinute(DayOfWeek.FRIDAY).minute, localCustomTime.getHourMinute(DayOfWeek.SATURDAY).hour, localCustomTime.getHourMinute(DayOfWeek.SATURDAY).minute)

        return newRemoteCustomTime(customTimeJson).id
    }
}