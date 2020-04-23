package com.krystianwsul.common.firebase

import com.krystianwsul.common.domain.DeviceDbInfo

interface MyUserProperties {

    fun setToken(deviceDbInfo: DeviceDbInfo)

    var defaultReminder: Boolean
    var defaultTab: Int
}