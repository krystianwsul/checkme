package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.users.UserJson
import com.krystianwsul.common.firebase.json.users.UserWrapper
import com.krystianwsul.common.firebase.managers.ValueRecordManager
import com.krystianwsul.common.firebase.records.users.MyUserRecord
import com.krystianwsul.common.utils.UserKey

class MyUserManager(
    deviceDbInfo: DeviceDbInfo,
    snapshot: Snapshot<UserWrapper>,
    private val databaseWrapper: DatabaseWrapper,
) : ValueRecordManager<MyUserRecord>() {

    companion object {

        private fun Snapshot<*>.toKey() = UserKey(key)
    }

    private fun Snapshot<UserWrapper>.toRecord() = MyUserRecord(databaseWrapper, false, value!!, toKey())

    override val databasePrefix = DatabaseWrapper.USERS_KEY

    init {
        setInitialValue(
                if (!snapshot.exists) {
                    val userWrapper = UserWrapper(
                            deviceDbInfo.run { UserJson(email, name, mutableMapOf(uuid to token), userInfo.uid) }
                    )

                    MyUserRecord(databaseWrapper, true, userWrapper, snapshot.toKey())
                } else {
                    snapshot.toRecord()
                }
        )
    }

    override val records get() = listOf(value)

    fun set(snapshot: Snapshot<UserWrapper>) = set(
            { it.createObject != snapshot.value },
            { snapshot.toRecord() },
    )
}
