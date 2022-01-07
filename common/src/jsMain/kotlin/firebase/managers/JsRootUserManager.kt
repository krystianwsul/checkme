package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.users.UserWrapper
import com.krystianwsul.common.firebase.records.users.RootUserRecord
import com.krystianwsul.common.utils.UserKey

class JsRootUserManager(databaseWrapper: DatabaseWrapper, userWrappers: Map<String, UserWrapper>) :
        RootUserManager<RootUserRecord>() {

    init {
        setInitialRecords(
                userWrappers.entries.associate {
                    val userKey = UserKey(it.key)

                    userKey to RootUserRecord(databaseWrapper, false, it.value, userKey)
                }
        )
    }

    override fun valueToRecord(value: RootUserRecord) = value
}