package com.krystianwsul.checkme.domainmodel

import android.text.TextUtils
import com.google.firebase.auth.FirebaseUser
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.firebase.UserData

data class UserInfo(
        val email: String,
        val name: String,
        val token: String?) {

    constructor(firebaseUser: FirebaseUser) : this(
            firebaseUser.email!!,
            firebaseUser.displayName!!,
            MyApplication.instance.token)

    val key by lazy { UserData.getKey(email) }

    init {
        check(!TextUtils.isEmpty(email))
        check(!TextUtils.isEmpty(name))
    }

    fun getValues(uuid: String) = mapOf(
            "email" to email,
            "name" to name,
            "tokens/$uuid" to token)
}