package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.snapshot.Snapshot
import com.krystianwsul.checkme.firebase.loaders.snapshot.UntypedSnapshot
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.RootUserManager
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

class AndroidRootUserManager(children: Iterable<Snapshot>) : RootUserManager(), SnapshotRecordManager<RootUserRecord> {

    companion object {

        private fun Snapshot.toKey() = UserKey(key)

        private fun Snapshot.toRecord() = RootUserRecord(
                false,
                getValue(UserWrapper::class.java)!!,
                toKey()
        )
    }

    override var recordPairs = children.associate { it.toKey() to Pair(it.toRecord(), false) }.toMutableMap()

    override fun set(snapshot: UntypedSnapshot) = setNonNull(snapshot.toKey()) { snapshot.toRecord() }

    fun addFriend(
            userKey: UserKey,
            userWrapper: UserWrapper
    ) = RootUserRecord(false, userWrapper, userKey).also { add(userKey, it) }
}
