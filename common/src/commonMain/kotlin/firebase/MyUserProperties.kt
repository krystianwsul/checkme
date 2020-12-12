package com.krystianwsul.common.firebase

import com.krystianwsul.common.domain.DeviceDbInfo

interface MyUserProperties {

    fun setToken(deviceDbInfo: DeviceDbInfo)

    var defaultReminder: Boolean // todo remove these two after 2021-01-12
    var defaultTab: Int
}