package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

class JsRootUserManager(databaseWrapper: DatabaseWrapper, userWrappers: Map<String, UserWrapper>) : RootUserManager() {

    override var _records = userWrappers.map { RootUserRecord(databaseWrapper, false, it.value, UserKey(it.key)) }
            .associateBy { it.userKey }
            .toMutableMap()
}