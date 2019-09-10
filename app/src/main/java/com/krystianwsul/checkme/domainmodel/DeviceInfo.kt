package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.common.domain.UserInfo

data class DeviceInfo(
        val userInfo: UserInfo,
        var token: String?) {

    val email get() = userInfo.email
    val name get() = userInfo.name
    val key get() = userInfo.key
}