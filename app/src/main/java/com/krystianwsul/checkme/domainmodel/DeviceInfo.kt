package com.krystianwsul.checkme.domainmodel

import android.text.TextUtils
import com.google.firebase.auth.FirebaseUser
import com.krystianwsul.checkme.firebase.UserData

data class DeviceInfo(
        val email: String,
        val name: String,
        var token: String?) {

    constructor(firebaseUser: FirebaseUser, token: String?) : this(
            firebaseUser.email!!,
            firebaseUser.displayName!!,
            token)

    val key by lazy { UserData.getKey(email) }

    init {
        check(!TextUtils.isEmpty(email))
        check(!TextUtils.isEmpty(name))
    }
}