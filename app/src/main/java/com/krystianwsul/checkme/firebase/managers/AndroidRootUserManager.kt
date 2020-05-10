package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.RootUserManager
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

class AndroidRootUserManager(children: Iterable<Snapshot>) : RootUserManager() {

    companion object {

        private fun Snapshot.toRecord() = RootUserRecord(false, getValue(UserWrapper::class.java)!!, UserKey(key))
    }

    override var records = children.associate { UserKey(it.key) to Pair(it.toRecord(), false) }.toMutableMap()

    fun setFriend(dataSnapshot: Snapshot): RootUserRecord {
        val userKey = UserKey(dataSnapshot.key)

        return dataSnapshot.toRecord().also {
            records[userKey] = it to false
        }
    }

    fun removeFriend(userKey: UserKey) = checkNotNull(records.remove(userKey))

    fun addFriend(userKey: UserKey, userWrapper: UserWrapper): RootUserRecord {
        check(!records.containsKey(userKey))

        return RootUserRecord(false, userWrapper, userKey).also {
            records[userKey] = it to false
        }
    }
}
