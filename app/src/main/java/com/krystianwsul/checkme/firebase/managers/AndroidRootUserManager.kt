package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.firebase.snapshot.TypedSnapshot
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.RootUserManager
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

class AndroidRootUserManager(children: Iterable<TypedSnapshot<UserWrapper>>) : RootUserManager(),
        SnapshotRecordManager<RootUserRecord, TypedSnapshot<UserWrapper>> {

    companion object {

        private fun Snapshot.toKey() = UserKey(key)

        private fun TypedSnapshot<UserWrapper>.toRecord() = RootUserRecord(
                false,
                getValue(UserWrapper::class.java)!!,
                toKey(),
        )
    }

    override var recordPairs = children.associate { it.toKey() to Pair(it.toRecord(), false) }.toMutableMap()

    override fun set(snapshot: TypedSnapshot<UserWrapper>) = setNonNull(snapshot.toKey()) { snapshot.toRecord() }

    fun addFriend(
            userKey: UserKey,
            userWrapper: UserWrapper
    ) = RootUserRecord(false, userWrapper, userKey).also { add(userKey, it) }
}
