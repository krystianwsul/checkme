package com.krystianwsul.common.domain

data class DeviceInfo(
        val userInfo: UserInfo,
        val token: String?
) {

    val email get() = userInfo.email
    val name get() = userInfo.name
    val key get() = userInfo.key
}