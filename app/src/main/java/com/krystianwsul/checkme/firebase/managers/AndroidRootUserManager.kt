package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.RootUserManager
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

class AndroidRootUserManager(children: Iterable<Snapshot>) : RootUserManager() {

    companion object {

        private fun Snapshot.toKey() = UserKey(key)

        private fun Snapshot.toRecord() = RootUserRecord(
                false,
                getValue(UserWrapper::class.java)!!,
                toKey()
        )
    }

    override var recordPairs = children.associate { it.toKey() to Pair(it.toRecord(), false) }.toMutableMap()

    fun setFriend(snapshot: Snapshot) = setNonNull(snapshot.toKey()) { snapshot.toRecord() }

    fun addFriend(
            userKey: UserKey,
            userWrapper: UserWrapper
    ) = RootUserRecord(false, userWrapper, userKey).also { add(userKey, it) }
}
