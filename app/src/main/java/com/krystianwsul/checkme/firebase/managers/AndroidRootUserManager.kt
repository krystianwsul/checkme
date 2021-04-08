package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.JsonDifferenceException
import com.krystianwsul.common.firebase.managers.RootUserManager
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

class AndroidRootUserManager(children: Iterable<Snapshot<UserWrapper>>) : RootUserManager(),
        SnapshotRecordManager<RootUserRecord, Snapshot<UserWrapper>> {

    companion object {

        private fun Snapshot<*>.toKey() = UserKey(key)

        private fun Snapshot<UserWrapper>.toRecord() = RootUserRecord(false, value!!, toKey())
    }

    override var recordPairs = children.associate { it.toKey() to Pair(it.toRecord(), false) }.toMutableMap()

    override fun set(snapshot: Snapshot<UserWrapper>) = setNonNull(
            snapshot.toKey(),
            {
                if (it.createObject == snapshot.value) throw JsonDifferenceException(it.createObject, snapshot.value)
            },
            { snapshot.toRecord() },
    )

    fun addFriend(
            userKey: UserKey,
            userWrapper: UserWrapper,
    ) = RootUserRecord(false, userWrapper, userKey).also { add(userKey, it) }
}
