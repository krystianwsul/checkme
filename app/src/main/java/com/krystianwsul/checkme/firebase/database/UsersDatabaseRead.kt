package com.krystianwsul.checkme.firebase.database

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.GenericTypeIndicator
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.users.UserWrapper

class UsersDatabaseRead : IndicatorDatabaseRead<Map<String, UserWrapper>>() {

    override fun DatabaseReference.getQuery() = child(DatabaseWrapper.USERS_KEY).orderByKey()

    override val genericTypeIndicator = object : GenericTypeIndicator<Map<String, UserWrapper>>() {}
}