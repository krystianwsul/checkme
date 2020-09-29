package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.UserData

data class UserInfo(
        val email: String,
        val name: String,
        val uid: String
) {

    val key by lazy { UserData.getKey(email) }

    init {
        check(email.isNotEmpty())
        check(name.isNotEmpty())
    }
}