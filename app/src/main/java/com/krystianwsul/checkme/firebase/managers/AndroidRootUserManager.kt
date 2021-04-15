package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.FriendsLoader
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.RootUserManager
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

class AndroidRootUserManager(
        children: List<FriendsLoader.UserWrapperData>,
        private val databaseWrapper: DatabaseWrapper,
) : RootUserManager() {

    companion object {

        private fun Snapshot<*>.toKey() = UserKey(key)
    }

    private fun Snapshot<UserWrapper>.toRecord() = RootUserRecord(databaseWrapper, false, value!!, toKey())

    init {
        setInitialRecords(children.associate { it.snapshot.toKey() to it.snapshot.toRecord() })
    }

    fun set(snapshot: Snapshot<UserWrapper>) = set(
            snapshot.toKey(),
            { it.createObject != snapshot.value },
            { snapshot.toRecord() },
    )

    fun addFriend(userKey: UserKey, userWrapper: UserWrapper) =
            RootUserRecord(databaseWrapper, false, userWrapper, userKey).also { add(userKey, it) }
}
