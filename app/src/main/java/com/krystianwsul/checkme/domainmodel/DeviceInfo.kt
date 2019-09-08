package com.krystianwsul.checkme.domainmodel

import com.google.firebase.auth.FirebaseUser

data class DeviceInfo(
        val userInfo: UserInfo,
        var token: String?) {

    constructor(firebaseUser: FirebaseUser, token: String?) : this(
            UserInfo(firebaseUser),
            token)

    val email get() = userInfo.email
    val name get() = userInfo.name
    val key get() = userInfo.key
}