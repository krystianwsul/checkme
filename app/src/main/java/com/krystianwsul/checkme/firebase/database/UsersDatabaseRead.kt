package com.krystianwsul.checkme.firebase.database

import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.users.UserWrapper

class UsersDatabaseRead : IndicatorDatabaseRead<Map<String, UserWrapper>>() {

    override val type = "users"

    override fun getResult() = AndroidDatabaseWrapper.rootReference.child(DatabaseWrapper.USERS_KEY)
        .orderByKey()
        .indicatorSnapshotChanges()
}