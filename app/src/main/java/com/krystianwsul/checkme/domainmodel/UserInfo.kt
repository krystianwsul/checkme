package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.firebase.UserData

data class UserInfo(
        val email: String,
        val name: String
) {

    val key by lazy { UserData.getKey(email) }

    init {
        check(email.isNotEmpty())
        check(name.isNotEmpty())
    }
}