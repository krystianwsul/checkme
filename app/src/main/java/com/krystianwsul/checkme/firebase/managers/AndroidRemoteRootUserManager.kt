package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.RemoteRootUserManager
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

class AndroidRemoteRootUserManager(children: Iterable<Snapshot>) : RemoteRootUserManager() {

    companion object {

        private fun Snapshot.toRecord() = RootUserRecord(false, getValue(UserWrapper::class.java)!!)
    }

    override var remoteRootUserRecords = children.map { it.toRecord() to false }
            .associateBy { it.first.id }
            .toMutableMap()

    fun setFriend(dataSnapshot: Snapshot): RootUserRecord {
        val userKey = UserKey(dataSnapshot.key)

        return dataSnapshot.toRecord().also {
            remoteRootUserRecords[userKey] = it to false
        }
    }

    fun removeFriend(userKey: UserKey) = checkNotNull(remoteRootUserRecords.remove(userKey))

    fun addFriend(userKey: UserKey, userWrapper: UserWrapper): RootUserRecord {
        check(!remoteRootUserRecords.containsKey(userKey))

        return RootUserRecord(false, userWrapper).also {
            remoteRootUserRecords[userKey] = it to false
        }
    }
}
