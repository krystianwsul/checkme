package com.krystianwsul.common.firebase

interface MyUserProperties {

    fun setToken(uuid: String, token: String?)

    var defaultReminder: Boolean
    var defaultTab: Int
}