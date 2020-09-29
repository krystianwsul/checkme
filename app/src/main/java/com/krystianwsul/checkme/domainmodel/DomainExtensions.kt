package com.krystianwsul.checkme.domainmodel

import com.google.firebase.auth.FirebaseUser
import com.krystianwsul.common.domain.UserInfo

fun FirebaseUser.toUserInfo() = UserInfo(email!!, displayName!!, uid)