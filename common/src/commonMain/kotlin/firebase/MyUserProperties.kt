package com.krystianwsul.common.firebase

import com.krystianwsul.common.domain.DeviceDbInfo

interface MyUserProperties {

    fun setToken(deviceDbInfo: DeviceDbInfo)
}