package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

class JsRootUserManager(databaseWrapper: DatabaseWrapper, userWrappers: Map<String, UserWrapper>) : RootUserManager() {

    init {
        setInitialRecords(
                userWrappers.entries.associate {
                    val userKey = UserKey(it.key)

                    userKey to RootUserRecord(databaseWrapper, false, it.value, userKey)
                }
        )
    }
}