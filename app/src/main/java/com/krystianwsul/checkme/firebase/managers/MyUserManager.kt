package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.snapshot.Snapshot
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.ValueRecordManager
import com.krystianwsul.common.firebase.records.MyUserRecord
import com.krystianwsul.common.utils.UserKey

class MyUserManager(
        deviceDbInfo: DeviceDbInfo,
        snapshot: Snapshot
) : ValueRecordManager<MyUserRecord>(), SnapshotRecordManager<MyUserRecord> {

    companion object {

        private fun Snapshot.toKey() = UserKey(key)

        private fun Snapshot.toRecord() = MyUserRecord(false, getValue(UserWrapper::class.java)!!, toKey())
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

    override fun set(snapshot: Snapshot) = set { snapshot.toRecord() }
}
