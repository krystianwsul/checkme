package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.utils.UserKey

abstract class RootUserManager<U : Any> : MapRecordManager<UserKey, U>() {

    override val databasePrefix = DatabaseWrapper.USERS_KEY
}