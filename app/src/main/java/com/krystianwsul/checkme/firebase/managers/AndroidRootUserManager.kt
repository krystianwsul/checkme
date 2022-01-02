package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.FriendsLoader
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.ReasonWrapper
import com.krystianwsul.common.firebase.UserLoadReason
import com.krystianwsul.common.firebase.json.users.UserWrapper
import com.krystianwsul.common.firebase.managers.RootUserManager
import com.krystianwsul.common.firebase.records.users.RootUserRecord
import com.krystianwsul.common.utils.UserKey

class AndroidRootUserManager(
        children: List<FriendsLoader.UserWrapperData>,
        private val databaseWrapper: DatabaseWrapper,
) : RootUserManager<ReasonWrapper<RootUserRecord>>() {

    companion object {

        private fun Snapshot<*>.toKey() = UserKey(key)
    }

    private fun Snapshot<UserWrapper>.toRecord() = RootUserRecord(databaseWrapper, false, value!!, toKey())

    init {
        setInitialRecords(
                children.associate { it.snapshot.toKey() to ReasonWrapper(it.reason, it.snapshot.toRecord()) }
        )
    }

    override fun valueToRecord(value: ReasonWrapper<RootUserRecord>) = value.value

    fun set(userWrapperData: FriendsLoader.UserWrapperData) = set(
            userWrapperData.snapshot.toKey(),
            { it.value.createObject != userWrapperData.snapshot.value },
            { ReasonWrapper(userWrapperData.reason, userWrapperData.snapshot.toRecord()) },
    )

    fun addFriend(userKey: UserKey, userWrapper: UserWrapper) = RootUserRecord(
            databaseWrapper,
            false,
            userWrapper,
            userKey,
    ).also { add(userKey, ReasonWrapper(UserLoadReason.FRIEND, it)) }
}
