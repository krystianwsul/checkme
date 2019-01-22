package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.records.RemoteSharedProjectRecord
import com.krystianwsul.checkme.utils.time.ExactTimeStamp

class RemoteSharedProject(
        domainFactory: DomainFactory,
        private val remoteProjectRecord: RemoteSharedProjectRecord,
        userInfo: UserInfo,
        uuid: String,
        now: ExactTimeStamp) : RemoteProject(domainFactory, remoteProjectRecord, uuid, now) {

    private val remoteUsers = remoteProjectRecord.remoteUserRecords
            .values
            .map { RemoteProjectUser(this, it) }
            .associateBy { it.id }
            .toMutableMap()

    val users get() = remoteUsers.values

    init {
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
}