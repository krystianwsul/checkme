package com.krystianwsul.common.firebase

interface RemoteMyUserInterface {

    fun setToken(uuid: String, token: String?)

    var defaultReminder: Boolean
    var defaultTab: Int
}