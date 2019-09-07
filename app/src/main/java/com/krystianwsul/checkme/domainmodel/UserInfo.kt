package com.krystianwsul.checkme.domainmodel

import com.google.firebase.auth.FirebaseUser
import com.krystianwsul.checkme.firebase.UserData

data class UserInfo(
        val email: String,
        val name: String
) {

    constructor(firebaseUser: FirebaseUser) : this(
            firebaseUser.email!!,
            firebaseUser.displayName!!)

    val key by lazy { UserData.getKey(email) }

    init {
        check(email.isNotEmpty())
        check(name.isNotEmpty())
    }
}