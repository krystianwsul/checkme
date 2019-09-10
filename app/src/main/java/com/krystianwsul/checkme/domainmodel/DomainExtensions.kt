package com.krystianwsul.checkme.domainmodel

import com.google.firebase.auth.FirebaseUser

fun FirebaseUser.toUserInfo() = UserInfo(email!!, displayName!!)