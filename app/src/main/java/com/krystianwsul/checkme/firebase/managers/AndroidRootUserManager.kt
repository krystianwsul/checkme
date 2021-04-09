package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.JsonDifferenceException
import com.krystianwsul.common.firebase.managers.RootUserManager
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

class AndroidRootUserManager(
        children: Iterable<Snapshot<UserWrapper>>,
        private val databaseWrapper: DatabaseWrapper,
) : RootUserManager(), SnapshotRecordManager<RootUserRecord, Snapshot<UserWrapper>> {

    companion object {

        private fun Snapshot<*>.toKey() = UserKey(key)
    }

    private fun Snapshot<UserWrapper>.toRecord() = RootUserRecord(databaseWrapper, false, value!!, toKey())

    override var recordPairs = children.associate { it.toKey() to Pair(it.toRecord(), false) }.toMutableMap()

    override fun set(snapshot: Snapshot<UserWrapper>) = set(
            snapshot.toKey(),
            { JsonDifferenceException.compare(it.createObject, snapshot.value) },
            { snapshot.toRecord() },
    )

    fun addFriend(userKey: UserKey, userWrapper: UserWrapper) =
            RootUserRecord(databaseWrapper, false, userWrapper, userKey).also { add(userKey, it) }
}
