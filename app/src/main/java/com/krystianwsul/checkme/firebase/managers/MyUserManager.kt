package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.firebase.snapshot.TypedSnapshot
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.ValueRecordManager
import com.krystianwsul.common.firebase.records.MyUserRecord
import com.krystianwsul.common.utils.UserKey

class MyUserManager(
        deviceDbInfo: DeviceDbInfo,
        snapshot: TypedSnapshot<UserWrapper>,
) : ValueRecordManager<MyUserRecord>(), SnapshotRecordManager<MyUserRecord, TypedSnapshot<UserWrapper>> {

    companion object {

        private fun Snapshot.toKey() = UserKey(key)

        private fun TypedSnapshot<UserWrapper>.toRecord() = MyUserRecord(false, getValue()!!, toKey())
    }

    override val databasePrefix = DatabaseWrapper.USERS_KEY

    override var value = if (!snapshot.exists()) {
        val userWrapper = UserWrapper(
                deviceDbInfo.run { UserJson(email, name, mutableMapOf(uuid to token), userInfo.uid) }
        )

        MyUserRecord(true, userWrapper, snapshot.toKey())
    } else {
        snapshot.toRecord()
    }

    override val records = listOf(value)

    override fun set(snapshot: TypedSnapshot<UserWrapper>) = set { snapshot.toRecord() }
}
