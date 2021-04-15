package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

abstract class RootUserManager : MapRecordManager<UserKey, RootUserRecord>() {

    override val databasePrefix = DatabaseWrapper.USERS_KEY

    override fun valueToRecord(value: RootUserRecord) = value
}