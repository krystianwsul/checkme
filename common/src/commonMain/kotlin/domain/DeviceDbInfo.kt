package com.krystianwsul.common.domain

data class DeviceDbInfo(val deviceInfo: DeviceInfo, val uuid: String) {

    val userInfo get() = deviceInfo.userInfo
    val email get() = userInfo.email
    val name get() = userInfo.name
    val key get() = userInfo.key
    val token get() = deviceInfo.token
}