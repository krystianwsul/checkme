package com.krystianwsul.checkme.firebase.database

import com.google.firebase.database.DatabaseReference
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.users.UserWrapper
import com.krystianwsul.common.utils.UserKey

class UserDatabaseRead(private val userKey: UserKey) : TypedDatabaseRead<UserWrapper>() {

    override val type = "user"

    override val kClass = UserWrapper::class

    override fun DatabaseReference.getQuery() = child("${DatabaseWrapper.USERS_KEY}/${userKey.key}")
}